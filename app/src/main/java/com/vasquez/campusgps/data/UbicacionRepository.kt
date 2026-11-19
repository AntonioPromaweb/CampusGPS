package com.vasquez.campusgps.data

import com.vasquez.campusgps.model.Posicion

// Se lanza cuando Google no pudo determinar la ubicación (por ejemplo, con el GPS apagado)
class UbicacionNoDisponibleException : Exception("Ubicación no disponible")

interface UbicacionRepository {
    suspend fun obtenerPosicion(): Result<Posicion>

    // El ViewModel lo consulta antes de buscar; así no necesita un Context
    fun playServicesDisponible(): Boolean
}
