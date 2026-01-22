package com.example.pardos.ui.game.components

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.pardos.domain.logic.Direction
import com.example.pardos.domain.model.BoardState
import com.example.pardos.domain.model.TileModel
import com.example.pardos.ui.game.GameViewModel
import com.example.pardos.ui.theme.GameTheme
import kotlin.math.abs
import com.example.pardos.R

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

    // 📏 AJUSTES DINÁMICOS SEGÚN EL TAMAÑO DEL TABLERO
    val outerPadding = if (gridSize >= 5) 8.dp else 12.dp
    val spacing = if (gridSize >= 5) 4.dp else 6.dp
    val cornerRadius = if (gridSize >= 5) 16.dp else 32.dp

    // 🌍 TRADUCCIÓN PARA ACCESIBILIDAD (Semantics)
    val boardDesc = stringResource(R.string.board_desc, gridSize, gridSize)

    // Usamos key para reiniciar todo el tablero si cambia el tamaño (ej. nivel 3x3 a 4x4)
    key(gridSize) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                // Sombra coloreada del TABLERO (Contenedor general)
                .shadow(
                    elevation = if (gridSize >= 5) 15.dp else 35.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = currentTheme.accentColor.copy(alpha = 0.3f),
                    spotColor = currentTheme.accentColor.copy(alpha = 0.3f)
                )
                .border(
                    width = if (dragState.currentDirection != null) 3.dp else 1.dp,
                    brush = Brush.radialGradient(
                        listOf(currentTheme.accentColor.copy(0.6f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .semantics { contentDescription = boardDesc },
            color = currentTheme.surfaceColor,
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
                                dragState.handleDrag(dragAmount) { direction ->
                                    // 🔥 INTEGRACIÓN HÁPTICA
                                    viewModel.onMove(direction) { hapticType ->
                                        haptic.performHapticFeedback(hapticType)
                                    }
                                    onMoveSound()
                                }
                            }
                        )
                    }
            ) {
                // ✅ USO CORRECTO DEL SCOPE: Usamos maxWidth para calcular el tamaño
                val tileSize = maxWidth / gridSize

                // 1. CAPA FONDO: Celdas vacías (Grid estático)
                Box(Modifier.fillMaxSize()) {
                    repeat(gridSize) { row ->
                        repeat(gridSize) { col ->
                            Box(
                                modifier = Modifier
                                    .size(tileSize)
                                    .offset(x = tileSize * col, y = tileSize * row)
                                    .padding(spacing)
                                    .background(
                                        color = currentTheme.mainTextColor.copy(alpha = 0.08f),
                                        shape = boardShape
                                    )
                            )
                        }
                    }
                }

                // 2. CAPA FICHAS: Fichas activas con animación
                state.tiles.forEach { tile ->
                    // Usamos key(tile.id) para que Compose rastree cada ficha individualmente y anime su posición
                    key(tile.id) {
                        AnimatedTile(
                            tile = tile,
                            tileSize = tileSize,
                            spacing = spacing,
                            shape = boardShape,
                            multiplier = viewModel.currentMultiplierBase
                        )
                    }
                }

                // 3. CAPA UI: Indicador de dirección
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
    shape: Shape,
    multiplier: Int
) {
    // ✨ ANIMACIÓN "JUICY" (Pop & Pulse)
    // Usamos Animatable para controlar el rebote manualmente
    val scaleAnim = remember { Animatable(0f) }

    LaunchedEffect(tile.value) {
        // Si la escala ya es > 0, significa que la ficha ya existía y acaba de cambiar de valor (Fusión)
        if (scaleAnim.value > 0f) {
            // Efecto PULSO: Escala a 1.2x y regresa a 1.0x
            scaleAnim.snapTo(1.2f)
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.5f, // Rebote medio
                    stiffness = 1500f    // Retorno rápido
                )
            )
        } else {
            // Efecto NACIMIENTO (Pop-in): Escala de 0 a 1
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = 800f
                )
            )
        }
    }

    // 🚀 ANIMACIÓN DE MOVIMIENTO (X, Y)
    // StiffnessMediumLow hace que se sientan ágiles pero fluidas
    val animX by animateDpAsState(
        targetValue = tileSize * tile.col,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy, // Sin rebote en el desplazamiento (queremos precisión)
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "moveX"
    )

    val animY by animateDpAsState(
        targetValue = tileSize * tile.row,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "moveY"
    )

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(animX, animY) // Aplica la posición animada
            .padding(spacing)
            .graphicsLayer {
                // Aplica la escala del efecto Pop/Pulse
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                // ❌ IMPORTANTE: NO ponemos shadowElevation aquí.
                // La sombra bonita la maneja el componente Tile internamente.
            }
            // ✅ Z-INDEX: Las fichas con valor mayor van arriba para que la animación de fusión se vea limpia
            .zIndex(tile.value.toFloat()),
        contentAlignment = Alignment.Center
    ) {
        // Renderizamos la ficha visual (Tu componente Tile mejorado)
        Tile(
            tile = tile,
            base = multiplier,
            modifier = Modifier.fillMaxSize(),
            shape = shape
        )
    }
}

@Composable
private fun DirectionIndicator(direction: Direction?, modifier: Modifier, color: Color) {
    // 🌍 Traducción del indicador
    val directionText = when (direction) {
        Direction.UP -> stringResource(R.string.direction_up)
        Direction.DOWN -> stringResource(R.string.direction_down)
        Direction.LEFT -> stringResource(R.string.direction_left)
        Direction.RIGHT -> stringResource(R.string.direction_right)
        null -> ""
    }

    AnimatedVisibility(
        visible = direction != null,
        modifier = modifier,
        // Animación rápida de entrada y salida (Pop)
        enter = fadeIn(tween(150)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.7f)
    ) {
        val rotation = when (direction) {
            Direction.UP -> -90f
            Direction.DOWN -> 90f
            Direction.LEFT -> 180f
            Direction.RIGHT -> 0f
            null -> 0f
        }

        // ✨ NUEVO DISEÑO: Contenedor circular con brillo ✨
        Box(
            modifier = Modifier
                .size(90.dp) // Un poco más grande el contenedor total
                .rotate(rotation)
                // 1. Sombra de color suave (Glow effect)
                .shadow(
                    elevation = 12.dp,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    spotColor = color.copy(alpha = 0.8f), // Color del tema intenso
                    ambientColor = color.copy(alpha = 0.8f)
                )
                // 2. Fondo semitransparente del color del tema
                .background(
                    color = color.copy(alpha = 0.2f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                // 3. Borde sutil para definición
                .border(
                    width = 2.dp,
                    color = color.copy(alpha = 0.4f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // La flecha ahora es blanca brillante y nítida dentro del halo
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = directionText,
                tint = Color.White, // Blanco puro para contraste
                modifier = Modifier.size(48.dp) // Un poco más pequeña dentro del círculo
            )
        }
    }
}

private class DragGestureState {
    var totalDragX by mutableStateOf(0f)
    var totalDragY by mutableStateOf(0f)
    var hasMoved by mutableStateOf(false)
    var currentDirection by mutableStateOf<Direction?>(null)

    fun reset() {
        totalDragX = 0f; totalDragY = 0f; hasMoved = false; currentDirection = null
    }

    fun complete() { currentDirection = null }

    fun handleDrag(dragAmount: androidx.compose.ui.geometry.Offset, onMove: (Direction) -> Unit) {
        if (hasMoved) return
        totalDragX += dragAmount.x
        totalDragY += dragAmount.y

        // Umbral de detección (20px) para evitar toques accidentales
        if (abs(totalDragX) > 20f || abs(totalDragY) > 20f) {
            currentDirection = if (abs(totalDragX) > abs(totalDragY)) {
                if (totalDragX > 0) Direction.RIGHT else Direction.LEFT
            } else {
                if (totalDragY > 0) Direction.DOWN else Direction.UP
            }
        }

        // Umbral de disparo (60px) para confirmar el movimiento
        if (abs(totalDragX) > 60f || abs(totalDragY) > 60f) {
            currentDirection?.let { onMove(it); hasMoved = true }
        }
    }
}