package com.vasquez.campusgps.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vasquez.campusgps.model.UbicacionUi
import com.vasquez.campusgps.util.formatearDistancia
import com.vasquez.campusgps.viewmodel.MapaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMapa(vm: MapaViewModel) {
    val estado by vm.estado.collectAsState()
    val lugares by vm.lugares.collectAsState()
    val seleccionado by vm.seleccionado.collectAsState()
    val masCercano by vm.masCercano.collectAsState()
    val mensajeGuia by vm.mensajeGuia.collectAsState()
    val consulta by vm.consulta.collectAsState()
    val busqueda by vm.busqueda.collectAsState()
    val destino by vm.destino.collectAsState()
    val contexto = LocalContext.current

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        // Si el usuario aceptó FINE o COARSE, buscamos; si negó todo, mostramos el mensaje
        if (resultado.values.any { it }) vm.buscar() else vm.permisoDenegado()
    }

    fun alPulsar() {
        val fine = ContextCompat.checkSelfPermission(
            contexto, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            contexto, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            vm.buscar() // ya tiene permiso: no preguntamos otra vez
        } else {
            lanzador.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CampusGPS",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // El mapa, la barra de búsqueda y la mascota Chaski ocupan el espacio libre
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                MapaUbicacion(
                    estado = estado,
                    lugares = lugares,
                    seleccionado = seleccionado,
                    masCercanoId = masCercano?.id,
                    destino = destino,
                    modifier = Modifier.fillMaxSize()
                )

                ChaskiGuia(
                    mensaje = mensajeGuia,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        // Deja libre el logo de Google (abajo a la izquierda del mapa)
                        .padding(start = 12.dp, bottom = 40.dp)
                )

                BarraBusqueda(
                    consulta = consulta,
                    estado = busqueda,
                    onConsultaCambia = { vm.cambiarConsulta(it) },
                    onBuscar = { vm.buscarEnMapa() },
                    onElegir = { vm.elegirResultado(it) },
                    onLimpiar = { vm.limpiarBusqueda() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { alPulsar() },
                    shape = CircleShape,
                    enabled = estado !is UbicacionUi.Buscando,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Obtener mi ubicación",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                when (val e = estado) {
                    UbicacionUi.Inicial -> Text(
                        text = "Pulsa el botón para obtener tu ubicación",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    UbicacionUi.Buscando -> Text(
                        text = "Buscando…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    is UbicacionUi.Lista -> Text(
                        text = "Latitud: ${e.posicion.latitud}  ·  Longitud: ${e.posicion.longitud}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    is UbicacionUi.Error -> MensajeError(
                        error = e,
                        onReintentar = { alPulsar() }
                    )
                }

                // Botón secundario: deshabilitado mientras no haya un lugar más cercano
                OutlinedButton(
                    onClick = { masCercano?.let { vm.seleccionar(it.id) } },
                    shape = CircleShape,
                    enabled = masCercano != null,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Ir al más cercano",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                masCercano?.let { lugar ->
                    val metros = lugares.firstOrNull { it.lugar.id == lugar.id }?.metros
                    Text(
                        text = "Más cercano: ${lugar.nombre}" +
                            (metros?.let { " (${formatearDistancia(it)})" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // La lista va debajo con altura fija
            ListaLugares(
                lugares = lugares,
                seleccionado = seleccionado,
                onClick = { vm.seleccionar(it) },
                masCercanoId = masCercano?.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )
        }
    }
}
