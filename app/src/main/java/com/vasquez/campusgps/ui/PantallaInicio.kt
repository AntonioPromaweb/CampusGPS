package com.vasquez.campusgps.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasquez.campusgps.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PantallaInicio(
    frase: String,
    onChaskiTocado: () -> Unit,
    onComenzar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Paleta del tema
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Animación continua de flotación y balanceo de Chaski
    val infiniteTransition = rememberInfiniteTransition(label = "ChaskiInicioTransition")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY"
    )

    val swayRotation by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SwayRotation"
    )

    // Animación de pulso para el botón Comenzar
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Salto con rebote (spring) al tocar a Chaski
    var jumpTarget by remember { mutableFloatStateOf(0f) }
    val jumpY by animateFloatAsState(
        targetValue = jumpTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ChaskiJump"
    )

    // Animación de ruta del mapa en segundo plano
    var pathProgressTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        pathProgressTarget = 1f
    }
    val pathProgress by animateFloatAsState(
        targetValue = pathProgressTarget,
        animationSpec = tween(2800, easing = LinearEasing),
        label = "PathProgress"
    )

    val pinDrop by animateFloatAsState(
        targetValue = if (pathProgress > 0.85f) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "PinDrop"
    )

    // Brújula animada
    var compassSpins by remember { mutableIntStateOf(1) }
    val compassRotation by animateFloatAsState(
        targetValue = compassSpins * 1440f,
        animationSpec = tween(2000, easing = FastOutSlowInEasing),
        label = "CompassRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Fondo decorativo sutil dibujado con Canvas (calles y ruta punteada animada)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val streetColor = primaryColor.copy(alpha = 0.07f)

            // Líneas de calles decorativas
            drawLine(streetColor, Offset(w * 0.1f, 0f), Offset(w * 0.1f, h), strokeWidth = 14.dp.toPx())
            drawLine(streetColor, Offset(w * 0.85f, 0f), Offset(w * 0.85f, h), strokeWidth = 10.dp.toPx())
            drawLine(streetColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 12.dp.toPx())
            drawLine(streetColor, Offset(0f, h * 0.72f), Offset(w, h * 0.72f), strokeWidth = 14.dp.toPx())

            // Ruta punteada animada que termina en un pin con rebote
            val routePath = Path().apply {
                moveTo(w * 0.18f, h * 0.2f)
                cubicTo(
                    w * 0.45f, h * 0.15f,
                    w * 0.75f, h * 0.35f,
                    w * 0.5f, h * 0.52f
                )
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(routePath, false)
            val pathLength = pathMeasure.length

            val partialPath = Path()
            pathMeasure.getSegment(0f, pathLength * pathProgress, partialPath, true)

            drawPath(
                path = partialPath,
                color = secondaryColor.copy(alpha = 0.45f),
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 10.dp.toPx()), 0f),
                    cap = StrokeCap.Round
                )
            )

            // Pin de mapa al final de la ruta
            if (pathProgress > 0.85f) {
                val endOffset = pathMeasure.getPosition(pathLength)
                val pinX = endOffset.x
                val pinY = endOffset.y - (1f - pinDrop) * 40.dp.toPx()

                val pinRadius = 10.dp.toPx()
                drawCircle(
                    color = primaryColor,
                    center = Offset(pinX, pinY),
                    radius = pinRadius
                )
                drawCircle(
                    color = Color.White,
                    center = Offset(pinX, pinY),
                    radius = pinRadius * 0.45f
                )
            }
        }

        // Contenido interactivo desplazable para adaptarse a pantallas pequeñas
        Column(
            modifier = Modifier
                .fillMaxSize()
                // La app usa pantalla completa: dejamos espacio para las barras del sistema
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Encabezado con título principal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = "CampusGPS",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu guía por el campus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariantColor
                )
            }

            // Sección Central: Chaski, Globo de Diálogo, Signos "?" y Brújula
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {

                // Signos de interrogación "?" flotando y desvaneciéndose alrededor
                SignoFlotante(initialX = (-90).dp, initialY = (-40).dp, delayMs = 0, color = secondaryColor)
                SignoFlotante(initialX = 95.dp, initialY = (-60).dp, delayMs = 500, color = secondaryColor)
                SignoFlotante(initialX = (-75).dp, initialY = 30.dp, delayMs = 1000, color = secondaryColor)
                SignoFlotante(initialX = 85.dp, initialY = 25.dp, delayMs = 1500, color = secondaryColor)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Globo de diálogo sobre Chaski con AnimatedContent
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, primaryColor),
                        shadowElevation = 6.dp
                    ) {
                        AnimatedContent(
                            targetState = frase,
                            transitionSpec = {
                                (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height / 2 } + fadeOut())
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            label = "FraseInicioAnim"
                        ) { texto ->
                            Text(
                                text = texto,
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Chaski (ic_chaski_perdido) grande al centro
                    Image(
                        painter = painterResource(id = R.drawable.ic_chaski_perdido),
                        contentDescription = "Chaski perdido intentando leer un mapa",
                        modifier = Modifier
                            .height(220.dp)
                            .offset(y = (floatY + jumpY).dp)
                            .rotate(swayRotation)
                            .clickable {
                                coroutineScope.launch {
                                    jumpTarget = -24f
                                    delay(160)
                                    jumpTarget = 0f
                                }
                                onChaskiTocado()
                            }
                    )
                }

                // Brújula animada e interactiva a la derecha
                BrujulaAnimada(
                    rotationDegrees = compassRotation,
                    onClick = { compassSpins += 1 },
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    onSurfaceVariantColor = onSurfaceVariantColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-8).dp, y = (-20).dp)
                )
            }

            // Botón Comenzar en forma de píldora con animación de pulso
            Button(
                onClick = onComenzar,
                shape = CircleShape,
                modifier = Modifier
                    .scale(pulseScale)
                    .widthIn(min = 220.dp)
                    .height(52.dp)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Comenzar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Composable auxiliar para signos de interrogación "?" flotantes
@Composable
private fun SignoFlotante(
    initialX: Dp,
    initialY: Dp,
    delayMs: Int,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "QuestionFloat")
    val animValue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = delayMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FloatAnim"
    )

    val offsetY = initialY - (animValue * 36).dp
    val alpha = if (animValue < 0.2f) animValue / 0.2f else (1f - animValue)

    Text(
        text = "?",
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        ),
        color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
        modifier = Modifier.offset(x = initialX, y = offsetY)
    )
}

// Brújula interactiva dibujada con Canvas
@Composable
private fun BrujulaAnimada(
    rotationDegrees: Float,
    onClick: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    onSurfaceVariantColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 3.dp.toPx()

            // Fondo del cuerpo de la brújula
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                center = center,
                radius = radius
            )
            // Borde exterior
            drawCircle(
                color = primaryColor,
                center = center,
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Marcas N/E/S/O
            val textPaint = android.graphics.Paint().apply {
                color = primaryColor.toArgb()
                textSize = 9.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - radius + 10.dp.toPx(), textPaint)

            val textVariantPaint = android.graphics.Paint().apply {
                color = onSurfaceVariantColor.toArgb()
                textSize = 8.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText("S", center.x, center.y + radius - 3.dp.toPx(), textVariantPaint)
            drawContext.canvas.nativeCanvas.drawText("E", center.x + radius - 7.dp.toPx(), center.y + 3.dp.toPx(), textVariantPaint)
            drawContext.canvas.nativeCanvas.drawText("O", center.x - radius + 7.dp.toPx(), center.y + 3.dp.toPx(), textVariantPaint)
        }

        // Aguja giratoria
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationDegrees)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val needleLen = size.width / 2f - 7.dp.toPx()

            // Punta Norte (Naranja / Secundario)
            val northPath = Path().apply {
                moveTo(center.x, center.y - needleLen)
                lineTo(center.x + 4.dp.toPx(), center.y)
                lineTo(center.x - 4.dp.toPx(), center.y)
                close()
            }
            drawPath(northPath, secondaryColor)

            // Punta Sur (Gris)
            val southPath = Path().apply {
                moveTo(center.x, center.y + needleLen)
                lineTo(center.x + 4.dp.toPx(), center.y)
                lineTo(center.x - 4.dp.toPx(), center.y)
                close()
            }
            drawPath(southPath, onSurfaceVariantColor.copy(alpha = 0.5f))

            // Pivote central
            drawCircle(color = primaryColor, center = center, radius = 3.dp.toPx())
        }
    }
}
