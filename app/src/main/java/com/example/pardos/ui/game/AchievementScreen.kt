package com.korkoor.pardos.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.domain.achievements.Achievement
import com.korkoor.pardos.domain.achievements.gameAchievements
import com.korkoor.pardos.ui.design.*
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.theme.GameTheme

/** Categorías para ordenar la colección (se deducen del id para no tocar la lista de logros). */
private enum class AchCategory(val label: String) {
    ALL("Todos"),
    PROGRESS("Progreso"),
    TILES("Fichas"),
    SPEED("Velocidad"),
    ENDURANCE("Resistencia"),
    SCORE("Puntos"),
    COMBO("Combos"),
    SPECIAL("Especiales")
}

private fun categoryOf(id: String): AchCategory = when {
    id == "first_win" || id == "getting_warmed_up" || id.startsWith("level_") ||
        id.startsWith("classic_") || id.startsWith("challenge_") || id.startsWith("zen_") -> AchCategory.PROGRESS
    id.startsWith("tile_") || id in setOf("lucky_seven", "thirteen", "double_double", "quad_squad", "power_of_two", "fibonacci") -> AchCategory.TILES
    id == "speedrun" || id.startsWith("speed_") || id in setOf("time_60", "time_45", "time_30", "time_15", "rapid_fire", "efficiency_king") -> AchCategory.SPEED
    id == "century" || id.startsWith("moves_") || id.endsWith("min") -> AchCategory.ENDURANCE
    id.startsWith("score_") -> AchCategory.SCORE
    id.startsWith("combo_") -> AchCategory.COMBO
    else -> AchCategory.SPECIAL
}

private enum class StatusFilter(val label: String) { ALL("Todos"), DONE("Logrados"), PENDING("Pendientes") }

@Composable
fun AchievementsScreen(
    unlockedIds: Set<String>,
    currentTheme: GameTheme,
    onBack: () -> Unit
) {
    val all = gameAchievements.all
    val total = all.size
    val context = androidx.compose.ui.platform.LocalContext.current
    val rewards = remember { com.korkoor.pardos.data.local.RewardsManager(context) }
    // Primera vez con el sistema de premios: se pagan los logros que ya tenías
    var backfill by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(unlockedIds) {
        val paid = rewards.backfillAchievements(unlockedIds)
        if (paid.first > 0 || paid.second > 0) backfill = paid
    }
    val unlocked = all.count { it.id in unlockedIds }

    var category by remember { mutableStateOf(AchCategory.ALL) }
    var status by remember { mutableStateOf(StatusFilter.ALL) }

    val filtered = remember(unlockedIds, category, status) {
        all.filter { category == AchCategory.ALL || categoryOf(it.id) == category }
            .filter {
                when (status) {
                    StatusFilter.ALL -> true
                    StatusFilter.DONE -> it.id in unlockedIds
                    StatusFilter.PENDING -> it.id !in unlockedIds
                }
            }
            // Primero los logrados, así el jugador ve lo que ya tiene
            .sortedByDescending { it.id in unlockedIds }
    }

    Box(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        PicnicBackgroundOptimized(color = currentTheme.accentColor.copy(alpha = 0.04f))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(
                eyebrow = "Colección",
                title = stringResource(R.string.menu_achievements),
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SummaryCard(unlocked, total, all, unlockedIds) }

                backfill?.let { (coins, gems) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.Medium))
                                .background(Gold.copy(alpha = 0.16f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "¡Tus logros ahora dan premio! Recibiste +$coins monedas" + if (gems > 0) " y +$gems gemas" else "",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy
                            )
                        }
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(AchCategory.entries.toList()) { c ->
                            val inCat = all.filter { c == AchCategory.ALL || categoryOf(it.id) == c }
                            val done = inCat.count { it.id in unlockedIds }
                            CategoryChip(
                                label = c.label,
                                count = "$done/${inCat.size}",
                                selected = c == category,
                                onClick = { category = c }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Navy.copy(alpha = 0.06f))
                            .padding(4.dp)
                    ) {
                        StatusFilter.entries.forEach { f ->
                            val sel = f == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) Color.White else Color.Transparent)
                                    .clickable { status = f }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    f.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (sel) Navy else Navy.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (status == StatusFilter.DONE) "Aún no tienes logros aquí. ¡A jugar!" else "¡Lo tienes todo en esta categoría!",
                                fontSize = 13.sp, color = InkSecondary
                            )
                        }
                    }
                }

                items(filtered, key = { it.id }) { ach ->
                    AchievementRow(ach, unlocked = ach.id in unlockedIds, reward = rewards.rewardFor(ach.id), rarity = rewards.rarityOf(ach.id))
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryCard(unlocked: Int, total: Int, all: List<Achievement>, unlockedIds: Set<String>) {
    val progress = if (total == 0) 0f else unlocked.toFloat() / total
    val animated by animateFloatAsState(progress, tween(900, easing = FastOutSlowInEasing), label = "ring")

    PardosCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.XLarge), elevation = 8.dp) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Anillo de progreso
            Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 11.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Navy.copy(alpha = 0.08f), startAngle = -90f, sweepAngle = 360f,
                        useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Sage, Gold, Sage)),
                        startAngle = -90f, sweepAngle = 360f * animated,
                        useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text("${(progress * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("COLECCIÓN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = InkTertiary, letterSpacing = 3.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$unlocked", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Navy)
                    Text(" / $total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkTertiary, modifier = Modifier.padding(bottom = 5.dp))
                }
                val remaining = total - unlocked
                Text(
                    if (remaining == 0) "¡Colección completa!" else "Te faltan $remaining para completarla",
                    fontSize = 12.sp, color = InkSecondary
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, count: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Navy else Color.White, label = "chipBg")
    Row(
        modifier = Modifier
            .shadow(if (selected) 0.dp else 3.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (selected) Color.White else Navy)
        Spacer(Modifier.width(6.dp))
        Text(count, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White.copy(alpha = 0.7f) else InkTertiary)
    }
}

@Composable
private fun AchievementRow(
    achievement: Achievement,
    unlocked: Boolean,
    reward: com.korkoor.pardos.domain.economy.Economy.AchievementReward,
    rarity: com.korkoor.pardos.domain.collection.Rarity
) {
    val shape = RoundedCornerShape(Radius.Large)
    val color = achievement.color

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (unlocked) 6.dp else 0.dp, shape, spotColor = color)
            .clip(shape)
            .background(if (unlocked) Color.White else Color.White.copy(alpha = 0.55f))
            .then(if (unlocked) Modifier.border(1.5.dp, color.copy(alpha = 0.35f), shape) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Medalla
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (unlocked) Brush.linearGradient(listOf(color, color.darker(0.78f)))
                    else Brush.linearGradient(listOf(Navy.copy(alpha = 0.07f), Navy.copy(alpha = 0.07f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (unlocked) {
                // brillo superior
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.30f), 0.5f to Color.Transparent)
                    )
                )
                Icon(achievement.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            } else {
                Icon(achievement.icon, contentDescription = null, tint = Navy.copy(alpha = 0.18f), modifier = Modifier.size(26.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = Navy.copy(alpha = 0.35f), modifier = Modifier.size(12.dp))
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(achievement.titleResId),
                fontSize = 15.sp, fontWeight = FontWeight.Black,
                color = if (unlocked) Navy else Navy.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(achievement.descriptionResId),
                fontSize = 12.sp, lineHeight = 16.sp,
                color = if (unlocked) InkSecondary else InkTertiary
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val rarityColor = when (rarity) {
                    com.korkoor.pardos.domain.collection.Rarity.COMMON -> Navy.copy(alpha = 0.35f)
                    com.korkoor.pardos.domain.collection.Rarity.RARE -> GemBlue
                    com.korkoor.pardos.domain.collection.Rarity.EPIC -> Violet
                    com.korkoor.pardos.domain.collection.Rarity.LEGENDARY -> Gold
                }
                Text(
                    "+${reward.coins}", fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = if (unlocked) Gold else Gold.copy(alpha = 0.6f)
                )
                if (reward.gems > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("+${reward.gems}◆", fontSize = 11.sp, fontWeight = FontWeight.Black, color = GemBlue)
                }
                if (rarity.ordinal >= com.korkoor.pardos.domain.collection.Rarity.EPIC.ordinal) {
                    Spacer(Modifier.width(8.dp))
                    Text("+COFRE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = rarityColor, letterSpacing = 1.sp)
                }
            }
        }

        if (unlocked) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Sage, modifier = Modifier.size(24.dp))
        }
    }
}
