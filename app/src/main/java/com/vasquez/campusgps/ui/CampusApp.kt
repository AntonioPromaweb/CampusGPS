package com.vasquez.campusgps.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vasquez.campusgps.viewmodel.MapaViewModel

@Composable
fun CampusApp(vm: MapaViewModel) {
    val mostrandoInicio by vm.mostrandoInicio.collectAsState()
    val fraseInicio by vm.fraseInicio.collectAsState()

    AnimatedContent(
        targetState = mostrandoInicio,
        transitionSpec = {
            (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f)) togetherWith
            (fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.95f))
        },
        label = "PantallaTransition"
    ) { enInicio ->
        if (enInicio) {
            PantallaInicio(
                frase = fraseInicio,
                onChaskiTocado = { vm.tocarChaski() },
                onComenzar = { vm.comenzar() }
            )
        } else {
            PantallaMapa(vm = vm)
        }
    }
}
