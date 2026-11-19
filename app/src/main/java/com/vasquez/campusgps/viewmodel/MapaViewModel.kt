package com.vasquez.campusgps.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vasquez.campusgps.data.BuscadorRepository
import com.vasquez.campusgps.data.LugaresRepository
import com.vasquez.campusgps.data.UbicacionNoDisponibleException
import com.vasquez.campusgps.data.UbicacionRepository
import com.vasquez.campusgps.model.AnimoGuia
import com.vasquez.campusgps.model.DestinoBusqueda
import com.vasquez.campusgps.model.EstadoBusqueda
import com.vasquez.campusgps.model.Lugar
import com.vasquez.campusgps.model.LugarConDistancia
import com.vasquez.campusgps.model.MensajeGuia
import com.vasquez.campusgps.model.Posicion
import com.vasquez.campusgps.model.ResultadoBusqueda
import com.vasquez.campusgps.model.TipoError
import com.vasquez.campusgps.model.UbicacionUi
import com.vasquez.campusgps.util.distanciaMetros
import com.vasquez.campusgps.util.formatearDistancia
import com.vasquez.campusgps.util.minutosAPie
import com.vasquez.campusgps.util.rumboCardinal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val lugaresRepo: LugaresRepository,
    private val buscadorRepo: BuscadorRepository
) : ViewModel() {

    private val _estado = MutableStateFlow<UbicacionUi>(UbicacionUi.Inicial)
    val estado: StateFlow<UbicacionUi> = _estado.asStateFlow()

    private val _lugares = MutableStateFlow(
        lugaresRepo.obtenerLugares().map { LugarConDistancia(it, null) }
    )
    val lugares: StateFlow<List<LugarConDistancia>> = _lugares.asStateFlow()

    private val _seleccionado = MutableStateFlow<Int?>(null)
    val seleccionado: StateFlow<Int?> = _seleccionado.asStateFlow()

    // Control de la pantalla de inicio (bienvenida)
    private val _mostrandoInicio = MutableStateFlow(true)
    val mostrandoInicio: StateFlow<Boolean> = _mostrandoInicio.asStateFlow()

    fun comenzar() {
        _mostrandoInicio.value = false
    }

    // Frases de Chaski en la pantalla de inicio
    private val frasesInicioList = listOf(
        "¡Hola! Soy Chaski… creo que me perdí.",
        "¿Alguien sabe dónde queda la Biblioteca?",
        "Dicen que la cafetería está por allá… o por allá.",
        "Tú sí sabes dónde estás. ¡Ayúdame a llegar!",
        "¡Ya! Pulsa Comenzar y no me pierdo más."
    )
    private val _indiceFrase = MutableStateFlow(0)
    val fraseInicio: StateFlow<String> = _indiceFrase
        .map { index -> frasesInicioList[index.coerceIn(0, frasesInicioList.lastIndex)] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), frasesInicioList.first())

    fun tocarChaski() {
        if (_indiceFrase.value < frasesInicioList.lastIndex) {
            _indiceFrase.value += 1
        }
    }

    // Estado del buscador de lugares
    private val _consulta = MutableStateFlow("")
    val consulta: StateFlow<String> = _consulta.asStateFlow()

    private val _busqueda = MutableStateFlow<EstadoBusqueda>(EstadoBusqueda.Inactiva)
    val busqueda: StateFlow<EstadoBusqueda> = _busqueda.asStateFlow()

    private val _destino = MutableStateFlow<DestinoBusqueda?>(null)
    val destino: StateFlow<DestinoBusqueda?> = _destino.asStateFlow()

    private var searchJob: Job? = null

    private companion object {
        const val PAUSA_SUGERENCIAS_MS = 350L
    }

    // Sugerencias de búsqueda (6 lugares de la app, ordenados por distancia si hay ubicación)
    val sugeridos: StateFlow<List<ResultadoBusqueda>> = estado
        .map { est ->
            val pos = if (est is UbicacionUi.Lista) est.posicion else null
            val lista = lugaresRepo.obtenerLugares()
            val ordenados = if (pos != null) {
                lista.sortedBy { distanciaMetros(pos, it) }
            } else {
                lista
            }
            ordenados.take(6).map { l ->
                ResultadoBusqueda(
                    nombre = l.nombre,
                    detalle = l.categoria,
                    latitud = l.latitud,
                    longitud = l.longitud,
                    lugarId = l.id
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            lugaresRepo.obtenerLugares().take(6).map { l ->
                ResultadoBusqueda(
                    nombre = l.nombre,
                    detalle = l.categoria,
                    latitud = l.latitud,
                    longitud = l.longitud,
                    lugarId = l.id
                )
            }
        )

    private fun String.sinTildes(): String {
        return java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
    }

    private fun obtenerLocales(q: String): List<ResultadoBusqueda> {
        if (q.length < 3) return emptyList()
        val queryNorm = q.sinTildes().trim()
        return lugaresRepo.obtenerLugares()
            .filter { l ->
                l.nombre.sinTildes().contains(queryNorm) || l.categoria.sinTildes().contains(queryNorm)
            }
            .map { l ->
                ResultadoBusqueda(
                    nombre = l.nombre,
                    detalle = l.categoria,
                    latitud = l.latitud,
                    longitud = l.longitud,
                    lugarId = l.id
                )
            }
    }

    fun cambiarConsulta(texto: String) {
        _consulta.value = texto
        searchJob?.cancel()
        if (texto.trim().length < 3) {
            _busqueda.value = EstadoBusqueda.Inactiva
            return
        }
        val locales = obtenerLocales(texto)
        // Los del campus aparecen al instante; los del mapa llegan solos tras una breve pausa al escribir
        _busqueda.value = if (locales.isNotEmpty()) {
            EstadoBusqueda.Resultados(locales = locales, externos = emptyList())
        } else {
            EstadoBusqueda.Inactiva
        }
        searchJob = viewModelScope.launch {
            delay(PAUSA_SUGERENCIAS_MS)
            buscadorRepo.buscar(texto.trim()).onSuccess { externos ->
                // Sugerencias silenciosas: si falla o no hay nada, se deja lo que ya se mostraba
                if (externos.isNotEmpty() || locales.isNotEmpty()) {
                    _busqueda.value = EstadoBusqueda.Resultados(locales = locales, externos = externos)
                }
            }
        }
    }

    fun buscarEnMapa() {
        val q = _consulta.value.trim()
        if (q.length < 3) return

        _busqueda.value = EstadoBusqueda.Buscando
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val locales = obtenerLocales(q)
            buscadorRepo.buscar(q)
                .onSuccess { externos ->
                    _busqueda.value = EstadoBusqueda.Resultados(locales = locales, externos = externos)
                }
                .onFailure {
                    _busqueda.value = EstadoBusqueda.Error("No pude buscar ahora. Revisa tu conexión e intenta de nuevo")
                }
        }
    }

    fun elegirResultado(r: ResultadoBusqueda) {
        if (r.lugarId != null) {
            seleccionar(r.lugarId)
            _destino.value = null
            _consulta.value = ""
            _busqueda.value = EstadoBusqueda.Inactiva
        } else {
            _seleccionado.value = null
            val est = _estado.value
            val pos = if (est is UbicacionUi.Lista) est.posicion else null

            val (metros, minutos, rumbo) = if (pos != null) {
                val m = distanciaMetros(pos, r.latitud, r.longitud)
                Triple(m, minutosAPie(m), rumboCardinal(pos, r.latitud, r.longitud))
            } else {
                Triple(null, null, null)
            }

            _destino.value = DestinoBusqueda(
                nombre = r.nombre,
                detalle = r.detalle,
                latitud = r.latitud,
                longitud = r.longitud,
                metros = metros,
                minutos = minutos,
                rumbo = rumbo
            )
            _consulta.value = ""
            _busqueda.value = EstadoBusqueda.Inactiva
        }
    }

    fun limpiarBusqueda() {
        _consulta.value = ""
        _busqueda.value = EstadoBusqueda.Inactiva
        _destino.value = null
        _seleccionado.value = null
    }

    // El primero de la lista que ya tiene distancia calculada (null si aún no hay ubicación)
    val masCercano: StateFlow<Lugar?> = lugares
        .map { lista -> lista.firstOrNull { it.metros != null }?.lugar }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mensajeGuia: StateFlow<MensajeGuia> = combine(
        estado,
        lugares,
        seleccionado,
        masCercano,
        busqueda,
        destino
    ) { flowArray ->
        @Suppress("UNCHECKED_CAST")
        val est = flowArray[0] as UbicacionUi
        @Suppress("UNCHECKED_CAST")
        val listaLugares = flowArray[1] as List<LugarConDistancia>
        @Suppress("UNCHECKED_CAST")
        val selId = flowArray[2] as Int?
        @Suppress("UNCHECKED_CAST")
        val cercano = flowArray[3] as Lugar?
        @Suppress("UNCHECKED_CAST")
        val busqState = flowArray[4] as EstadoBusqueda
        @Suppress("UNCHECKED_CAST")
        val destState = flowArray[5] as DestinoBusqueda?

        when {
            // Error de ubicación
            est is UbicacionUi.Error -> {
                val texto = if (est.tipo == TipoError.GPS_APAGADO) {
                    "Uy, parece que el GPS está apagado. Actívalo y lo intentamos de nuevo"
                } else {
                    "Uy, " + est.mensaje.replaceFirstChar { it.lowercase() }
                }
                MensajeGuia(texto, AnimoGuia.PREOCUPADO)
            }
            // Buscando ubicación
            est is UbicacionUi.Buscando -> {
                MensajeGuia("Buscando tu ubicación…", AnimoGuia.PENSANDO)
            }
            // Error de búsqueda
            busqState is EstadoBusqueda.Error -> {
                MensajeGuia(busqState.mensaje, AnimoGuia.PREOCUPADO)
            }
            // Búsqueda en mapa en curso
            busqState is EstadoBusqueda.Buscando -> {
                MensajeGuia("Buscando en el mapa…", AnimoGuia.PENSANDO)
            }
            // Destino de búsqueda activo
            destState != null -> {
                if (destState.metros != null && destState.minutos != null && destState.rumbo != null) {
                    val dist = formatearDistancia(destState.metros)
                    val texto = "Encontré ${destState.nombre} · $dist · unos ${destState.minutos} min a pie, hacia el ${destState.rumbo}."
                    MensajeGuia(texto, AnimoGuia.CONTENTO)
                } else {
                    val texto = "Encontré ${destState.nombre}. Pulsa «Obtener mi ubicación» y te digo qué tan lejos está."
                    MensajeGuia(texto, AnimoGuia.CONTENTO)
                }
            }
            // Lugar seleccionado del campus
            selId != null && est is UbicacionUi.Lista -> {
                val pos = est.posicion
                val lugarSel = listaLugares.firstOrNull { it.lugar.id == selId }
                if (lugarSel != null && lugarSel.metros != null) {
                    val lugar = lugarSel.lugar
                    val dist = formatearDistancia(lugarSel.metros)
                    val min = minutosAPie(lugarSel.metros)
                    val rumbo = rumboCardinal(pos, lugar)
                    val esElMasCercano = (lugar.id == cercano?.id)
                    val prefijo = if (esElMasCercano) "¡Este es el más cercano! " else ""

                    val texto = "$prefijo${lugar.nombre} (${lugar.categoria}) · $dist · unos $min min a pie, hacia el $rumbo"
                    MensajeGuia(texto, AnimoGuia.CONTENTO)
                } else {
                    MensajeGuia("¡Estás aquí!", AnimoGuia.CONTENTO)
                }
            }
            // Lista / Ubicación de usuario
            est is UbicacionUi.Lista -> {
                val pos = est.posicion
                if (cercano != null) {
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
            // Inicial
            else -> {
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

        _destino.value?.let { dest ->
            val m = distanciaMetros(pos, dest.latitud, dest.longitud)
            _destino.value = dest.copy(
                metros = m,
                minutos = minutosAPie(m),
                rumbo = rumboCardinal(pos, dest.latitud, dest.longitud)
            )
        }

        // Comprobación del Paso 5 (se ve en Logcat, filtra por "Distancias")
        _lugares.value.forEach { Log.d("Distancias", "${it.lugar.nombre}: ${it.metros} m") }
    }

    fun seleccionar(id: Int?) {
        _seleccionado.value = id
        // Elegir un lugar del campus reemplaza al destino de una búsqueda externa
        if (id != null) _destino.value = null
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
