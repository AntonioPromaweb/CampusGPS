package com.vasquez.campusgps.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.vasquez.campusgps.R
import com.vasquez.campusgps.model.LugarConDistancia
import com.vasquez.campusgps.model.UbicacionUi
import com.vasquez.campusgps.ui.theme.obtenerEstiloCategoria

// Solo dibuja lo que recibe: no consulta ningún Repository ni calcula distancias.
@Composable
fun MapaUbicacion(
    estado: UbicacionUi,
    lugares: List<LugarConDistancia>,
    seleccionado: Int?,
    masCercanoId: Int?,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-12.0464, -77.0428), 12f)
    }

    // Cuando llega la ubicación, la cámara viaja hasta ella con zoom 16
    LaunchedEffect(estado) {
        if (estado is UbicacionUi.Lista) {
            camara.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(estado.posicion.latitud, estado.posicion.longitud),
                    16f
                )
            )
        }
    }

    // Al seleccionar un lugar, la cámara viaja hasta él con zoom 17
    LaunchedEffect(seleccionado) {
        val lugar = lugares.firstOrNull { it.lugar.id == seleccionado }?.lugar
        if (lugar != null) {
            camara.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(lugar.latitud, lugar.longitud), 17f)
            )
        }
    }

    val mapProperties = remember(isDark, contexto) {
        val rawRes = if (isDark) R.raw.mapa_oscuro else R.raw.mapa_claro
        MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(contexto, rawRes)
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camara,
        properties = mapProperties
    ) {
        // Marcador del usuario: punto azul con anillo blanco y título "Estás aquí"
        if (estado is UbicacionUi.Lista) {
            val userIcon = remember {
                crearBitmapDescriptorUsuario(contexto)
            }
            Marker(
                state = rememberUpdatedMarkerState(
                    position = LatLng(estado.posicion.latitud, estado.posicion.longitud)
                ),
                title = "Estás aquí",
                icon = userIcon,
                // El punto se centra sobre la posición (por defecto el ancla es la base)
                anchor = Offset(0.5f, 0.5f)
            )
        }

        // Marcador para cada lugar. key(id) identifica a cada uno.
        lugares.forEach { item ->
            val lugar = item.lugar
            val esMasCercano = lugar.id == masCercanoId
            val estilo = obtenerEstiloCategoria(lugar.categoria)

            key(lugar.id) {
                val estadoMarcador = rememberUpdatedMarkerState(
                    position = LatLng(lugar.latitud, lugar.longitud)
                )

                val markerIcon = remember(lugar.categoria, esMasCercano) {
                    crearBitmapDescriptorLugar(
                        context = contexto,
                        categoriaColor = estilo.color,
                        iconoRes = estilo.iconoRes,
                        esMasCercano = esMasCercano
                    )
                }

                // El lugar seleccionado muestra su nombre sobre el marcador
                LaunchedEffect(seleccionado) {
                    if (seleccionado == lugar.id) estadoMarcador.showInfoWindow()
                }

                Marker(
                    state = estadoMarcador,
                    title = lugar.nombre,
                    snippet = lugar.categoria,
                    icon = markerIcon,
                    // La punta de la gota (47/52 del alto) debe quedar sobre el lugar
                    anchor = Offset(0.5f, 47f / 52f)
                )
            }
        }
    }
}

private fun crearBitmapDescriptorUsuario(context: Context): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (30 * density).toInt()

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val outerRadius = 13f * density
    val innerRadius = 8.5f * density

    val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, outerRadius, paintWhite)

    val paintBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#3B6FE0")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, innerRadius, paintBlue)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun crearBitmapDescriptorLugar(
    context: Context,
    categoriaColor: Color,
    @DrawableRes iconoRes: Int,
    esMasCercano: Boolean
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val scale = if (esMasCercano) 1.3f else 1.0f

    val baseWidthDp = 38f
    val baseHeightDp = 52f
    val paddingDp = 5f * scale

    val widthPx = (baseWidthDp * scale * density).toInt()
    val heightPx = (baseHeightDp * scale * density).toInt()

    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = widthPx / 2f
    val paddingPx = paddingDp * density
    val r = (cx - paddingPx)
    val cy = r + paddingPx
    val tipY = cy + 2.0f * r

    // Path del pin/gota
    val pinPath = Path().apply {
        val rectF = RectF(cx - r, cy - r, cx + r, cy + r)
        arcTo(rectF, 30f, -240f, false)
        lineTo(cx, tipY)
        close()
    }

    // 1. Anillo dorado si es el más cercano (#E0B341)
    if (esMasCercano) {
        val paintGold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E0B341")
            style = Paint.Style.STROKE
            strokeWidth = 6f * density * scale
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(pinPath, paintGold)
    }

    // 2. Borde blanco fino
    val paintWhiteBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density * scale
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(pinPath, paintWhiteBorder)

    // 3. Relleno del color de la categoría
    val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = categoriaColor.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, paintFill)

    // 4. Dibujar el ícono de la categoría en blanco
    val drawable = ContextCompat.getDrawable(context, iconoRes)
    if (drawable != null) {
        val iconSize = (r * 1.15f).toInt()
        val left = (cx - iconSize / 2f).toInt()
        val top = (cy - iconSize / 2f).toInt()
        drawable.setBounds(left, top, left + iconSize, top + iconSize)
        drawable.setTint(android.graphics.Color.WHITE)
        drawable.draw(canvas)
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
