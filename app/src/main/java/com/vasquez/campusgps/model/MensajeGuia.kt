package com.vasquez.campusgps.model

enum class AnimoGuia { NORMAL, CONTENTO, PENSANDO, PREOCUPADO }

data class MensajeGuia(val texto: String, val animo: AnimoGuia)
