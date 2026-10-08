package com.korkoor.pardos.ui.menu

import com.korkoor.pardos.ui.design.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.korkoor.pardos.data.local.EconomyManager
import com.korkoor.pardos.domain.rewards.ChapterRewards
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.logic.ProgressionEngine
import com.korkoor.pardos.domain.model.LevelInfo
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin


/** Niveles por capítulo del mapa. */
private const val CHAPTER_SIZE = 20
private val ROW_HEIGHT = 92.dp

private val chapterNames = listOf(
    "Jardín de Arena", "Bosque Sereno", "Orilla del Río", "Colinas de Té",
    "Faro en la Bruma", "Valle de Lavanda", "Mercado Nocturno", "Pico Nevado",
    "Isla de Cerezos", "Desierto Dorado", "Bahía Lunar", "Templo de Bambú"
)
private val chapterColors = listOf(
    Color(0xFF6B9E86), Color(0xFFE07A5F), Color(0xFFE0A93B), Color(0xFF4E8FA6), Color(0xFF7A74E0)
)

private fun chapterOf(levelId: Int) = (levelId - 1) / CHAPTER_SIZE
private fun chapterName(chapter: Int) = chapterNames[chapter % chapterNames.size]
private fun chapterColor(chapter: Int) = chapterColors[chapter % chapterColors.size]

private sealed interface MapItem {
    data class Node(val level: LevelInfo) : MapItem
    data class Banner(val chapter: Int, val stars: Int, val maxStars: Int, val completed: Int) : MapItem
    /** Cofre al final de un capítulo. [ready] = último nivel superado; [claimed] = ya abierto. */
    data class Chest(val chapter: Int, val ready: Boolean, val claimed: Boolean) : MapItem
}

/** Posición horizontal (0..1) del nodo: un zigzag suave. */
private fun nodeX(levelId: Int): Float = 0.5f + 0.27f * sin(levelId * 0.85f)

@Composable
fun LevelSelectorScreen(
    levels: List<LevelInfo>,
    currentTheme: GameTheme,
    onLevelSelected: (LevelInfo) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    // Al volver del juego se relee el progreso guardado
    LaunchedEffect(Unit) { onRefresh() }

    val context = LocalContext.current
    val economy = remember { EconomyManager(context) }
    val haptic = LocalHapticFeedback.current
    var claimTick by remember { mutableIntStateOf(0) }

    // Lista plana ascendente: banner de capítulo antes de su primer nivel.
    // La LazyColumn va en reverseLayout: el nivel 1 queda abajo y el mapa "sube".
    val items = remember(levels, claimTick) {
        buildList<MapItem> {
            levels.forEach { lvl ->
                if ((lvl.id - 1) % CHAPTER_SIZE == 0) {
                    val ch = chapterOf(lvl.id)
                    val inChapter = levels.filter { chapterOf(it.id) == ch }
                    add(
                        MapItem.Banner(
                            chapter = ch,
                            stars = inChapter.sumOf { it.starsEarned },
                            maxStars = inChapter.size * 3,
                            completed = inChapter.count { it.starsEarned > 0 }
                        )
                    )
                }
                add(MapItem.Node(lvl))
                // Cofre tras el último nivel de cada capítulo
                if (lvl.id % CHAPTER_SIZE == 0) {
                    val ch = chapterOf(lvl.id)
                    add(MapItem.Chest(ch, ready = lvl.starsEarned > 0, claimed = economy.isChapterChestClaimed(ch)))
                }
            }
        }
    }

    val currentLevel = remember(levels) { levels.lastOrNull { !it.isLocked } }
    val currentIndex = remember(items, currentLevel) {
        items.indexOfFirst { it is MapItem.Node && it.level.id == currentLevel?.id }.coerceAtLeast(0)
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<LevelInfo?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }

    // Abrir centrado en el nivel actual (y recentrar si cambia el progreso)
    LaunchedEffect(currentIndex, items.size) {
        if (items.isNotEmpty()) listState.scrollToItem((currentIndex - 3).coerceAtLeast(0))
    }

    // ¿Se ve el nivel actual? Si no, mostramos el botón "Continuar"
    val currentVisible by remember(currentIndex) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex } }
    }

    val visibleChapter by remember {
        derivedStateOf {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: currentIndex
            when (val it = items.getOrNull(first)) {
                is MapItem.Node -> chapterOf(it.level.id)
                is MapItem.Banner -> it.chapter
                is MapItem.Chest -> it.chapter
                null -> 0
            }
        }
    }

    val totalStars = remember(levels) { levels.sumOf { it.starsEarned } }

    LaunchedEffect(hint) {
        if (hint != null) {
            delay(2200)
            hint = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(currentTheme.colors))) {
        PicnicBackgroundOptimized(color = currentTheme.accentColor.copy(alpha = 0.05f))

        // Ambiente: el fondo se tiñe suavemente con el color del capítulo que estás viendo
        val ambient by animateColorAsState(chapterColor(visibleChapter).copy(alpha = 0.10f), tween(700), label = "ambient")
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ambient, Color.Transparent, ambient))))

        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 130.dp, bottom = 40.dp)
        ) {
            itemsIndexed(items, key = { _, item ->
                when (item) {
                    is MapItem.Node -> "n${item.level.id}"
                    is MapItem.Banner -> "b${item.chapter}"
                    is MapItem.Chest -> "c${item.chapter}"
                }
            }) { _, item ->
                when (item) {
                    is MapItem.Banner -> ChapterBanner(item)
                    is MapItem.Chest -> ChapterChest(
                        chest = item,
                        onClick = {
                            when {
                                item.claimed -> hint = "Ya abriste este cofre"
                                !item.ready -> hint = "Supera el nivel ${ChapterRewards.lastLevelOf(item.chapter)} para abrirlo"
                                else -> {
                                    val reward = economy.claimChapterChest(item.chapter)
                                    if (reward != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        hint = "¡Cofre abierto! +${reward.coins} monedas y +${reward.gems} gemas"
                                        claimTick++
                                    }
                                }
                            }
                        }
                    )
                    is MapItem.Node -> MapNode(
                        level = item.level,
                        nextUnlocked = levels.getOrNull(item.level.id)?.isLocked == false,
                        isLast = item.level.id >= levels.size,
                        isCurrent = item.level.id == currentLevel?.id,
                        onClick = {
                            if (item.level.isLocked) {
                                hint = "Completa el nivel ${item.level.id - 1} para desbloquearlo"
                            } else {
                                hint = null
                                selected = item.level
                            }
                        }
                    )
                }
            }
        }

        // --- Cabecera flotante ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(currentTheme.colors.first(), currentTheme.colors.first().copy(alpha = 0f))))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = CircleShape, color = Color.White, shadowElevation = 6.dp, modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Navy)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("CAMPAÑA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f), letterSpacing = 3.sp)
                    Text(
                        "Cap. ${visibleChapter + 1} · ${chapterName(visibleChapter)}",
                        fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$totalStars", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy)
                }
            }
        }

        // --- Botón para volver al nivel actual ---
        AnimatedVisibility(
            visible = !currentVisible && selected == null,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Row(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF6B9E86))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF7FB69C), Color(0xFF5A8C74))))
                    .clickable { scope.launch { listState.animateScrollToItem((currentIndex - 3).coerceAtLeast(0)) } }
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "NIVEL ${currentLevel?.id ?: 1}",
                    fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp
                )
            }
        }

        // --- Aviso de nivel bloqueado ---
        AnimatedVisibility(
            visible = hint != null && selected == null,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp, start = 24.dp, end = 24.dp),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Navy)
                    .clickable { hint = null }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(hint ?: "", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            }
        }

        // --- Vista previa del nivel (toque fuera para cerrar) ---
        if (selected != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selected = null }
            )
        }
        AnimatedVisibility(
            visible = selected != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            val level = selected
            if (level != null) {
                LevelPreviewCard(
                    level = level,
                    onPlay = {
                        selected = null
                        onLevelSelected(level)
                    }
                )
            } else {
                Spacer(Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun ChapterBanner(banner: MapItem.Banner) {
    val color = chapterColor(banner.chapter)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(color, color.copy(red = color.red * 0.8f, green = color.green * 0.8f, blue = color.blue * 0.8f))
                )
            )
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("CAPÍTULO ${banner.chapter + 1}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.75f), letterSpacing = 3.sp)
                Spacer(Modifier.height(2.dp))
                Text(chapterName(banner.chapter), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFE08A), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${banner.stars}/${banner.maxStars}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Text("${banner.completed}/$CHAPTER_SIZE niveles", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
private fun MapNode(level: LevelInfo, nextUnlocked: Boolean, isLast: Boolean, isCurrent: Boolean, onClick: () -> Unit) {
    val color = chapterColor(chapterOf(level.id))
    val locked = level.isLocked
    val completed = !locked && level.starsEarned > 0
    val isChallenge = level.id % 5 == 0
    val isEpic = level.id % 25 == 0

    val myX = nodeX(level.id)
    val nextX = nodeX(level.id + 1)
    val segmentDone = nextUnlocked
    val lineColor = if (segmentDone) color.copy(alpha = 0.55f) else Navy.copy(alpha = 0.12f)
    // Entre capítulos hay un banner: la ruta solo llega al borde de la fila
    val endsAtBanner = level.id % CHAPTER_SIZE == 0

    val pulse by rememberInfiniteTransition(label = "node").animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nodePulse"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .drawBehind {
                // Ruta hacia el nivel siguiente (arriba). El nodo siguiente se pinta después y la tapa.
                if (!isLast) {
                    val x0 = size.width * myX
                    val x1 = size.width * nextX
                    val y0 = size.height / 2f
                    val y1 = -size.height / 2f
                    val xEnd = if (endsAtBanner) x0 else x1
                    val path = Path().apply {
                        moveTo(x0, y0)
                        cubicTo(x0, y0 + (y1 - y0) * 0.55f, xEnd, y1 - (y1 - y0) * 0.55f, xEnd, y1)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 7.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = if (!segmentDone) PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 12.dp.toPx())) else null
                        )
                    )
                }
            }
    ) {
        val nodeSize = if (isCurrent) 68.dp else 58.dp
        val xDp = maxWidth * myX - nodeSize / 2

        Box(
            modifier = Modifier
                .offset(x = xDp, y = (ROW_HEIGHT - nodeSize) / 2)
                .size(nodeSize)
                .scale(if (isCurrent) pulse else 1f)
                .then(
                    when {
                        isCurrent -> Modifier.shadow(16.dp, CircleShape, spotColor = color)
                        !locked -> Modifier.shadow(6.dp, CircleShape)
                        else -> Modifier
                    }
                )
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> Brush.linearGradient(listOf(color, color.copy(red = color.red * 0.85f, green = color.green * 0.85f, blue = color.blue * 0.85f)))
                        locked -> Brush.linearGradient(listOf(Color(0xFFEDEBE6), Color(0xFFEDEBE6)))
                        else -> Brush.linearGradient(listOf(Color.White, Color.White))
                    }
                )
                .border(
                    width = if (isEpic) 3.dp else 2.dp,
                    color = when {
                        isEpic && !locked -> Gold
                        isCurrent -> Color.White
                        completed -> color
                        else -> Navy.copy(alpha = 0.08f)
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (locked) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Navy.copy(alpha = 0.28f), modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = "${level.id}",
                    fontSize = if (level.id >= 1000) 14.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isCurrent) Color.White else Navy
                )
            }
        }

        // Estrellas bajo el nodo completado
        if (completed) {
            Row(
                modifier = Modifier.offset(x = maxWidth * myX - 21.dp, y = (ROW_HEIGHT + nodeSize) / 2 - 2.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(3) { i ->
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (i < level.starsEarned) Color(0xFFFFC83D) else Navy.copy(alpha = 0.12f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Insignia de reto cronometrado
        if (isChallenge && !locked) {
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * myX + nodeSize / 2 - 16.dp, y = (ROW_HEIGHT - nodeSize) / 2 - 4.dp)
                    .size(22.dp)
                    .background(Color(0xFFE07A5F), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun LevelPreviewCard(level: LevelInfo, onPlay: () -> Unit) {
    val color = chapterColor(chapterOf(level.id))
    val size = ProgressionEngine.calculateBoardSize(level.target)
    val timeText = level.maxTime?.let { "${it / 60}:${(it % 60).toString().padStart(2, '0')}" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = Color(0xFFFFFBF5),
        shadowElevation = 20.dp
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.width(40.dp).height(4.dp).background(Navy.copy(alpha = 0.12f), CircleShape))
            Spacer(Modifier.height(16.dp))
            Text("NIVEL ${level.id}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 2.sp)
            Text(level.difficultyName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 2.sp)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Icon(
                        Icons.Rounded.Star, contentDescription = null,
                        tint = if (i < level.starsEarned) Color(0xFFFFC83D) else Navy.copy(alpha = 0.12f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PreviewStat("META", "${level.target}")
                PreviewStat("TABLERO", "${size}×$size")
                PreviewStat("TIEMPO", timeText ?: "Libre")
            }

            if (level.bestMoves > 0) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Tu mejor: ${level.bestMoves} mov.",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7FB69C), Color(0xFF5A8C74))))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (level.starsEarned > 0) "JUGAR DE NUEVO" else "JUGAR",
                        fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.4f), letterSpacing = 1.5.sp)
    }
}


@Composable
private fun ChapterChest(chest: MapItem.Chest, onClick: () -> Unit) {
    val color = chapterColor(chest.chapter)
    val state = when {
        chest.claimed -> 2
        chest.ready -> 1
        else -> 0
    }
    val lineColor = if (chest.ready) color.copy(alpha = 0.55f) else Navy.copy(alpha = 0.12f)
    val x = nodeX(ChapterRewards.lastLevelOf(chest.chapter))

    val bounce by rememberInfiniteTransition(label = "chest").animateFloat(
        initialValue = 0f,
        targetValue = if (state == 1) -6f else 0f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "chestBounce"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .drawBehind {
                // Tramo hacia el banner del siguiente capítulo
                val x0 = size.width * x
                drawLine(
                    color = lineColor,
                    start = Offset(x0, size.height / 2f),
                    end = Offset(x0, 0f),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (!chest.ready) PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 12.dp.toPx())) else null
                )
            }
    ) {
        val size = 64.dp
        Box(
            modifier = Modifier
                .offset(x = maxWidth * x - size / 2, y = (ROW_HEIGHT - size) / 2 + bounce.dp)
                .size(size)
                .then(if (state == 1) Modifier.shadow(14.dp, RoundedCornerShape(20.dp), spotColor = Gold) else Modifier.shadow(4.dp, RoundedCornerShape(20.dp)))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when (state) {
                        1 -> Brush.linearGradient(listOf(Color(0xFFF2CC8F), Gold))
                        2 -> Brush.linearGradient(listOf(Color(0xFFE4EEE8), Color(0xFFE4EEE8)))
                        else -> Brush.linearGradient(listOf(Color(0xFFEDEBE6), Color(0xFFEDEBE6)))
                    }
                )
                .border(2.dp, if (state == 1) Color.White else Navy.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state == 2) Icons.Rounded.CheckCircle else Icons.Rounded.Inventory2,
                contentDescription = "Cofre del capítulo ${chest.chapter + 1}",
                tint = when (state) {
                    1 -> Color.White
                    2 -> Sage
                    else -> Navy.copy(alpha = 0.28f)
                },
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
