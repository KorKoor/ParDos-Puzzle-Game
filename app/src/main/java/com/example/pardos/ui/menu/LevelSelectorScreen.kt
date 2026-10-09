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
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
internal const val CHAPTER_SIZE = 20
internal val ROW_HEIGHT = 92.dp

internal fun chapterOf(levelId: Int) = (levelId - 1) / CHAPTER_SIZE
private fun chapterName(chapter: Int) = chapterTheme(chapter).name
private fun chapterColor(chapter: Int) = chapterTheme(chapter).color

internal sealed interface MapItem {
    data class Node(val level: LevelInfo) : MapItem
    data class Banner(val chapter: Int, val stars: Int, val maxStars: Int, val completed: Int) : MapItem
    /** Cofre al final de un capítulo. [ready] = último nivel superado; [claimed] = ya abierto. */
    data class Chest(val chapter: Int, val ready: Boolean, val claimed: Boolean) : MapItem
}

/** Posición horizontal (0..1) del nodo: un zigzag suave. */
internal fun nodeX(levelId: Int): Float = 0.5f + 0.27f * sin(levelId * 0.85f)

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
    val haptic = com.korkoor.pardos.ui.design.rememberGameHaptics()
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
    var showChapters by remember { mutableStateOf(false) }

    // Tus amigos aparecen en el nivel donde van
    var friends by remember { mutableStateOf<List<com.korkoor.pardos.domain.model.UserProfile>>(emptyList()) }
    LaunchedEffect(Unit) {
        try { com.korkoor.pardos.data.local.ProfileManager(context).getFriendsProfiles { friends = it } } catch (_: Exception) { }
    }
    val friendsByLevel = remember(friends) { friends.groupBy { it.currentCampaignLevel } }
    val bannerIndex = remember(items) {
        items.mapIndexedNotNull { i, it -> (it as? MapItem.Banner)?.let { b -> b.chapter to i } }.toMap()
    }

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
            // el capítulo del elemento que está en el centro de la pantalla
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val mid = info.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }
            val first = mid?.index ?: currentIndex
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

    // Si acabas de superar un nivel, el nuevo nivel actual se desbloquea con una animación
    val seenPrefs = remember { context.getSharedPreferences("pardos_map", android.content.Context.MODE_PRIVATE) }
    val previouslySeen = remember { seenPrefs.getInt("seen_current", 0) }
    val justUnlockedId = remember(currentLevel?.id) { currentLevel?.id?.takeIf { previouslySeen in 1 until it } ?: -1 }
    LaunchedEffect(currentLevel?.id) {
        delay(2500)
        seenPrefs.edit().putInt("seen_current", currentLevel?.id ?: 0).apply()
    }
    val rowPx = with(androidx.compose.ui.platform.LocalDensity.current) { ROW_HEIGHT.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Mundo vivo detrás del mapa: cielo del capítulo, sol/luna, nubes, relieve y partículas
        MapBackdrop(chapter = visibleChapter, scrollPx = { listState.firstVisibleItemIndex * rowPx + listState.firstVisibleItemScrollOffset })

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
                                        com.korkoor.pardos.data.local.RewardsManager(context).grantChapterChest(item.chapter)
                                        hint = "¡Cofre abierto! +${reward.coins} monedas, +${reward.gems} gemas y un cofre de colección"
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
                        justUnlocked = item.level.id == justUnlockedId,
                        distanceAhead = (item.level.id - (currentLevel?.id ?: 1)).coerceAtLeast(0),
                        friends = friendsByLevel[item.level.id].orEmpty(),
                        onFriendsClick = { names -> hint = names },
                        onClick = {
                            if (item.level.isLocked) {
                                com.korkoor.pardos.audio.GameAudio.play(com.korkoor.pardos.audio.Sfx.UI_LOCKED)
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

        // --- Cabecera flotante: una tarjeta translúcida para que el texto siempre se lea ---
        val night = chapterIsNight(visibleChapter)
        val headerBase by animateColorAsState(skyTopColor(visibleChapter), tween(900), label = "headerBase")
        val headerInk by animateColorAsState(if (night) Color.White else Navy, tween(600), label = "headerInk")
        val headerCard = if (night) Color(0xFF14142B).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.86f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(headerBase.copy(alpha = 0.9f), headerBase.copy(alpha = 0f))))
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(30.dp), spotColor = chapterColor(visibleChapter))
                    .clip(RoundedCornerShape(30.dp))
                    .background(headerCard)
                    .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JellySurface(onClick = onBack, shape = CircleShape, color = Color.White, shadowElevation = 4.dp, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Navy)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { showChapters = true }.padding(vertical = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CAMPAÑA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = headerInk.copy(alpha = 0.6f), letterSpacing = 3.sp)
                        Icon(Icons.Rounded.UnfoldMore, contentDescription = "Capítulos", tint = headerInk.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                    Text(
                        "Cap. ${visibleChapter + 1} · ${chapterName(visibleChapter)}",
                        fontSize = 16.sp, fontWeight = FontWeight.Black, color = headerInk, maxLines = 1
                    )
                    val chapterDone = remember(levels, visibleChapter) {
                        levels.count { chapterOf(it.id) == visibleChapter && it.starsEarned > 0 }
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth(0.8f).height(5.dp).clip(CircleShape).background(headerInk.copy(alpha = 0.14f))) {
                        Box(
                            Modifier.fillMaxHeight()
                                .fillMaxWidth((chapterDone.toFloat() / CHAPTER_SIZE).coerceIn(0f, 1f).coerceAtLeast(0.03f))
                                .background(chapterColor(visibleChapter), CircleShape)
                        )
                    }
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

        // --- Selector de capítulos ---
        if (showChapters) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showChapters = false }
            )
        }
        AnimatedVisibility(
            visible = showChapters,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            val lastChapter = chapterOf(levels.size.coerceAtLeast(1))
            val reachChapter = ((currentLevel?.id ?: 1).let { chapterOf(it) } + 1).coerceAtMost(lastChapter)
            ChapterSheet(
                chapters = (reachChapter downTo 0).toList(),
                levels = levels,
                currentChapter = chapterOf(currentLevel?.id ?: 1),
                visibleChapter = visibleChapter,
                onPick = { ch ->
                    showChapters = false
                    val idx = bannerIndex[ch]
                    if (idx != null) scope.launch { listState.animateScrollToItem((idx - 1).coerceAtLeast(0)) }
                },
                onClose = { showChapters = false }
            )
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
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Sage)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(SageLight, SageDark)))
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
private fun LevelPreviewCard(level: LevelInfo, onPlay: () -> Unit) {
    val chapterColor = chapterColor(chapterOf(level.id))
    val spec = level.spec
    val kind = level.kind
    val color = if (spec != null && kind != com.korkoor.pardos.domain.level.LevelKind.ZEN) kind.accent() else chapterColor

    JellySurface(
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
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.korkoor.pardos.ui.game.components.KindBadge(kind, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("NIVEL ${level.id}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 2.sp)
                    Text((spec?.title ?: level.difficultyName).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = color.darker(0.85f), letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Icon(
                        Icons.Rounded.Star, contentDescription = null,
                        tint = if (i < level.starsEarned) Color(0xFFFFC83D) else Navy.copy(alpha = 0.12f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (spec != null) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.korkoor.pardos.ui.game.components.BoardMiniPreview(spec, boardSize = 92.dp)
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PreviewStat("META", when {
                            spec.goal == com.korkoor.pardos.domain.level.LevelGoal.SCORE -> com.korkoor.pardos.domain.level.formatThousands(spec.goalValue) + " pts"
                            spec.goal == com.korkoor.pardos.domain.level.LevelGoal.MERGES -> "${spec.goalValue} fusiones"
                            spec.goal == com.korkoor.pardos.domain.level.LevelGoal.COMBO -> "Combo de ${spec.goalValue}" + if (spec.goalCount > 1) " ×${spec.goalCount}" else ""
                            spec.goal == com.korkoor.pardos.domain.level.LevelGoal.LADDER -> "${spec.goalValue shr (spec.goalCount - 1)}…${spec.goalValue}"
                            spec.goalCount > 1 -> "${spec.goalCount}×${spec.goalValue}"
                            else -> "${spec.goalValue}"
                        }, Alignment.Start)
                        PreviewStat("LÍMITE", when {
                            spec.moveLimit != null -> "${spec.moveLimit} mov."
                            spec.timeLimitMs != null -> com.korkoor.pardos.domain.level.formatClock(spec.timeLimitMs!!)
                            else -> "Libre"
                        }, Alignment.Start)
                        PreviewStat("TABLERO", "${spec.boardSize}×${spec.boardSize}" + if (spec.stones.isNotEmpty()) " · ${spec.stones.size} ${if (spec.stones.size == 1) "piedra" else "piedras"}" else "", Alignment.Start)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    kind.rule, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.6f), textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    com.korkoor.pardos.domain.level.LevelRules.threeStarHint(spec),
                    fontSize = 11.sp, color = Navy.copy(alpha = 0.45f), textAlign = TextAlign.Center
                )
            }

            val toChest = com.korkoor.pardos.domain.rewards.ChapterRewards.lastLevelOf(chapterOf(level.id)) - level.id
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(chapterColor.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = chapterColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (toChest <= 0) "Este nivel abre el cofre del capítulo" else "Cofre del capítulo en $toChest ${if (toChest == 1) "nivel" else "niveles"}",
                    fontSize = 11.sp, fontWeight = FontWeight.Black, color = chapterColor.darker(0.8f)
                )
            }

            if (level.bestMoves > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tu mejor: ${level.bestMoves} mov.",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = if (level.starsEarned > 0) "Jugar de nuevo" else "Jugar",
                onClick = onPlay,
                icon = Icons.Rounded.PlayArrow,
                height = 60.dp,
                color = color.actionTone()
            )
        }
    }
}

@Composable
private fun PreviewStat(label: String, value: String, align: Alignment.Horizontal = Alignment.CenterHorizontally) {
    Column(horizontalAlignment = align) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.4f), letterSpacing = 1.5.sp)
    }
}


// =====================================================================================
//  SELECTOR DE CAPÍTULOS: saltar a cualquier capítulo ya alcanzado
// =====================================================================================

@Composable
private fun ChapterSheet(
    chapters: List<Int>, levels: List<LevelInfo>, currentChapter: Int, visibleChapter: Int,
    onPick: (Int) -> Unit, onClose: () -> Unit
) {
    JellySurface(
        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = Color(0xFFFFFBF5), shadowElevation = 20.dp
    ) {
        Column(Modifier.navigationBarsPadding().padding(top = 14.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).background(Navy.copy(alpha = 0.12f), CircleShape))
            Spacer(Modifier.height(12.dp))
            Text("CAPÍTULOS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.5f), letterSpacing = 3.sp, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(chapters, key = { _, ch -> ch }) { _, ch ->
                    val theme = chapterTheme(ch)
                    val inChapter = levels.filter { chapterOf(it.id) == ch }
                    val done = inChapter.count { it.starsEarned > 0 }
                    val stars = inChapter.sumOf { it.starsEarned }
                    val locked = inChapter.isNotEmpty() && inChapter.all { it.isLocked }
                    val isHere = ch == visibleChapter
                    Row(
                        modifier = Modifier.fillMaxWidth().alpha(if (locked) 0.5f else 1f).clip(RoundedCornerShape(20.dp))
                            .background(if (isHere) theme.color.copy(alpha = 0.16f) else Color.White)
                            .border(if (isHere) 2.dp else 1.dp, if (isHere) theme.color else Navy.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
                            .clickable(enabled = !locked) { onPick(ch) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(44.dp).background(Brush.verticalGradient(listOf(lerpColor(theme.color), theme.color)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("${ch + 1}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(theme.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1)
                                if (ch == currentChapter) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("AQUÍ", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp,
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(theme.color).padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth((done.toFloat() / CHAPTER_SIZE).coerceIn(0f, 1f).coerceAtLeast(0.02f)).background(theme.color, CircleShape))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("$stars/${CHAPTER_SIZE * 3}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy)
                            }
                            Text("$done/$CHAPTER_SIZE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.5f))
                        }
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}

private fun lerpColor(c: Color): Color = androidx.compose.ui.graphics.lerp(c, Color.White, 0.3f)
