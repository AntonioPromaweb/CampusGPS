package com.vasquez.campusgps.data

import com.vasquez.campusgps.model.Lugar

class LugaresRepositoryLocal : LugaresRepository {
    override fun obtenerLugares(): List<Lugar> = listOf(
        Lugar(1, "Biblioteca Central", "Estudio", -12.0460, -77.0435),
        Lugar(2, "Cafetería Campus", "Comida", -12.0470, -77.0420),
        Lugar(3, "Laboratorio de Cómputo", "Estudio", -12.0455, -77.0415),
        Lugar(4, "Auditorio Principal", "Eventos", -12.0480, -77.0440),
        Lugar(5, "Estacionamiento Norte", "Servicios", -12.0440, -77.0430),
        Lugar(6, "Tecsup Santa Anita", "Estudio", -12.0452, -76.9520),
        Lugar(7, "Plaza de Armas de Lima", "Otro", -12.0464, -77.0300),
        Lugar(8, "Plaza San Martín", "Otro", -12.0518, -77.0348),
        Lugar(9, "Parque de la Exposición", "Otro", -12.0570, -77.0360),
        Lugar(10, "Museo de Arte de Lima", "Eventos", -12.0689, -77.0364),
        Lugar(11, "Estadio Nacional", "Eventos", -12.0670, -77.0330),
        Lugar(12, "Parque Kennedy", "Otro", -12.1219, -77.0297),
        Lugar(13, "Larcomar", "Comida", -12.1318, -77.0305),
        Lugar(14, "Jockey Plaza", "Servicios", -12.0847, -76.9747)
    )
}
