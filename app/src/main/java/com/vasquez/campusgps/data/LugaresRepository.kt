package com.vasquez.campusgps.data

import com.vasquez.campusgps.model.Lugar

interface LugaresRepository {
    fun obtenerLugares(): List<Lugar>
}
