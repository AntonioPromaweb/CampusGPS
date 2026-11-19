package com.vasquez.campusgps.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.vasquez.campusgps.R
import com.vasquez.campusgps.model.AnimoGuia
import com.vasquez.campusgps.model.MensajeGuia
import kotlinx.coroutines.delay

@Composable
fun ChaskiGuia(
    mensaje: MensajeGuia,
    modifier: Modifier = Modifier
) {
    var globoVisible by rememberSaveable { mutableStateOf(true) }

    // (a) Chaski sube y baja suavemente de forma continua
    val infiniteTransition = rememberInfiniteTransition(label = "ChaskiFlotando")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY"
    )

    // (c) Animaciones según cambio de ánimo (salto y sacudida lateral)
    var jumpTarget by remember { mutableFloatStateOf(0f) }
    var shakeTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(mensaje.animo) {
        jumpTarget = -14f
        delay(150)
        jumpTarget = 0f

        if (mensaje.animo == AnimoGuia.PREOCUPADO) {
            shakeTarget = 8f
            delay(50)
            shakeTarget = -8f
            delay(50)
            shakeTarget = 6f
            delay(50)
            shakeTarget = 0f
        }
    }

    val jumpOffsetY by animateFloatAsState(
        targetValue = jumpTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SaltoAnim"
    )

    val shakeOffsetX by animateFloatAsState(
        targetValue = shakeTarget,
        animationSpec = tween(50),
        label = "SacudidaAnim"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Imagen de Chaski con animaciones de flotado, salto y sacudida
        Image(
            painter = painterResource(id = R.drawable.ic_chaski),
            contentDescription = "Chaski, tu guía",
            modifier = Modifier
                .height(72.dp)
                .offset(
                    x = shakeOffsetX.dp,
                    y = (floatY + jumpOffsetY).dp
                )
                .clickable { globoVisible = !globoVisible }
        )

        // (d) El globo aparece con AnimatedVisibility (scaleIn + fadeIn) al inicio
        AnimatedVisibility(
            visible = globoVisible,
            enter = scaleIn(initialScale = 0.7f) + fadeIn(),
            exit = scaleOut(targetScale = 0.7f) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .padding(start = 2.dp)
                    .widthIn(max = 220.dp),
                shape = BubbleShape(),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 4.dp
            ) {
                // (b) El texto del globo cambia con AnimatedContent (fade + deslizamiento vertical corto)
                AnimatedContent(
                    targetState = mensaje.texto,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { height -> -height / 2 } + fadeOut())
                    },
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
                    label = "TextoGuiaAnim"
                ) { texto ->
                    Text(
                        text = texto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Forma del globo de diálogo con colita (triángulo) apuntando a Chaski a la izquierda
private class BubbleShape(
    private val cornerRadiusDp: Float = 14f,
    private val tailWidthDp: Float = 8f,
    private val tailHeightDp: Float = 10f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = cornerRadiusDp * density.density
        val tw = tailWidthDp * density.density
        val th = tailHeightDp * density.density

        val path = Path().apply {
            val tailCenterY = size.height / 2f

            // Esquina superior izquierda (desplazada por la colita)
            moveTo(tw + r, 0f)
            lineTo(size.width - r, 0f)
            arcTo(Rect(size.width - 2 * r, 0f, size.width, 2 * r), 270f, 90f, false)

            // Esquina inferior derecha
            lineTo(size.width, size.height - r)
            arcTo(Rect(size.width - 2 * r, size.height - 2 * r, size.width, size.height), 0f, 90f, false)

            // Esquina inferior izquierda
            lineTo(tw + r, size.height)
            arcTo(Rect(tw, size.height - 2 * r, tw + 2 * r, size.height), 90f, 90f, false)

            // Borde izquierdo hasta la colita
            lineTo(tw, tailCenterY + th / 2f)
            // Punta de la colita hacia la izquierda (x = 0)
            lineTo(0f, tailCenterY)
            lineTo(tw, tailCenterY - th / 2f)

            // Borde izquierdo hacia la esquina superior izquierda
            lineTo(tw, r)
            arcTo(Rect(tw, 0f, tw + 2 * r, 2 * r), 180f, 90f, false)
            close()
        }
        return Outline.Generic(path)
    }
}
