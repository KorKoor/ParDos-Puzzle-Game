package com.korkoor.pardos.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// =====================================================================================
//  LENGUAJE VISUAL "JUGUETE DE GELATINA"
//  Las superficies parecen piezas físicas: tienen un LABIO sólido debajo (como un bloque de
//  madera o una tecla), un brillo suave arriba y se hunden al pulsarlas. Todo lo interactivo
//  responde al tacto; las listas entran escalonadas; el fondo tiene textura y respira.
// =====================================================================================

/** Aclara un color mezclándolo con blanco. */
fun Color.lighten(fraction: Float = 0.25f): Color = lerp(this, Color.White, fraction.coerceIn(0f, 1f))

/** Oscurece un color mezclándolo con un marrón-ciruela cálido (más rico que mezclar con negro). */
fun Color.deepen(fraction: Float = 0.28f): Color = lerp(this, Color(0xFF2B2438), fraction.coerceIn(0f, 1f))

/** Color del labio por defecto: arena cálida bajo las superficies claras, versión oscura del color bajo las de color. */
fun defaultLip(fill: Color): Color =
    if (fill.luminance() > 0.78f) lerp(fill, Color(0xFFCDB894), 0.50f).copy(alpha = 0.78f) else fill.deepen(0.30f)

/** Duración corta con la que "se hunde" una superficie al pulsar. */
private const val PRESS_SPRING_STIFFNESS = 520f

/**
 * Tarjeta "juguete": cuerpo con degradado, brillo arriba, borde cálido y labio sólido debajo.
 * Si [onClick] no es null, se hunde al pulsar y suelta con rebote.
 */
@Composable
fun JellyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.Large),
    fill: Color = Color.White,
    lip: Color = defaultLip(fill),
    lipHeight: Dp = 5.dp,
    enabled: Boolean = true,
    brush: Brush? = null,
    /** Si es true, el contenido recibe como mínimo el tamaño de la tarjeta (como `Surface` de Material). */
    fillContent: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = rememberGameHaptics()
    val pressed by interaction.collectIsPressedAsState()
    val travel by animateFloatAsState(
        targetValue = if (pressed && onClick != null && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = PRESS_SPRING_STIFFNESS), label = "jellyPress"
    )
    Box(
        modifier = modifier
            .drawBehind {
                // El labio: el mismo contorno, desplazado hacia abajo
                val lipPx = lipHeight.toPx()
                val outline = shape.createOutline(Size(size.width, size.height - lipPx), layoutDirection, this)
                translate(0f, lipPx) {
                    drawOutline(outline, lip)
                }
            }
            .padding(bottom = lipHeight),
        propagateMinConstraints = true
    ) {
        val clickMod = if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            // un "tic" suave al pulsar (respeta el interruptor de vibración de Ajustes)
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onClick()
        } else Modifier
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = travel * lipHeight.toPx() * 0.85f }
                .clip(shape)
                .background(brush ?: Brush.verticalGradient(listOf(fill, fill.deepen(0.035f))))
                .drawWithContent {
                    drawContent()
                    // brillo: borde interior claro que se desvanece hacia abajo + borde exterior fino y cálido
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutline(outline, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0f)), endY = size.height * 0.55f), style = Stroke(width = 2.2f * density))
                    drawOutline(outline, Color(0xFF3D405B).copy(alpha = 0.07f), style = Stroke(width = 1f * density))
                }
                .then(clickMod),
            propagateMinConstraints = fillContent,
            content = content
        )
    }
}

/**
 * Fondo de pantalla con vida: degradado cálido, tres manchas de color que respiran y **fichas fantasma** del juego (2, 4, 8, 16...)
 * que suben despacio girando, más unas luces suaves (bokeh). Se pinta a ~30 fps y solo en la fase de dibujo, así que casi no cuesta.
 * Se usa en lugar de `.background(ScreenBackground)`; acepta un degradado propio (el del tema).
 */
fun Modifier.pardosBackdrop(base: Brush = ScreenBackground): Modifier = composed {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val clock = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (now - last >= 33_000_000L) { last = now; clock.floatValue = (now / 1_000_000_000.0 % 3600.0).toFloat() }
            }
        }
    }
    // Noche de brujas: en lugar de fichas con números suben calabazas, fantasmas, murciélagos y dulces
    val halloween = Season.halloween
    val sprites = if (halloween) listOf(
        androidx.compose.ui.graphics.ImageBitmap.imageResource(com.korkoor.pardos.R.drawable.ico_pumpkin),
        androidx.compose.ui.graphics.ImageBitmap.imageResource(com.korkoor.pardos.R.drawable.ico_ghost),
        androidx.compose.ui.graphics.ImageBitmap.imageResource(com.korkoor.pardos.R.drawable.ico_bat),
        androidx.compose.ui.graphics.ImageBitmap.imageResource(com.korkoor.pardos.R.drawable.ico_candy),
        androidx.compose.ui.graphics.ImageBitmap.imageResource(com.korkoor.pardos.R.drawable.ico_skull),
        androidx.compose.ui.graphics.ImageBitmap.imageResource(com.korkoor.pardos.R.drawable.ico_lollipop)
    ) else emptyList()
    val webArt = if (halloween) androidx.compose.ui.graphics.ImageBitmap.imageResource(com.korkoor.pardos.R.drawable.ico_web) else null
    val spiderArt = if (halloween) androidx.compose.ui.graphics.ImageBitmap.imageResource(com.korkoor.pardos.R.drawable.ico_spider) else null
    val values = remember { listOf(2, 4, 8, 16, 32, 64, 2, 4, 8, 2, 16, 4) }
    val layouts = remember {
        values.map { measurer.measure(androidx.compose.ui.text.AnnotatedString("$it"), androidx.compose.ui.text.TextStyle(fontSize = 40.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)) }
    }
    val tints = remember { listOf(Color(0xFFE9C9A0), Color(0xFFF2CC8F), Color(0xFFEE9560), Color(0xFFDB6A57), Color(0xFF6B9E86)) }
    val seeds = remember {
        val r = kotlin.random.Random(41)
        List(values.size) { floatArrayOf(r.nextFloat(), 0.55f + r.nextFloat() * 0.8f, 7f + r.nextFloat() * 9f, r.nextFloat() * 360f, (r.nextFloat() - 0.5f) * 16f, r.nextFloat()) }
    }
    this
        .background(base)
        .drawBehind {
            val t = clock.floatValue
            val w = size.width; val h = size.height
            val tau = (2 * PI).toFloat()
            // manchas que respiran
            fun blob(color: Color, cx: Float, cy: Float, rad: Float, phase: Float) {
                val a = 0.5f + 0.5f * sin((t / 14f + phase) * tau)
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.12f + 0.06f * a), Color.Transparent), Offset(cx, cy), rad * (0.92f + 0.12f * a)),
                    rad * 1.1f, Offset(cx, cy)
                )
            }
            blob(Sage, w * 0.1f + 14f * cos(t / 14f * tau), h * 0.12f, w * 0.6f, 0f)
            blob(Terracotta, w * 0.95f, h * 0.45f + 18f * sin(t / 14f * tau), w * 0.55f, 0.33f)
            blob(Gold, w * 0.2f, h * 0.95f, w * 0.65f, 0.66f)
            // luces suaves
            for (i in 0 until 7) {
                val s = seeds[i]
                val bx = w * ((s[0] + 0.05f * sin((t / 20f + s[5]) * tau)) % 1f)
                val by = h * ((s[5] + 0.04f * cos((t / 25f + s[0]) * tau)) % 1f)
                val br = (18f + s[1] * 22f) * density
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.40f), Color.Transparent), Offset(bx, by), br), br, Offset(bx, by))
            }
            if (halloween) {
                // sprites que suben despacio (calabazas, fantasmas, murciélagos...), con su balanceo y desvanecido
                seeds.forEachIndexed { i, s ->
                    val img = sprites[i % sprites.size]
                    val px = ((34f + s[1] * 30f) * density).toInt()
                    val span = h + px * 2f
                    val y = h + px - ((t * s[2] * 0.8f * density + s[5] * span) % span)
                    val x = w * s[0] + px * 0.5f * sin((t / 16f + s[5]) * tau)
                    val fade = (sin(((h + px - y) / span) * PI.toFloat())).coerceIn(0f, 1f)
                    val rot = (if (i % 6 == 2) 0f else s[4] * 1.4f * sin((t / 6f + s[5]) * tau))
                    withTransform({ rotate(rot, Offset(x, y)) }) {
                        drawImage(img, dstOffset = androidx.compose.ui.unit.IntOffset((x - px / 2f).toInt(), (y - px / 2f).toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(px, px), alpha = 0.17f * fade)
                    }
                }
                // telarañas en las esquinas y una araña que cuelga y se balancea
                webArt?.let {
                    val ws = (w * 0.36f).toInt()
                    drawImage(it, dstOffset = androidx.compose.ui.unit.IntOffset(-ws / 10, -ws / 10), dstSize = androidx.compose.ui.unit.IntSize(ws, ws), alpha = 0.42f)
                    withTransform({ scale(-1f, 1f, Offset(w / 2f, 0f)) }) {
                        val ws2 = (w * 0.22f).toInt()
                        drawImage(it, dstOffset = androidx.compose.ui.unit.IntOffset(-ws2 / 10, -ws2 / 10), dstSize = androidx.compose.ui.unit.IntSize(ws2, ws2), alpha = 0.30f)
                    }
                }
                spiderArt?.let {
                    val sx = w * 0.9f + 6f * density * sin(t / 3.2f * tau)
                    val sy = h * 0.075f + 10f * density * (0.5f + 0.5f * sin(t / 4.1f * tau))
                    drawLine(Color.White.copy(alpha = 0.55f), Offset(w * 0.9f, 0f), Offset(sx, sy - 14f * density), strokeWidth = 1.4f * density)
                    val ss = (30f * density).toInt()
                    drawImage(it, dstOffset = androidx.compose.ui.unit.IntOffset((sx - ss / 2f).toInt(), (sy - ss / 2f).toInt()), dstSize = androidx.compose.ui.unit.IntSize(ss, ss), alpha = 0.85f)
                }
                return@drawBehind
            }
            // fichas fantasma que suben
            seeds.forEachIndexed { i, s ->
                val tile = (34f + s[1] * 34f) * density
                val span = h + tile * 2f
                val y = h + tile - ((t * s[2] * density + s[5] * span) % span)
                val x = w * s[0] + tile * 0.4f * sin((t / 18f + s[5]) * tau)
                val rot = s[3] + s[4] * t / 10f
                val tint = tints[i % tints.size]
                val fade = (sin(((h + tile - y) / span) * PI.toFloat())).coerceIn(0f, 1f)
                withTransform({
                    translate(x - tile / 2f, y - tile / 2f)
                    rotate(rot, Offset(tile / 2f, tile / 2f))
                }) {
                    drawRoundRect(tint.copy(alpha = 0.07f * fade), Offset(0f, tile * 0.07f), Size(tile, tile), androidx.compose.ui.geometry.CornerRadius(tile * 0.24f))
                    drawRoundRect(tint.copy(alpha = 0.13f * fade), Offset.Zero, Size(tile, tile), androidx.compose.ui.geometry.CornerRadius(tile * 0.24f))
                    val lay = layouts[i]
                    val k = (tile * 0.5f) / lay.size.height
                    withTransform({ translate((tile - lay.size.width * k) / 2f, (tile - lay.size.height * k) / 2f); scale(k, k, Offset.Zero) }) {
                        drawText(lay, color = Navy.copy(alpha = 0.11f * fade))
                    }
                }
            }
        }
}

/**
 * Entrada escalonada: el elemento aparece subiendo con un rebote suave, con un retraso según su [index].
 * Úsalo en listas y bloques de pantalla para que "caigan" uno tras otro.
 */
fun Modifier.staggerIn(index: Int, stepMs: Int = 55, distance: Dp = 18.dp, key: Any? = Unit): Modifier = composed {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay((index.coerceAtMost(12) * stepMs).toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 260f))
    }
    graphicsLayer {
        alpha = (progress.value * 1.4f).coerceIn(0f, 1f)
        translationY = (1f - progress.value) * distance.toPx()
    }
}

/** Una pulsación suave y continua: para llamar la atención sobre algo que se puede cobrar o abrir. */
fun Modifier.breathing(amount: Float = 0.035f, periodMs: Int = 1500): Modifier = composed {
    val s by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 1f, targetValue = 1f + amount,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathS"
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Pequeño balanceo, como una etiqueta colgada: para sellos y distintivos. */
fun Modifier.wobble(degrees: Float = 3f, periodMs: Int = 2200): Modifier = composed {
    val a by rememberInfiniteTransition(label = "wobble").animateFloat(
        initialValue = -degrees, targetValue = degrees,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wobbleA"
    )
    graphicsLayer { rotationZ = a }
}

/** Rellena el fondo con degradado de color con labio, para botones y fichas de color. */
fun Color.toyGradient(): Brush = Brush.verticalGradient(listOf(this.lighten(0.14f), this, this.deepen(0.10f)))


/** [JellyCard] con una fila dentro (la forma más común): padding, borde opcional y alineación ya resueltos. */
@Composable
fun JellyRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.Large),
    fill: Color = Color.White,
    lip: Color = defaultLip(fill),
    lipHeight: Dp = 5.dp,
    enabled: Boolean = true,
    brush: Brush? = null,
    padding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    borderColor: Color? = null,
    borderWidth: Dp = 1.5.dp,
    verticalAlignment: androidx.compose.ui.Alignment.Vertical = androidx.compose.ui.Alignment.CenterVertically,
    horizontalArrangement: androidx.compose.foundation.layout.Arrangement.Horizontal = androidx.compose.foundation.layout.Arrangement.Start,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    JellyCard(modifier, onClick, shape, fill, lip, lipHeight, enabled, brush) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .then(if (borderColor != null) Modifier.border(borderWidth, borderColor, shape) else Modifier)
                .padding(padding),
            verticalAlignment = verticalAlignment,
            horizontalArrangement = horizontalArrangement,
            content = content
        )
    }
}

/** [JellyCard] con una columna dentro. */
@Composable
fun JellyColumn(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.Large),
    fill: Color = Color.White,
    lip: Color = defaultLip(fill),
    lipHeight: Dp = 5.dp,
    enabled: Boolean = true,
    brush: Brush? = null,
    padding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    borderColor: Color? = null,
    borderWidth: Dp = 1.5.dp,
    horizontalAlignment: androidx.compose.ui.Alignment.Horizontal = androidx.compose.ui.Alignment.Start,
    verticalArrangement: androidx.compose.foundation.layout.Arrangement.Vertical = androidx.compose.foundation.layout.Arrangement.Top,
    /** Rellena también el alto de la tarjeta (para repartir el contenido cuando la tarjeta tiene altura fija). */
    fillHeight: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    JellyCard(modifier, onClick, shape, fill, lip, lipHeight, enabled, brush) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                .then(if (borderColor != null) Modifier.border(borderWidth, borderColor, shape) else Modifier)
                .padding(padding),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}


/**
 * Sustituto directo de `material3.Surface` con el aspecto de juguete (labio + brillo). Conserva los mismos parámetros para
 * poder cambiar `Surface(` por `JellySurface(` sin tocar nada más. Sin elevación ni clic, es un contenedor plano y sin labio.
 */
@Composable
fun JellySurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(Radius.Large),
    color: Color = Color.White,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val raised = onClick != null || shadowElevation > 0.dp
    val opaque = color.copy(alpha = 1f)
    JellyCard(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        fill = opaque,
        lip = if (color.alpha < 0.5f) Color.Transparent else defaultLip(opaque),
        lipHeight = if (raised && color.alpha >= 0.5f) 5.dp else 0.dp,
        enabled = enabled,
        brush = if (color.alpha < 1f) androidx.compose.ui.graphics.SolidColor(color) else null,
        fillContent = true
    ) {
        Box(Modifier.then(if (border != null) Modifier.border(border, shape) else Modifier), propagateMinConstraints = true) { content() }
    }
}


/** Aparición de diálogos y premios: crece desde un poco más pequeño con un rebote suave. */
fun Modifier.popIn(startScale: Float = 0.86f): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f)) }
    graphicsLayer {
        val sc = startScale + (1f - startScale) * progress.value
        scaleX = sc; scaleY = sc
        alpha = (progress.value * 2f).coerceIn(0f, 1f)
    }
}

/**
 * Sustituto de `AlertDialog` de Material con la identidad del juego: tarjeta de juguete con labio, aparición con rebote,
 * título grande y botones alineados a la derecha. Acepta los mismos parámetros de uso común para poder intercambiarlo.
 */
@Composable
fun ToyAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.XLarge),
    containerColor: Color = Color.White,
    iconContentColor: Color = Color.Unspecified,
    titleContentColor: Color = Color.Unspecified,
    textContentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties()
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        JellyCard(
            modifier = modifier.fillMaxWidth().popIn(),
            shape = shape, fill = containerColor.copy(alpha = 1f), lipHeight = 7.dp
        ) {
            androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
                if (icon != null) {
                    Box(Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)) { icon() }
                    androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                }
                if (title != null) { title(); androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp)) }
                if (text != null) { text(); androidx.compose.foundation.layout.Spacer(Modifier.height(18.dp)) }
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}

