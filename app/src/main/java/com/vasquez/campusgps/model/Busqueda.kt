package com.vasquez.campusgps.model

data class ResultadoBusqueda(
    val nombre: String,
    val detalle: String,
    val latitud: Double,
    val longitud: Double,
    val lugarId: Int? = null
)

sealed interface EstadoBusqueda {
    data object Inactiva : EstadoBusqueda
    data object Buscando : EstadoBusqueda
    data class Resultados(
        val locales: List<ResultadoBusqueda>,
        val externos: List<ResultadoBusqueda>
    ) : EstadoBusqueda
    data class Error(val mensaje: String) : EstadoBusqueda
}

data class DestinoBusqueda(
    val nombre: String,
    val detalle: String,
    val latitud: Double,
    val longitud: Double,
    val metros: Float?,
    val minutos: Int?,
    val rumbo: String?
)
