package com.vasquez.campusgps.util

import android.location.Location
import com.vasquez.campusgps.model.Lugar
import com.vasquez.campusgps.model.Posicion

// Función pura: calcula la distancia en metros entre tu posición y un lugar.
// Orden de Location.distanceBetween: (lat1, lon1, lat2, lon2, resultado)
fun distanciaMetros(desde: Posicion, hasta: Lugar): Float {
    val resultado = FloatArray(1)
    Location.distanceBetween(
        desde.latitud, desde.longitud,
        hasta.latitud, hasta.longitud,
        resultado
    )
    return resultado[0]
}
