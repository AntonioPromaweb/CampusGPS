package com.vasquez.campusgps.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.vasquez.campusgps.R

data class CategoriaEstilo(
    val color: Color,
    @DrawableRes val iconoRes: Int
)

fun obtenerEstiloCategoria(categoria: String?): CategoriaEstilo {
    return when (categoria?.trim()?.lowercase()) {
        "estudio" -> CategoriaEstilo(
            color = CategoriaEstudioColor,
            iconoRes = R.drawable.ic_categoria_estudio
        )
        "comida" -> CategoriaEstilo(
            color = CategoriaComidaColor,
            iconoRes = R.drawable.ic_categoria_comida
        )
        "eventos" -> CategoriaEstilo(
            color = CategoriaEventosColor,
            iconoRes = R.drawable.ic_categoria_eventos
        )
        "servicios" -> CategoriaEstilo(
            color = CategoriaServiciosColor,
            iconoRes = R.drawable.ic_categoria_servicios
        )
        else -> CategoriaEstilo(
            color = CategoriaOtroColor,
            iconoRes = R.drawable.ic_categoria_otro
        )
    }
}
