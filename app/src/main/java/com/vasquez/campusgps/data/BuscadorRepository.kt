package com.vasquez.campusgps.data

import com.vasquez.campusgps.model.ResultadoBusqueda

class BuscadorNoDisponibleException : Exception("Búsqueda no disponible")

interface BuscadorRepository {
    suspend fun buscar(texto: String): Result<List<ResultadoBusqueda>>
}
