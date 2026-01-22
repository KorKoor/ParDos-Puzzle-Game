package com.korkoor.pardos.ui.game.components

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.korkoor.pardos.R
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.domain.model.TileModel
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
        ShapeType.DIAMOND -> RoundedCornerShape(20.dp)
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

    val boardDesc = stringResource(R.string.board_desc, gridSize, gridSize)

    key(gridSize) {
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
                Box(Modifier.fillMaxSize()) {
                    repeat(gridSize) { row ->
                        repeat(gridSize) { col ->
                            Box(
                                modifier = Modifier
                                    .size(tileSize)
                                    .offset(x = tileSize * col, y = tileSize * row)
                                    .padding(spacing)
                                    .clip(boardShape)
                                    .background(currentTheme.mainTextColor.copy(alpha = 0.06f))
                                    .rotate(if (shapeType == "Diamante") 45f else 0f)
                                    .scale(if (shapeType == "Diamante") 0.70f else 1f)
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

                // 3. INDICADOR DIRECCIÓN
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
    // ✨ ANIMACIÓN "JELLY"
    val scaleAnim = remember { Animatable(0f) }
    LaunchedEffect(tile.value) {
        if (scaleAnim.value > 0f) {
            scaleAnim.snapTo(1.35f)
            scaleAnim.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
        } else {
            scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 600f))
        }
    }

    val moveSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = 450f)
    val animX by animateDpAsState(tileSize * tile.col, moveSpec, "x")
    val animY by animateDpAsState(tileSize * tile.row, moveSpec, "y")

    val shape = getShape(shapeName)
    val isDiamond = shapeName == "Diamante"
    val backgroundColor = getTileColor(tile.value, currentTheme)
    val textColor = getTileTextColor(tile.value)

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(animX, animY)
            .padding(spacing)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                shadowElevation = if (tile.value > 128) 8.dp.toPx() else 4.dp.toPx()
                this.shape = shape
                clip = true

                // 🔥 FIX DEL ERROR DE COMPILACIÓN:
                // Usamos .hashCode() para asegurarnos de que sea un número antes de usar %
                rotationZ = (scaleAnim.value - 1f) * (if (tile.id.hashCode() % 2 == 0) 4f else -4f)
            }
            .zIndex(tile.value.toFloat()),
        contentAlignment = Alignment.Center
    ) {
        // ✨ VISUAL "CANDY" (Gomita brillante)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (isDiamond) 45f else 0f)
                .scale(if (isDiamond) 0.70f else 1f)
                .background(backgroundColor)
        ) {
            // GLOSS (Brillo superior)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            )

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "${tile.value}",
                    fontSize = if (tile.value > 1000) 18.sp else 26.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    modifier = Modifier.rotate(if (isDiamond) -45f else 0f)
                )
            }
        }
    }
}

// --- UTILIDADES DE COLOR ---
@Composable
fun getTileColor(value: Int, theme: GameTheme): Color {
    if (value >= 4096) return theme.accentColor
    return when (value) {
        2 -> Color(0xFFFAF9F6)
        4 -> Color(0xFFF4EBD9)
        8 -> Color(0xFFEBE0CC)
        16 -> Color(0xFFE6D2B5)
        32 -> Color(0xFFE0C49F)
        64 -> Color(0xFFDAB68B)
        128 -> Color(0xFFE8D5B5)
        256 -> Color(0xFFE2C9A1)
        512 -> Color(0xFFDDB685)
        1024 -> Color(0xFFD6A476)
        2048 -> Color(0xFFCC8B65)
        else -> theme.accentColor
    }
}

@Composable
fun getTileTextColor(value: Int): Color {
    val coffeeText = Color(0xFF5D534A)
    val creamText = Color(0xFFFAF9F6)
    return if (value < 1024) coffeeText else creamText
}

@Composable
private fun DirectionIndicator(direction: Direction?, modifier: Modifier, color: Color) {
    AnimatedVisibility(
        visible = direction != null,
        modifier = modifier,
        enter = fadeIn() + scaleIn(spring(dampingRatio = 0.4f)),
        exit = fadeOut() + scaleOut(targetScale = 1.2f)
    ) {
        val rot = when (direction) {
            Direction.UP -> -90f; Direction.DOWN -> 90f; Direction.LEFT -> 180f; Direction.RIGHT -> 0f; else -> 0f
        }
        Box(
            modifier = Modifier
                .size(110.dp)
                .rotate(rot)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(55.dp)
            )
        }
    }
}

private class DragGestureState {
    var totalDragX by mutableStateOf(0f); var totalDragY by mutableStateOf(0f)
    var hasMoved by mutableStateOf(false); var currentDirection by mutableStateOf<Direction?>(null)
    fun reset() { totalDragX = 0f; totalDragY = 0f; hasMoved = false; currentDirection = null }
    fun complete() { currentDirection = null }
    fun handleDrag(dragAmount: Offset, onMove: (Direction) -> Unit) {
        if (hasMoved) return
        totalDragX += dragAmount.x; totalDragY += dragAmount.y
        if (abs(totalDragX) > 15f || abs(totalDragY) > 15f) {
            currentDirection = if (abs(totalDragX) > abs(totalDragY)) {
                if (totalDragX > 0) Direction.RIGHT else Direction.LEFT
            } else { if (totalDragY > 0) Direction.DOWN else Direction.UP }
        }
        if (abs(totalDragX) > 50f || abs(totalDragY) > 50f) {
            currentDirection?.let { onMove(it); hasMoved = true }
        }
    }
}