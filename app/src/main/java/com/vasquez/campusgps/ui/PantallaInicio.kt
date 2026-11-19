package com.vasquez.campusgps.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
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
    val isDark = isSystemInDarkTheme()

    // Paleta del tema
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 1. Cielo con degradado vertical a pantalla completa
    val skyGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF0B1622), Color(0xFF1D3040))
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFF6DB9EA),
                0.7f to Color(0xFFCFE9F4),
                1.0f to Color(0xFFF2EBDD)
            )
        )
    }

    // Filtro de oscuridad para las capas de Lima en modo noche (~0.45 de brillo)
    val darkFilter = if (isDark) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.45f, 0f, 0f, 0f, 0f,
                    0f, 0.45f, 0f, 0f, 0f,
                    0f, 0f, 0.45f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    } else null

    // Animaciones continuas de fondo (Sol, Luna, Nubes, Parallax, Combi, Flotado de Chaski)
    val infiniteTransition = rememberInfiniteTransition(label = "ChaskiInicioTransition")

    // Flotado continuo de Chaski
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

    // Animación de nubes (desplazamiento continuo ±20.dp, 14 s)
    val nubesOffset by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NubesOffset"
    )

    // Parallax: capa lejos (±4.dp, 16 s) y capa ciudad (±8.dp, 12 s)
    val lejosOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LejosOffset"
    )

    val ciudadOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CiudadOffset"
    )

    // Combi cruzando la calle (bucle de 9 s)
    val combiProgress by infiniteTransition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CombiProgress"
    )

    // Titileo suave de estrellas en modo oscuro
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarAlpha"
    )

    // Pulso del botón Comenzar
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Reacciones táctiles
    var jumpTarget by remember { mutableFloatStateOf(0f) }
    val jumpY by animateFloatAsState(
        targetValue = jumpTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ChaskiJump"
    )

    var combiHonkVisible by remember { mutableStateOf(false) }
    var combiBounceTarget by remember { mutableFloatStateOf(0f) }
    val combiBounceY by animateFloatAsState(
        targetValue = combiBounceTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CombiBounce"
    )

    // Ruta punteada y caída del pin
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

    // Posiciones fijas para las 25 estrellas en modo oscuro
    val starPositions = remember {
        listOf(
            Offset(0.10f, 0.05f), Offset(0.25f, 0.12f), Offset(0.42f, 0.04f), Offset(0.60f, 0.10f), Offset(0.78f, 0.06f),
            Offset(0.88f, 0.15f), Offset(0.15f, 0.22f), Offset(0.35f, 0.18f), Offset(0.52f, 0.25f), Offset(0.70f, 0.20f),
            Offset(0.05f, 0.30f), Offset(0.92f, 0.28f), Offset(0.20f, 0.35f), Offset(0.48f, 0.32f), Offset(0.65f, 0.38f),
            Offset(0.82f, 0.34f), Offset(0.30f, 0.42f), Offset(0.58f, 0.45f), Offset(0.12f, 0.48f), Offset(0.75f, 0.46f),
            Offset(0.40f, 0.10f), Offset(0.85f, 0.08f), Offset(0.28f, 0.28f), Offset(0.68f, 0.12f), Offset(0.95f, 0.40f)
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(skyGradient)
    ) {
        val totalWidthDp = maxWidth

        // Estrellas en la parte superior en modo oscuro (Canvas)
        if (isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                starPositions.forEachIndexed { i, pos ->
                    val alphaValue = ((starAlpha + i * 0.15f) % 1f * 0.7f + 0.3f).coerceIn(0.2f, 1f)
                    drawCircle(
                        color = Color.White.copy(alpha = alphaValue),
                        center = Offset(pos.x * w, pos.y * h * 0.5f),
                        radius = (1.5f + (i % 3) * 0.8f).dp.toPx()
                    )
                }
            }
        }

        // Sol o Luna en la esquina superior derecha
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            if (!isDark) {
                // 2. Sol de día (80.dp), girando lento (1 vuelta / 40s) y con leve pulso
                val sunRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(40000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "SunRotation"
                )
                val sunScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "SunScale"
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_lima_sol),
                    contentDescription = "Sol de Lima",
                    modifier = Modifier
                        .size(80.dp)
                        .scale(sunScale)
                        .rotate(sunRotation)
                )
            } else {
                // 2. Luna de noche (60.dp) con flotado vertical sutil
                val moonFloatY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "MoonFloatY"
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_lima_luna),
                    contentDescription = "Luna sobre Lima",
                    modifier = Modifier
                        .size(60.dp)
                        .offset(y = moonFloatY.dp)
                )
            }
        }

        // 3. Ruta punteada animada sobre el cielo y pin que cae con rebote
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val routePath = Path().apply {
                moveTo(w * 0.80f, h * 0.30f)
                cubicTo(
                    w * 0.60f, h * 0.20f,
                    w * 0.20f, h * 0.26f,
                    w * 0.14f, h * 0.46f
                )
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(routePath, false)
            val pathLength = pathMeasure.length

            val partialPath = Path()
            pathMeasure.getSegment(0f, pathLength * pathProgress, partialPath, true)

            // Ruta punteada en color secundario contrastante
            drawPath(
                path = partialPath,
                color = secondaryColor.copy(alpha = if (isDark) 0.85f else 0.7f),
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 10.dp.toPx()), 0f),
                    cap = StrokeCap.Round
                )
            )

            // Pin de mapa al finalizar la ruta
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

        // 4 y 5. Capas de la ciudad de Lima en el fondo, alineadas abajo (Alignment.BottomCenter)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // (a) Nubes en la parte alta
            Image(
                painter = painterResource(id = R.drawable.ic_lima_nubes),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                alpha = if (isDark) 0.15f else 0.85f,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(x = nubesOffset.dp, y = (-70).dp)
            )

            // (b) Capa Lejos (Cerros y Cruz del Cerro San Cristóbal)
            Image(
                painter = painterResource(id = R.drawable.ic_lima_lejos),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = darkFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(1.06f)
                    .offset(x = lejosOffset.dp)
            )

            // (b) Capa Ciudad (Balcones coloniales, catedral, edificios, faro)
            Image(
                painter = painterResource(id = R.drawable.ic_lima_ciudad),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = darkFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(1.06f)
                    .offset(x = ciudadOffset.dp)
            )

            // (b) Capa Calle (Pistas, árboles y faroles)
            Image(
                painter = painterResource(id = R.drawable.ic_lima_calle),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = darkFilter,
                modifier = Modifier.fillMaxWidth()
            )

            // (c y d) Combi cruzando la calle con globito "¡Piii, piii!"
            val combiX = (combiProgress * (totalWidthDp.value + 120) - 60).dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .offset(x = combiX, y = (-26).dp + combiBounceY.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = combiHonkVisible,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFD9733B)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "¡Piii, piii!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD9733B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Image(
                        painter = painterResource(id = R.drawable.ic_lima_combi),
                        contentDescription = "Combi de Lima",
                        colorFilter = darkFilter,
                        modifier = Modifier
                            .size(width = 64.dp, height = 36.dp)
                            .clickable {
                                coroutineScope.launch {
                                    combiBounceTarget = -14f
                                    combiHonkVisible = true
                                    delay(140)
                                    combiBounceTarget = 0f
                                    delay(860)
                                    combiHonkVisible = false
                                }
                            }
                    )
                }
            }
        }

        // 6. Primer plano (Títulos, Chaski, Globo de diálogo, "?", Brújula y Botón "Comenzar")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Encabezado legible sobre el cielo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "CampusGPS",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 34.sp),
                    color = if (isDark) primaryColor else Color(0xFF12301F),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tu guía por el campus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) onSurfaceVariantColor else Color(0xFF1E2A24),
                    fontWeight = FontWeight.Medium
                )
            }

            // Sección Central: Chaski parado en la ciudad, Globo de diálogo, Signos "?" y Brújula
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
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
                        shadowElevation = 8.dp
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

                    // Chaski (ic_chaski_perdido) grande al centro en primer plano
                    Image(
                        painter = painterResource(id = R.drawable.ic_chaski_perdido),
                        contentDescription = "Chaski perdido intentando leer un mapa",
                        modifier = Modifier
                            .height(260.dp)
                            // Proporción del vector (120x160): sin esto no se escala hacia arriba
                            .aspectRatio(120f / 160f)
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

            // Botón Comenzar en forma de píldora con elevación y animación de pulso
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
