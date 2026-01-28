package com.korkoor.pardos.ui.game.components

import FloatingScore
import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.korkoor.pardos.R
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.domain.model.TileModel
import com.korkoor.pardos.ui.game.FloatingScore
import com.korkoor.pardos.ui.game.GameViewModel
import com.korkoor.pardos.ui.theme.GameTheme
import kotlin.math.abs

// ============================================================================
// 1. FORMAS "CANDY & TOY" (Octágono + Estilos Suaves)
// ============================================================================

enum class ShapeType(val displayName: String) {
    SQUARE("Cuadrado"),
    CIRCLE("Círculo"),
    TRIANGLE("Triángulo"),
    DIAMOND("Diamante"),
    OCTAGON("Octágono");

    companion object {
        fun fromDisplayName(name: String): ShapeType =
            entries.find { it.displayName == name } ?: SQUARE
    }
}

fun getShape(shapeName: String): Shape {
    return when (ShapeType.fromDisplayName(shapeName)) {
        ShapeType.CIRCLE -> CircleShape
        ShapeType.SQUARE -> RoundedCornerShape(26.dp)
        ShapeType.TRIANGLE -> SoftTriangleShape
        ShapeType.DIAMOND -> RoundedCornerShape(18.dp) // Radio suave para el rombo
        ShapeType.OCTAGON -> SoftOctagonShape
    }
}

// 🔥 OCTÁGONO SOFT
val SoftOctagonShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val cut = w * 0.29f
    val r = w * 0.12f

    val p1 = Offset(cut, 0f); val p2 = Offset(w - cut, 0f)
    val p3 = Offset(w, cut); val p4 = Offset(w, h - cut)
    val p5 = Offset(w - cut, h); val p6 = Offset(cut, h)
    val p7 = Offset(0f, h - cut); val p8 = Offset(0f, cut)

    moveTo(p1.x + r, p1.y)
    lineTo(p2.x - r, p2.y); quadraticBezierTo(p2.x, p2.y, p2.x + r/1.5f, p2.y + r/1.5f)
    lineTo(p3.x - r/1.5f, p3.y - r/1.5f); quadraticBezierTo(p3.x, p3.y, p3.x, p3.y + r)
    lineTo(p4.x, p4.y - r); quadraticBezierTo(p4.x, p4.y, p4.x - r/1.5f, p4.y + r/1.5f)
    lineTo(p5.x + r/1.5f, p5.y - r/1.5f); quadraticBezierTo(p5.x, p5.y, p5.x - r, p5.y)
    lineTo(p6.x + r, p6.y); quadraticBezierTo(p6.x, p6.y, p6.x - r/1.5f, p6.y - r/1.5f)
    lineTo(p7.x + r/1.5f, p7.y + r/1.5f); quadraticBezierTo(p7.x, p7.y, p7.x, p7.y - r)
    lineTo(p8.x, p8.y + r); quadraticBezierTo(p8.x, p8.y, p8.x + r/1.5f, p8.y - r/1.5f)
    lineTo(p1.x - r/1.5f, p1.y + r/1.5f); quadraticBezierTo(p1.x, p1.y, p1.x + r, p1.y)
    close()
}

// 🔥 TRIÁNGULO SOFT
val SoftTriangleShape = GenericShape { size, _ ->
    val w = size.width; val h = size.height
    val r = w * 0.18f
    val top = Offset(w / 2, 0f)
    val botRight = Offset(w, h); val botLeft = Offset(0f, h)

    moveTo(top.x - r, top.y + r); quadraticBezierTo(top.x, top.y, top.x + r, top.y + r)
    lineTo(botRight.x - r, botRight.y - r/2); quadraticBezierTo(botRight.x, botRight.y, botRight.x - 2*r, botRight.y)
    lineTo(botLeft.x + 2*r, botLeft.y); quadraticBezierTo(botLeft.x, botLeft.y, botLeft.x + r, botLeft.y - r/2)
    close()
}

// ============================================================================
// 2. COMPONENTE PRINCIPAL
// ============================================================================

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BoardDisplay(
    state: BoardState,
    viewModel: GameViewModel,
    shapeType: String,
    haptic: HapticFeedback,
    currentTheme: GameTheme,
    onMoveSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dragState = remember { DragGestureState() }
    val boardShape = getShape(shapeType)
    val gridSize = state.boardSize
    val outerPadding = if (gridSize >= 5) 10.dp else 16.dp
    val spacing = if (gridSize >= 5) 6.dp else 10.dp
    val cornerRadius = 40.dp

    val isDiamond = shapeType == "Diamante"

    // Clave para recomponer si cambia el tamaño o la forma
    key(gridSize, shapeType) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = currentTheme.accentColor.copy(alpha = 0.3f),
                    spotColor = currentTheme.accentColor.copy(alpha = 0.2f)
                ),
            color = currentTheme.surfaceColor.copy(alpha = 0.96f),
            shape = RoundedCornerShape(cornerRadius)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .padding(outerPadding)
                    .pointerInput(gridSize) {
                        detectDragGestures(
                            onDragStart = { dragState.reset() },
                            onDragEnd = { dragState.complete() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragState.handleDrag(dragAmount) { dir ->
                                    viewModel.onMove(dir) { haptic.performHapticFeedback(it) }
                                    onMoveSound()
                                }
                            }
                        )
                    }
            ) {
                val tileSize = maxWidth / gridSize

                // 1. CAPA FONDO (HUECOS)
                // Ahora los huecos rotan si es modo diamante, solucionando el bug visual
                Box(Modifier.fillMaxSize()) {
                    repeat(gridSize) { row ->
                        repeat(gridSize) { col ->
                            Box(
                                modifier = Modifier
                                    .size(tileSize)
                                    .offset(x = tileSize * col, y = tileSize * row)
                                    .padding(spacing)
                                    .graphicsLayer {
                                        if (isDiamond) {
                                            rotationZ = 45f
                                            scaleX = 0.72f
                                            scaleY = 0.72f
                                        }
                                        this.shape = boardShape
                                        clip = true
                                    }
                                    .background(currentTheme.mainTextColor.copy(alpha = 0.06f))
                            )
                        }
                    }
                }

                // 2. CAPA FICHAS
                state.tiles.forEach { tile ->
                    key(tile.id) {
                        AnimatedTile(
                            tile = tile,
                            tileSize = tileSize,
                            spacing = spacing,
                            shapeName = shapeType,
                            currentTheme = currentTheme
                        )
                    }
                }

                // 🔥 NUEVA CAPA DE PUNTOS FLOTANTES
                // Se dibuja encima del tablero pero dentro del área de juego
                viewModel.floatingScores.forEach { score ->
                    key(score.id) {
                        // Obtenemos el ancho de cada celda aproximado (asumiendo que BoardDisplay llena el Box)
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            // val tileSize = maxWidth / state.boardSize
                            FloatingScore(
                                score = score,
                                tileSize = tileSize,
                                onFinished = { id -> viewModel.removeFloatingScore(id) }
                            )
                        }
                    }
                }

                // 3. INDICADOR DIRECCIÓN (Flecha visual al arrastrar)
                DirectionIndicator(
                    direction = dragState.currentDirection,
                    modifier = Modifier.align(Alignment.Center),
                    color = currentTheme.accentColor
                )
            }
        }
    }
}

@Composable
private fun AnimatedTile(
    tile: TileModel,
    tileSize: Dp,
    spacing: Dp,
    shapeName: String,
    currentTheme: GameTheme
) {
    // --- ANIMACIÓN "JELLY" (Squash & Stretch) ---
    // Cuando la ficha cambia de valor (fusión) o se crea, hace un rebote.
    val scaleAnim = remember { Animatable(0f) }
    LaunchedEffect(tile.value) {
        // Efecto de pop más exagerado para valores altos
        val targetScale = if (tile.value > 128) 1.2f else 1.15f
        scaleAnim.snapTo(targetScale)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
        )
    }

    // Movimiento suave de la ficha
    val moveSpec = spring<Dp>(dampingRatio = 0.75f, stiffness = 400f)
    val animX by animateDpAsState(tileSize * tile.col, moveSpec, label = "x")
    val animY by animateDpAsState(tileSize * tile.row, moveSpec, label = "y")

    val shape = getShape(shapeName)
    val isDiamond = shapeName == "Diamante"
    val backgroundColor = getTileColor(tile.value, currentTheme)
    val textColor = getTileTextColor(tile.value)

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(animX, animY)
            .padding(spacing)
            // 🔥 FIX SOMBRA Y ROTACIÓN:
            // Usamos graphicsLayer para aplicar la rotación AL CONTENEDOR completo.
            // Esto hace que la sombra se genere con la forma rotada correcta.
            .graphicsLayer {
                val baseScale = if (isDiamond) 0.72f else 1f
                scaleX = scaleAnim.value * baseScale
                scaleY = scaleAnim.value * baseScale

                if (isDiamond) {
                    rotationZ = 45f
                }

                // Elevación dinámica: las fichas más grandes "flotan" más alto
                shadowElevation = if (tile.value >= 128) 8.dp.toPx() else 3.dp.toPx()
                this.shape = shape
                clip = true
            }
            .background(backgroundColor)
    ) {
        // --- EFECTO GLOSS (Brillo de caramelo) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f)
                    )
                )
        )

        // Texto del número
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${tile.value}",
                fontSize = if (tile.value > 1000) 20.sp else 28.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                // Si la ficha está rotada (diamante), contra-rotamos el texto para que se lea recto
                modifier = Modifier.rotate(if (isDiamond) -45f else 0f)
            )
        }
    }
}

// --- UTILIDADES DE COLOR (Paleta Café Pastel Aesthetic) ---
@Composable
fun getTileColor(value: Int, theme: GameTheme): Color {
    // Usamos el color de acento para fichas "Legendarias"
    if (value >= 4096) return theme.accentColor

    // Paleta "Coffee Shop" (Tonos pastel, cremas y cafés suaves)
    return when (value) {
        2 -> Color(0xFFEEE4DA)      // Leche / Crema (Igual, base limpia)
        4 -> Color(0xFFEFE0C9)      // Vainilla Suave
        8 -> Color(0xFFEBCDAA)      // Latte Claro
        16 -> Color(0xFFE6B89C)     // Durazno / Café con leche muy claro
        32 -> Color(0xFFDDA684)     // Cappuccino suave
        64 -> Color(0xFFD49372)     // Canela / Terracota suave
        128 -> Color(0xFFECC271)    // Dorado Miel (Brillante pero pastel)
        256 -> Color(0xFFEBC662)    // Caramelo Claro
        512 -> Color(0xFFE9C052)    // Mostaza Dorada
        1024 -> Color(0xFFE7B843)   // Toffee
        2048 -> Color(0xFFE5B032)   // Oro Viejo
        else -> theme.accentColor
    }
}

@Composable
fun getTileTextColor(value: Int): Color {
    val darkText = Color(0xFF776E65) // Gris pardo oscuro (para fondos claros)
    val lightText = Color(0xFFF9F6F2) // Blanco crema (para fondos oscuros/intensos)

    // Los números 2 y 4 son muy claros, el resto ya tiene suficiente contraste para texto blanco
    return if (value < 8) darkText else lightText
}

@Composable
private fun DirectionIndicator(direction: Direction?, modifier: Modifier, color: Color) {
    AnimatedVisibility(
        visible = direction != null,
        modifier = modifier,
        enter = fadeIn() + scaleIn(spring(dampingRatio = 0.5f)),
        exit = fadeOut() + scaleOut(targetScale = 1.5f)
    ) {
        val rot = when (direction) {
            Direction.UP -> -90f
            Direction.DOWN -> 90f
            Direction.LEFT -> 180f
            Direction.RIGHT -> 0f
            else -> 0f
        }
        Box(
            modifier = Modifier
                .size(100.dp)
                .rotate(rot)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// ✨ NUEVO: COMPONENTE DE TEXTO FLOTANTE PARA PUNTAJES
@Composable
fun FloatingScore(
    score: com.korkoor.pardos.ui.game.components.FloatingScoreModel, // Asegúrate de importar tu modelo
    tileSize: Dp,
    onFinished: (String) -> Unit
) {
    val animState = remember { Animatable(0f) }

    LaunchedEffect(score.id) {
        animState.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        onFinished(score.id)
    }

    val floatUpDistance = 80.dp
    val currentOffset = tileSize * 0.2f - (floatUpDistance * animState.value)
    val currentAlpha = 1f - animState.value
    val currentScale = 0.5f + (animState.value * 0.5f)

    val xPos = (tileSize * score.col) + (tileSize / 3)
    val yPos = (tileSize * score.row) + (tileSize / 2)

    Text(
        text = "+${score.value}",
        color = Color(0xFF3D405B).copy(alpha = currentAlpha),
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(x = xPos, y = yPos + currentOffset)
            .scale(currentScale)
            .alpha(currentAlpha)
    )
}

// Definición simple del modelo si no quieres crear un archivo separado
data class FloatingScoreModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val value: Int,
    val col: Int,
    val row: Int,
    val timestamp: Long = System.currentTimeMillis()
)

private class DragGestureState {
    var totalDragX by mutableStateOf(0f); var totalDragY by mutableStateOf(0f)
    var hasMoved by mutableStateOf(false); var currentDirection by mutableStateOf<Direction?>(null)

    fun reset() {
        totalDragX = 0f; totalDragY = 0f
        hasMoved = false; currentDirection = null
    }

    fun complete() { currentDirection = null }

    fun handleDrag(dragAmount: Offset, onMove: (Direction) -> Unit) {
        if (hasMoved) return
        totalDragX += dragAmount.x
        totalDragY += dragAmount.y

        val sensitivity = 15f // Sensibilidad mínima para detectar intención
        val threshold = 50f   // Distancia para ejecutar el movimiento

        // Detectar dirección visualmente antes de ejecutar
        if (abs(totalDragX) > sensitivity || abs(totalDragY) > sensitivity) {
            currentDirection = if (abs(totalDragX) > abs(totalDragY)) {
                if (totalDragX > 0) Direction.RIGHT else Direction.LEFT
            } else {
                if (totalDragY > 0) Direction.DOWN else Direction.UP
            }
        }

        // Ejecutar movimiento
        if (abs(totalDragX) > threshold || abs(totalDragY) > threshold) {
            currentDirection?.let {
                onMove(it)
                hasMoved = true
            }
        }
    }
}