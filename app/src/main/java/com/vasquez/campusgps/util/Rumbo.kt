package com.vasquez.campusgps.util

import android.location.Location
import com.vasquez.campusgps.model.Lugar
import com.vasquez.campusgps.model.Posicion
import kotlin.math.roundToInt

// Función pura: calcula el rumbo cardinal (norte, noreste, etc.) entre dos puntos.
fun rumboCardinal(desde: Posicion, hasta: Lugar): String {
    val results = FloatArray(2)
    Location.distanceBetween(
        desde.latitud, desde.longitud,
        hasta.latitud, hasta.longitud,
        results
    )
    val bearing = (results[1] + 360f) % 360f
    val sectores = arrayOf("norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste")
    val indice = ((bearing + 22.5f) / 45f).toInt() % 8
    return sectores[indice]
}

// Sobrecarga con latitud y longitud numéricas directamente
fun rumboCardinal(desde: Posicion, latitud: Double, longitud: Double): String {
    val results = FloatArray(2)
    Location.distanceBetween(
        desde.latitud, desde.longitud,
        latitud, longitud,
        results
    )
    val bearing = (results[1] + 360f) % 360f
    val sectores = arrayOf("norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste")
    val indice = ((bearing + 22.5f) / 45f).toInt() % 8
    return sectores[indice]
}

// Función pura: estima minutos a pie asumiendo 80 metros/minuto, mínimo 1.
fun minutosAPie(metros: Float): Int {
    return (metros / 80f).roundToInt().coerceAtLeast(1)
}
