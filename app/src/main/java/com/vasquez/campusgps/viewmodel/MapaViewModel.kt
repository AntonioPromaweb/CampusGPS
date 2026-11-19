package com.vasquez.campusgps.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vasquez.campusgps.data.LugaresRepository
import com.vasquez.campusgps.data.UbicacionNoDisponibleException
import com.vasquez.campusgps.data.UbicacionRepository
import com.vasquez.campusgps.model.AnimoGuia
import com.vasquez.campusgps.model.Lugar
import com.vasquez.campusgps.model.LugarConDistancia
import com.vasquez.campusgps.model.MensajeGuia
import com.vasquez.campusgps.model.Posicion
import com.vasquez.campusgps.model.TipoError
import com.vasquez.campusgps.model.UbicacionUi
import com.vasquez.campusgps.util.distanciaMetros
import com.vasquez.campusgps.util.formatearDistancia
import com.vasquez.campusgps.util.minutosAPie
import com.vasquez.campusgps.util.rumboCardinal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapaViewModel(
    private val ubicacionRepo: UbicacionRepository,
    private val lugaresRepo: LugaresRepository
) : ViewModel() {

    private val _estado = MutableStateFlow<UbicacionUi>(UbicacionUi.Inicial)
    val estado: StateFlow<UbicacionUi> = _estado.asStateFlow()

    private val _lugares = MutableStateFlow(
        lugaresRepo.obtenerLugares().map { LugarConDistancia(it, null) }
    )
    val lugares: StateFlow<List<LugarConDistancia>> = _lugares.asStateFlow()

    private val _seleccionado = MutableStateFlow<Int?>(null)
    val seleccionado: StateFlow<Int?> = _seleccionado.asStateFlow()

    // El primero de la lista que ya tiene distancia calculada (null si aún no hay ubicación)
    val masCercano: StateFlow<Lugar?> = lugares
        .map { lista -> lista.firstOrNull { it.metros != null }?.lugar }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mensajeGuia: StateFlow<MensajeGuia> = combine(
        estado,
        lugares,
        seleccionado,
        masCercano
    ) { est, listaLugares, selId, cercano ->
        when (est) {
            is UbicacionUi.Error -> {
                val texto = if (est.tipo == TipoError.GPS_APAGADO) {
                    "Uy, parece que el GPS está apagado. Actívalo y lo intentamos de nuevo"
                } else {
                    "Uy, " + est.mensaje.replaceFirstChar { it.lowercase() }
                }
                MensajeGuia(texto, AnimoGuia.PREOCUPADO)
            }
            is UbicacionUi.Buscando -> {
                MensajeGuia("Buscando tu ubicación…", AnimoGuia.PENSANDO)
            }
            is UbicacionUi.Lista -> {
                val pos = est.posicion
                val lugarSel = selId?.let { id -> listaLugares.firstOrNull { it.lugar.id == id } }

                if (lugarSel != null && lugarSel.metros != null) {
                    val lugar = lugarSel.lugar
                    val dist = formatearDistancia(lugarSel.metros)
                    val min = minutosAPie(lugarSel.metros)
                    val rumbo = rumboCardinal(pos, lugar)
                    val esElMasCercano = (lugar.id == cercano?.id)
                    val prefijo = if (esElMasCercano) "¡Este es el más cercano! " else ""

                    val texto = "$prefijo${lugar.nombre} (${lugar.categoria}) · $dist · unos $min min a pie, hacia el $rumbo"
                    MensajeGuia(texto, AnimoGuia.CONTENTO)
                } else if (cercano != null) {
                    val cercanoItem = listaLugares.firstOrNull { it.lugar.id == cercano.id }
                    if (cercanoItem != null && cercanoItem.metros != null) {
                        val dist = formatearDistancia(cercanoItem.metros)
                        val rumbo = rumboCardinal(pos, cercano)
                        val texto = "¡Estás aquí! Lo más cerca: ${cercano.nombre}, a $dist al $rumbo."
                        MensajeGuia(texto, AnimoGuia.CONTENTO)
                    } else {
                        MensajeGuia("¡Estás aquí!", AnimoGuia.CONTENTO)
                    }
                } else {
                    MensajeGuia("¡Estás aquí!", AnimoGuia.CONTENTO)
                }
            }
            UbicacionUi.Inicial -> {
                MensajeGuia("¡Hola! Soy Chaski. Pulsa el botón y te muestro dónde estás.", AnimoGuia.NORMAL)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MensajeGuia("¡Hola! Soy Chaski. Pulsa el botón y te muestro dónde estás.", AnimoGuia.NORMAL)
    )

    fun buscar() {
        // Sin Google Play Services no hay nada que reintentar
        if (!ubicacionRepo.playServicesDisponible()) {
            _estado.value = UbicacionUi.Error(
                "Este dispositivo no tiene Google Play Services",
                TipoError.SIN_PLAY_SERVICES
            )
            return
        }
        _estado.value = UbicacionUi.Buscando
        viewModelScope.launch {
            ubicacionRepo.obtenerPosicion()
                .onSuccess {
                    _estado.value = UbicacionUi.Lista(it)
                    actualizarDistancias(it)
                }
                .onFailure { _estado.value = errorDe(it) }
        }
    }

    fun permisoDenegado() {
        _estado.value = UbicacionUi.Error(
            "Sin permiso de ubicación no puedo mostrar dónde estás",
            TipoError.SIN_PERMISO
        )
    }

    // Calcula la distancia a cada lugar, ordena de menor a mayor y publica la lista
    private fun actualizarDistancias(pos: Posicion) {
        _lugares.value = lugaresRepo.obtenerLugares()
            .map { LugarConDistancia(it, distanciaMetros(pos, it)) }
            .sortedBy { it.metros }
        // Comprobación del Paso 5 (se ve en Logcat, filtra por "Distancias")
        _lugares.value.forEach { Log.d("Distancias", "${it.lugar.nombre}: ${it.metros} m") }
    }

    fun seleccionar(id: Int?) {
        _seleccionado.value = id
    }

    // Convierte la excepción en un error con mensaje claro y un tipo para elegir la acción
    private fun errorDe(e: Throwable): UbicacionUi.Error = when (e) {
        is SecurityException -> UbicacionUi.Error(
            "Sin permiso de ubicación no puedo mostrar dónde estás",
            TipoError.SIN_PERMISO
        )
        is UbicacionNoDisponibleException -> UbicacionUi.Error(
            "No se pudo obtener la ubicación. Activa el GPS e intenta de nuevo",
            TipoError.GPS_APAGADO
        )
        else -> UbicacionUi.Error(
            e.message ?: "No se pudo obtener la ubicación",
            TipoError.OTRO
        )
    }
}
