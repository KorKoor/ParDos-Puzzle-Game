package com.korkoor.pardos.ui.game.components

import androidx.compose.ui.zIndex
import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
        ShapeType.SQUARE -> RoundedCornerShape(percent = 20)
        ShapeType.DIAMOND -> RoundedCornerShape(percent = 15)
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
    modifier: Modifier = Modifier,
    /** Mano y resaltado del tutorial o de la pista (null = nada). */
    guide: BoardGuide? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dragState = remember { DragGestureState() }
    val gridSize = state.boardSize
    val economyForBoard = remember { com.korkoor.pardos.data.local.EconomyManager(context) }
    val equippedSkin by economyForBoard.equippedSkin.collectAsState()
    val equippedFx by economyForBoard.equippedFx.collectAsState()

    // 🚀 MEJORA: Definimos rangos de tamaño para expansión masiva
    val isLargeGrid = gridSize >= 4

    val boardShape = getShape(shapeType, isLargeGrid)

    // 🛠️ AJUSTE DE ESPACIOS: Tableros grandes ocupan más pantalla, 3x3 se queda normal
    val outerPadding = when {
        gridSize >= 5 -> 2.dp  // Casi al borde para 5x5 y 6x6
        gridSize == 4 -> 6.dp  // Expandido para 4x4
        else -> 16.dp          // Normal para 3x3
    }

    val spacing = when {
        gridSize >= 5 -> 2.dp  // Espacio mínimo entre fichas
        gridSize == 4 -> 5.dp  // Espacio medio
        else -> 10.dp          // Espacio original
    }

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
          Box(Modifier.fillMaxSize()) {
            BoxWithConstraints(
                modifier = Modifier
                    .padding(outerPadding)
                    // 🛠️ FIX: Escuchamos el cambio de modo selección para bloquear/permitir gestos
                    .pointerInput(gridSize, viewModel.isSelectModeActive) {
                        if (!viewModel.isSelectModeActive) {
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
                    }
            ) {
                val availableWidth = maxWidth - (spacing * (gridSize - 1))
                val tileSize = availableWidth / gridSize

                // 1. CAPA DE FONDO (Grilla). Las piedras del nivel ocupan casillas que no se pueden usar.
                val stones = remember(state.blocked) { state.blocked.toSet() }
                Box(Modifier.fillMaxSize()) {
                    repeat(gridSize) { row ->
                        repeat(gridSize) { col ->
                            val xPos = (tileSize + spacing) * col
                            val yPos = (tileSize + spacing) * row

                            if ((row to col) in stones) {
                                StoneBlock(
                                    size = tileSize,
                                    index = row * gridSize + col,
                                    resetKey = state.currentLevel * 2 + if (state.moveCount == 0) 1 else 0,
                                    modifier = Modifier.offset(x = xPos, y = yPos)
                                )
                                return@repeat
                            }

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
                            skin = equippedSkin,
                            fx = equippedFx,
                            tile = tile,
                            tileSize = tileSize,
                            spacing = spacing,
                            shapeName = shapeType,
                            isLargeGrid = isLargeGrid,
                            currentTheme = currentTheme,
                            isSelectMode = viewModel.isSelectModeActive,
                            isFirstSelected = viewModel.firstSelectedTileId == tile.id,
                            onClick = {
                                // 🛠️ Aquí se detecta el toque para PowerUps (Eliminar, Doblar, etc)
                                if (viewModel.isSelectModeActive) {
                                    viewModel.handleTileClick(tile.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    }
                }

                // 2b. GUÍA (tutorial / pista): halos y mano deslizando
                if (guide != null && !viewModel.isSelectModeActive) {
                    GuideOverlay(guide, tileSize, spacing, gridSize)
                }

                // 3. CAPA DE PUNTOS FLOTANTES
                val scores = viewModel.floatingScores.toList()
                scores.forEach { score ->
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

                // Solo mostramos la flecha de dirección si no estamos eligiendo fichas
                if (!viewModel.isSelectModeActive) {
                    DirectionIndicator(
                        direction = dragState.currentDirection,
                        modifier = Modifier.align(Alignment.Center),
                        color = currentTheme.accentColor
                    )
                }
            }
            // Aura de flow: el halo del tablero crece con las jugadas seguidas que fusionan
            FlowAura(streak = viewModel.flowStreak, callout = viewModel.flowCallout)
          }
        }
    }
}

/**
 * Piedra del nivel: una roca con relieve, una cara iluminada y grietas. No se mueve ni se fusiona.
 * Entra con un pequeño rebote (escalonado por casilla) cada vez que empieza el nivel.
 */
@Composable
private fun StoneBlock(size: Dp, index: Int, resetKey: Int, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(resetKey) {
        pop.snapTo(0.4f)
        kotlinx.coroutines.delay(60L * (index % 6))
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
    }
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) }
    ) {
        val w = this.size.width
        val h = this.size.height
        val rock = Path().apply {
            moveTo(w * 0.17f, h * 0.30f)
            quadraticBezierTo(w * 0.25f, h * 0.09f, w * 0.52f, h * 0.11f)
            quadraticBezierTo(w * 0.80f, h * 0.09f, w * 0.89f, h * 0.33f)
            quadraticBezierTo(w * 0.95f, h * 0.62f, w * 0.83f, h * 0.82f)
            quadraticBezierTo(w * 0.62f, h * 0.95f, w * 0.33f, h * 0.91f)
            quadraticBezierTo(w * 0.09f, h * 0.85f, w * 0.07f, h * 0.58f)
            quadraticBezierTo(w * 0.05f, h * 0.40f, w * 0.17f, h * 0.30f)
            close()
        }
        // sombra apoyada en el suelo
        translate(0f, h * 0.045f) { drawPath(rock, Color(0xFF3D405B).copy(alpha = 0.22f)) }
        // cuerpo
        drawPath(
            rock,
            Brush.linearGradient(
                0f to Color(0xFFD3D8E2), 0.55f to Color(0xFFA2AABB), 1f to Color(0xFF7C869B),
                start = Offset(w * 0.2f, h * 0.1f), end = Offset(w * 0.8f, h * 0.95f)
            )
        )
        // cara iluminada (arriba a la izquierda)
        val light = Path().apply {
            moveTo(w * 0.20f, h * 0.31f)
            quadraticBezierTo(w * 0.28f, h * 0.15f, w * 0.50f, h * 0.17f)
            quadraticBezierTo(w * 0.66f, h * 0.18f, w * 0.60f, h * 0.30f)
            quadraticBezierTo(w * 0.42f, h * 0.30f, w * 0.30f, h * 0.46f)
            quadraticBezierTo(w * 0.18f, h * 0.45f, w * 0.20f, h * 0.31f)
            close()
        }
        drawPath(light, Color.White.copy(alpha = 0.34f))
        // grietas
        val crack = Color(0xFF59627A).copy(alpha = 0.55f)
        val cw = w * 0.028f
        drawLine(crack, Offset(w * 0.52f, h * 0.36f), Offset(w * 0.58f, h * 0.52f), cw, StrokeCap.Round)
        drawLine(crack, Offset(w * 0.58f, h * 0.52f), Offset(w * 0.50f, h * 0.66f), cw, StrokeCap.Round)
        drawLine(crack, Offset(w * 0.58f, h * 0.52f), Offset(w * 0.72f, h * 0.58f), cw, StrokeCap.Round)
        // contorno
        drawPath(rock, Color(0xFF5B647A).copy(alpha = 0.55f), style = Stroke(width = w * 0.03f))
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AnimatedTile(
    skin: com.korkoor.pardos.domain.shop.TileSkin,
    fx: com.korkoor.pardos.domain.shop.MergeFx,
    tile: TileModel,
    tileSize: Dp,
    spacing: Dp,
    shapeName: String,
    isLargeGrid: Boolean,
    currentTheme: GameTheme,
    isSelectMode: Boolean,
    isFirstSelected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SelectionGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "GlowAlpha"
    )

    val scaleAnim = remember { Animatable(0f) }
    // Cuenta las fusiones de esta ficha para disparar el efecto cosmético (no en la primera composición)
    var mergeCount by remember { mutableIntStateOf(0) }
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(tile.value) {
        if (!tile.isNew && !firstRun) mergeCount++
        firstRun = false
        if (tile.isNew) {
            // Aparición: crece desde pequeña con rebote suave
            scaleAnim.snapTo(0.2f)
            scaleAnim.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 350f))
        } else {
            // Fusión: "pop" de gelatina
            val targetScale = if (tile.value > 128) 1.22f else 1.16f
            scaleAnim.snapTo(targetScale)
            scaleAnim.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
        }
    }

    val selectionScale by animateFloatAsState(
        targetValue = if (isFirstSelected) 0.85f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SelectionScale"
    )

    val targetX = (tileSize + spacing) * tile.col
    val targetY = (tileSize + spacing) * tile.row

    val moveSpec = spring<Dp>(dampingRatio = 0.75f, stiffness = 400f)
    val animX by animateDpAsState(targetX, moveSpec, label = "x")
    val animY by animateDpAsState(targetY, moveSpec, label = "y")

    val shape = getShape(shapeName, isLargeGrid)
    val isDiamond = shapeName == "Diamante" && !isLargeGrid

    val look = tileLook(skin, tile.value, currentTheme)
    val backgroundColor = look.background
    val textColor = look.text

    MergeBurst(fx, mergeCount, tile.value, tileSize, Modifier.offset(animX, animY).zIndex(5f))

    Box(
        modifier = Modifier
            .size(tileSize)
            .offset(animX, animY)
            // 🚀 FIX: El clickable va ANTES del graphicsLayer para asegurar el área de toque
            // Usamos un InteractionSource vacío para que el click sea instantáneo y no bloquee
            .clickable(
                enabled = isSelectMode,
                onClick = onClick,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
            .graphicsLayer {
                val baseScale = if (isDiamond) 0.72f else 1f
                scaleX = scaleAnim.value * baseScale * selectionScale
                scaleY = scaleAnim.value * baseScale * selectionScale

                if (isDiamond) rotationZ = 45f

                shadowElevation = look.elevation.dp.toPx()
                this.shape = shape
                clip = true
            }
            .background(backgroundColor)
            .then(if (look.border != null) Modifier.border(if (look.glow != null) 2.dp else 1.5.dp, look.border, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (look.gloss) Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Brillo tipo gelatina: luz arriba, sombra suave abajo
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.30f),
                        0.45f to Color.White.copy(alpha = 0.04f),
                        1f to Color(0xFF3D405B).copy(alpha = 0.10f)
                    )
                )
        )

        if (isSelectMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = if (isFirstSelected) 4.dp else 2.dp,
                        color = if (isFirstSelected) Color.White else currentTheme.accentColor.copy(alpha = glowAlpha),
                        shape = shape
                    )
                    .background(
                        if (isFirstSelected) currentTheme.accentColor.copy(alpha = 0.4f)
                        else Color.Transparent
                    )
            )
        }

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

/**
 * Paleta "Toy & Jelly": crema cálido → arena → terracota → salvia → azul → violeta → noche.
 * Usa los mismos colores de identidad de la app (terracota, salvia, arena, navy).
 */
@Composable
fun getTileColor(value: Int, theme: GameTheme): Color {
    if (value >= 4096) return theme.accentColor
    return when (value) {
        2 -> Color(0xFFF5ECDF)
        4 -> Color(0xFFF2DFC2)
        8 -> Color(0xFFF2CC8F)
        16 -> Color(0xFFEDB27A)
        32 -> Color(0xFFE5906A)
        64 -> Color(0xFFD9694C)
        128 -> Color(0xFF81B29A)
        256 -> Color(0xFF5FA08A)
        512 -> Color(0xFF4E8FA6)
        1024 -> Color(0xFF7A74E0)
        2048 -> Color(0xFF3D405B)
        else -> theme.accentColor
    }
}

@Composable
fun getTileTextColor(value: Int): Color {
    return when {
        value <= 16 -> Color(0xFF5C4F44)
        value >= 2048 -> Color(0xFFF2CC8F)
        else -> Color(0xFFFFFBF5)
    }
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

data class FloatingScoreModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val value: Int,
    val col: Int,
    val row: Int,
    val timestamp: Long = System.currentTimeMillis()
)

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
    val currentOffset = -floatUpDistance * animState.value
    val currentAlpha = 1f - animState.value
    val currentScale = 0.5f + (animState.value * 0.5f)

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