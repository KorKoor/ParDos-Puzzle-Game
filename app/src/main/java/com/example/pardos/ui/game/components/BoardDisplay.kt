package com.korkoor.pardos.ui.game.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.domain.model.TileModel
import com.korkoor.pardos.ui.game.GameViewModel
import com.korkoor.pardos.ui.theme.GameTheme
import kotlin.math.abs

// ============================================================================
// 1. FORMAS "SMART" (Adaptables por porcentaje)
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

fun getShape(shapeName: String, isLargeGrid: Boolean): Shape {
    val type = ShapeType.fromDisplayName(shapeName)

    return when (type) {
        ShapeType.CIRCLE -> CircleShape

        // 🔥 SOLUCIÓN DEL BUG: Usamos percent = 20.
        // Esto significa "redondea solo el 20% de la esquina".
        ShapeType.SQUARE -> RoundedCornerShape(percent = 20)

        // Para el diamante también usamos porcentaje
        ShapeType.DIAMOND -> RoundedCornerShape(percent = 15)

        // Si el tablero es muy denso, suavizamos
        ShapeType.TRIANGLE -> if (isLargeGrid) RoundedCornerShape(percent = 20) else SoftTriangleShape
        ShapeType.OCTAGON -> SoftOctagonShape
    }
}

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

val SoftTriangleShape = GenericShape { size, _ ->
    val w = size.width; val h = size.height
    val r = w * 0.15f
    val top = Offset(w / 2, 0f)
    val botRight = Offset(w, h); val botLeft = Offset(0f, h)

    moveTo(top.x - r, top.y + r); quadraticBezierTo(top.x, top.y, top.x + r, top.y + r)
    lineTo(botRight.x - r, botRight.y - r/2); quadraticBezierTo(botRight.x, botRight.y, botRight.x - 2*r, botRight.y)
    lineTo(botLeft.x + 2*r, botLeft.y); quadraticBezierTo(botLeft.x, botLeft.y, botLeft.x + r, botLeft.y - r/2)
    close()
}

// ============================================================================
// 2. COMPONENTE DE TABLERO
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
    val gridSize = state.boardSize
    val isLargeGrid = gridSize >= 5

    val boardShape = getShape(shapeType, isLargeGrid)

    // Ajustes de espaciado
    val outerPadding = if (isLargeGrid) 8.dp else 16.dp
    val spacing = if (isLargeGrid) 4.dp else 10.dp
    val cornerRadius = 24.dp
    val isDiamond = shapeType == "Diamante" && !isLargeGrid

    key(gridSize, shapeType) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = currentTheme.accentColor.copy(alpha = 0.2f),
                    spotColor = currentTheme.accentColor.copy(alpha = 0.15f)
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
                // Cálculo matemático del tamaño de celda
                val availableWidth = maxWidth - (spacing * (gridSize - 1))
                val tileSize = availableWidth / gridSize

                // 1. CAPA DE FONDO
                Box(Modifier.fillMaxSize()) {
                    repeat(gridSize) { row ->
                        repeat(gridSize) { col ->
                            val xPos = (tileSize + spacing) * col
                            val yPos = (tileSize + spacing) * row

                            Box(
                                modifier = Modifier
                                    .size(tileSize)
                                    .offset(x = xPos, y = yPos)
                                    .graphicsLayer {
                                        if (isDiamond) {
                                            rotationZ = 45f
                                            scaleX = 0.72f; scaleY = 0.72f
                                        }
                                        this.shape = boardShape
                                        clip = true
                                    }
                                    .background(currentTheme.mainTextColor.copy(alpha = 0.06f))
                            )
                        }
                    }
                }

                // 2. CAPA DE FICHAS ANIMADAS
                state.tiles.forEach { tile ->
                    key(tile.id) {
                        AnimatedTile(
                            tile = tile,
                            tileSize = tileSize,
                            spacing = spacing,
                            shapeName = shapeType,
                            isLargeGrid = isLargeGrid,
                            currentTheme = currentTheme
                        )
                    }
                }

                // 3. CAPA DE PUNTOS FLOTANTES
                // 🔥 AQUÍ SE ARREGLAN LOS ERRORES DE REFERENCIA 🔥
                // Convertimos explícitamente la lista para que el compilador sepa el tipo
                val scores = viewModel.floatingScores.toList()
                scores.forEach { score ->
                    // Verificamos que sea del tipo correcto (aunque debería serlo por inferencia)
                    if (score is FloatingScoreModel) {
                        key(score.id) {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                FloatingScore(
                                    score = score,
                                    tileSize = tileSize,
                                    onFinished = { id -> viewModel.removeFloatingScore(id) }
                                )
                            }
                        }
                    }
                }

                DirectionIndicator(
                    direction = dragState.currentDirection,
                    modifier = Modifier.align(Alignment.Center),
                    color = currentTheme.accentColor
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AnimatedTile(
    tile: TileModel,
    tileSize: Dp,
    spacing: Dp,
    shapeName: String,
    isLargeGrid: Boolean,
    currentTheme: GameTheme
) {
    val scaleAnim = remember { Animatable(0f) }
    LaunchedEffect(tile.value) {
        val targetScale = if (tile.value > 128) 1.2f else 1.15f
        scaleAnim.snapTo(targetScale)
        scaleAnim.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
    }

    val targetX = (tileSize + spacing) * tile.col
    val targetY = (tileSize + spacing) * tile.row

    val moveSpec = spring<Dp>(dampingRatio = 0.75f, stiffness = 400f)
    val animX by animateDpAsState(targetX, moveSpec, label = "x")
    val animY by animateDpAsState(targetY, moveSpec, label = "y")

    val shape = getShape(shapeName, isLargeGrid)
    val isDiamond = shapeName == "Diamante" && !isLargeGrid

    val backgroundColor = getTileColor(tile.value, currentTheme)
    val textColor = getTileTextColor(tile.value)

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(animX, animY)
            .graphicsLayer {
                val baseScale = if (isDiamond) 0.72f else 1f
                scaleX = scaleAnim.value * baseScale
                scaleY = scaleAnim.value * baseScale

                if (isDiamond) rotationZ = 45f

                shadowElevation = if (tile.value >= 128) 4.dp.toPx() else 2.dp.toPx()
                this.shape = shape
                clip = true
            }
            .background(backgroundColor)
    ) {
        // Efecto Gloss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                        start = Offset(0f, 0f), end = Offset(80f, 80f)
                    )
                )
        )

        // 🔥 TEXTO QUE SE ADAPTA AL TAMAÑO (SOLUCIÓN NÚMEROS GRANDES) 🔥
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val text = "${tile.value}"
            val digits = text.length

            val fontSizeFactor = when {
                digits <= 2 -> 0.5f
                digits == 3 -> 0.4f
                digits == 4 -> 0.35f
                else -> 0.3f
            }

            val dynamicFontSize = (maxWidth.value * fontSizeFactor).sp

            Text(
                text = text,
                fontSize = dynamicFontSize,
                fontWeight = FontWeight.Black,
                color = textColor,
                modifier = Modifier.rotate(if (isDiamond) -45f else 0f),
                softWrap = false
            )
        }
    }
}

// ============================================================================
// 3. UTILIDADES Y CLASES FALTANTES
// ============================================================================

@Composable
fun getTileColor(value: Int, theme: GameTheme): Color {
    if (value >= 4096) return theme.accentColor
    return when (value) {
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEFE0C9)
        8 -> Color(0xFFEBCDAA)
        16 -> Color(0xFFE6B89C)
        32 -> Color(0xFFDDA684)
        64 -> Color(0xFFD49372)
        128 -> Color(0xFFECC271)
        256 -> Color(0xFFEBC662)
        512 -> Color(0xFFE9C052)
        1024 -> Color(0xFFE7B843)
        2048 -> Color(0xFFE5B032)
        else -> theme.accentColor
    }
}

@Composable
fun getTileTextColor(value: Int): Color {
    val darkText = Color(0xFF776E65)
    val lightText = Color(0xFFF9F6F2)
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

// 🔥 CLASE Y COMPOSABLE FALTANTES (DEFINIDOS AQUÍ PARA EVITAR ERRORES) 🔥

// Modelo de datos para los puntajes flotantes
data class FloatingScoreModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val value: Int,
    val col: Int,
    val row: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// Composable que dibuja el texto flotante
@Composable
fun FloatingScore(
    score: FloatingScoreModel,
    tileSize: Dp,
    onFinished: (String) -> Unit
) {
    val animState = remember { Animatable(0f) }

    LaunchedEffect(score.id) {
        animState.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        onFinished(score.id)
    }

    val floatUpDistance = 50.dp
    // Calculamos el offset en píxeles o Dp relativos
    val currentOffset = -floatUpDistance * animState.value
    val currentAlpha = 1f - animState.value
    val currentScale = 0.5f + (animState.value * 0.5f)

    // Posición centrada en la celda
    val xPos = (tileSize * score.col) + (tileSize / 4)
    val yPos = (tileSize * score.row) + (tileSize / 4)

    Text(
        text = "+${score.value}",
        color = Color(0xFF3D405B).copy(alpha = currentAlpha),
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .offset(x = xPos, y = yPos + currentOffset)
            .scale(currentScale)
            .alpha(currentAlpha)
    )
}

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

        val sensitivity = 15f
        val threshold = 50f

        if (abs(totalDragX) > sensitivity || abs(totalDragY) > sensitivity) {
            currentDirection = if (abs(totalDragX) > abs(totalDragY)) {
                if (totalDragX > 0) Direction.RIGHT else Direction.LEFT
            } else {
                if (totalDragY > 0) Direction.DOWN else Direction.UP
            }
        }

        if (abs(totalDragX) > threshold || abs(totalDragY) > threshold) {
            currentDirection?.let {
                onMove(it)
                hasMoved = true
            }
        }
    }
}