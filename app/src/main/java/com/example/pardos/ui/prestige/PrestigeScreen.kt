package com.korkoor.pardos.ui.prestige

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.PrestigeManager
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.domain.collection.Rarity
import com.korkoor.pardos.domain.model.UserProfile
import com.korkoor.pardos.domain.prestige.Metric
import com.korkoor.pardos.domain.prestige.Milestone
import com.korkoor.pardos.domain.prestige.MilestoneGroup
import com.korkoor.pardos.domain.prestige.Platinum
import com.korkoor.pardos.domain.prestige.PrestigeMilestones
import com.korkoor.pardos.domain.prestige.PrestigeRank
import com.korkoor.pardos.domain.prestige.PrestigeScore
import com.korkoor.pardos.domain.prestige.PrestigeStats
import com.korkoor.pardos.domain.prestige.ProfileTitle
import com.korkoor.pardos.domain.prestige.ProfileTitles
import com.korkoor.pardos.domain.prestige.RankEntry
import com.korkoor.pardos.domain.prestige.Rivals
import com.korkoor.pardos.domain.prestige.TitleUnlock
import com.korkoor.pardos.domain.prestige.TrophyTier
import com.korkoor.pardos.ui.design.Gold
import com.korkoor.pardos.ui.design.Navy
import com.korkoor.pardos.ui.design.PardosCard
import com.korkoor.pardos.ui.design.PardosTopBar
import com.korkoor.pardos.ui.design.SectionLabel
import com.korkoor.pardos.ui.design.Sage
import com.korkoor.pardos.ui.design.Terracotta
import com.korkoor.pardos.ui.design.Violet
import com.korkoor.pardos.ui.design.pardosBackdrop

private val Night1 = Color(0xFF241547)
private val Night2 = Color(0xFF3A2570)
private val Night3 = Color(0xFF1E2A5A)

fun Metric.icon(): ImageVector = when (this) {
    Metric.PIECES -> Icons.Rounded.Collections
    Metric.EPIC_PLUS -> Icons.Rounded.AutoAwesome
    Metric.FOIL -> Icons.Rounded.Diamond
    Metric.SERIES -> Icons.Rounded.ViewModule
    Metric.ALBUM -> Icons.Rounded.MenuBook
    Metric.CAMPAIGN_LEVEL -> Icons.Rounded.Map
    Metric.CAMPAIGN_STARS -> Icons.Rounded.Star
    Metric.TOWER -> Icons.Rounded.Layers
    Metric.WIN_STREAK -> Icons.Rounded.Bolt
    Metric.DAY_STREAK -> Icons.Rounded.LocalFireDepartment
    Metric.BOSSES -> Icons.Rounded.Shield
    Metric.KINDS -> Icons.Rounded.Category
    Metric.DAILY -> Icons.Rounded.Today
    Metric.FRIENDS -> Icons.Rounded.Groups
    Metric.FLOW -> Icons.Rounded.Whatshot
    Metric.TROPHIES -> Icons.Rounded.EmojiEvents
}

fun Rarity.color(): Color = when (this) {
    Rarity.COMMON -> Color(0xFF8D96A8)
    Rarity.RARE -> Color(0xFF4F9BD9)
    Rarity.EPIC -> Color(0xFF9B6BE0)
    Rarity.LEGENDARY -> Color(0xFFF2A81E)
}

fun com.korkoor.pardos.domain.prestige.ProfileTitle.tint(): Color = rarity.color()

fun Rarity.label(): String = when (this) {
    Rarity.COMMON -> "Común"; Rarity.RARE -> "Raro"; Rarity.EPIC -> "Épico"; Rarity.LEGENDARY -> "Legendario"
}

/**
 * Prestigio: tu rango, el Platino, los hitos que faltan y los títulos que puedes lucir. Todo enseña lo cerca que estás de
 * lo siguiente (la barra siempre está a la vista) y de cómo vas frente a tus amigos.
 */
@Composable
fun PrestigeScreen(onBack: () -> Unit, onTrophies: () -> Unit, onAlbum: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { PrestigeManager(context) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { manager.refresh(); tick++ }
    val stats = remember(tick) { manager.stats() }
    val done = remember(tick) { manager.doneMilestones }
    val score = PrestigeScore.score(stats)
    val rank = PrestigeRank.forScore(score)
    val title = remember(tick) { manager.equippedTitle() }

    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val profile = remember(tick) { ProfileManager(context).getProfile() }
    LaunchedEffect(Unit) { ProfileManager(context).getFriendsProfiles { friends = it } }

    var tab by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().pardosBackdrop()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PardosTopBar(eyebrow = "Tu leyenda", title = "Prestigio", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { RankHero(stats, score, rank, title) }
                item { PlatinumCard(stats, done.size, onTrophies) }
                item { NextUpCard(stats, done) }
                item { RivalCard(profile, score, friends) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TabChip("Hitos", tab == 0, Modifier.weight(1f)) { tab = 0 }
                        TabChip("Títulos", tab == 1, Modifier.weight(1f)) { tab = 1 }
                    }
                }
                if (tab == 0) {
                    for (g in MilestoneGroup.entries) {
                        val list = PrestigeMilestones.inGroup(g)
                        if (list.isEmpty()) continue
                        val doneCount = list.count { it.id in done }
                        item { SectionLabel("${g.label} · $doneCount/${list.size}") }
                        val ordered = list.sortedWith(compareBy<Milestone> { it.id in done }.thenByDescending { it.fraction(stats) })
                        items(ordered.size) { i -> MilestoneRow(ordered[i], stats, ordered[i].id in done) }
                    }
                } else {
                    item { TitlesTab(manager, stats, rank, done) { tick++ } }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinkButton("Ver trofeos", Icons.Rounded.EmojiEvents, Modifier.weight(1f), onTrophies)
                        LinkButton("Ver álbum", Icons.Rounded.Collections, Modifier.weight(1f), onAlbum)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- héroe del rango

@Composable
private fun RankHero(stats: PrestigeStats, score: Int, rank: PrestigeRank, title: ProfileTitle) {
    val (light, mid, _) = rank.palette()
    val next = rank.next
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Night1, Night2, Night3)))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadge(rank, 104.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("RANGO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = light.copy(alpha = 0.7f), letterSpacing = 3.sp)
                    Text(rank.title, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(
                        title.name, fontSize = 12.sp, fontWeight = FontWeight.Black, color = title.rarity.color().lighten(),
                        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                    Text("$score puntos de prestigio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            // barra al siguiente rango
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))) {
                Box(Modifier.fillMaxWidth(PrestigeRank.progress(score).coerceAtLeast(0.03f)).height(10.dp).background(Brush.horizontalGradient(listOf(mid, light)), CircleShape))
            }
            Spacer(Modifier.height(6.dp))
            if (next != null) {
                Text(
                    "Faltan ${PrestigeRank.pointsToNext(score)} puntos para ${next.title}",
                    fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White
                )
                Text(
                    "Premio: ${next.rewardCoins} monedas · ${next.rewardGems} gemas" + if (next.rewardChest != null) " · un cofre" else "",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Gold
                )
            } else {
                Text("¡Rango máximo! Eres una leyenda de ParDos", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Gold)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (tier in TrophyTier.entries) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TrophyMedal(tier, 44.dp)
                        Text("${stats.trophiesByTier[tier] ?: 0}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(tier.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

private fun Color.lighten(): Color = Color(
    red = (red + (1f - red) * 0.45f), green = (green + (1f - green) * 0.45f), blue = (blue + (1f - blue) * 0.45f), alpha = alpha
)

// ---------------------------------------------------------------- platino

@Composable
private fun PlatinumCard(stats: PrestigeStats, milestonesDone: Int, onTrophies: () -> Unit) {
    val (have, total) = Platinum.progress(stats.trophiesUnlocked, stats.trophiesTotal, milestonesDone, PrestigeMilestones.all.size)
    val fraction = if (total == 0) 0f else have.toFloat() / total
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(if (stats.platinum) Brush.linearGradient(listOf(Color(0xFFEAF1FF), Color(0xFFBBCBF2), Color(0xFFF4F7FF))) else Brush.linearGradient(listOf(Color.White, Color(0xFFF1F3FA))))
            .clickable(onClick = onTrophies)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatinumTrophy(fraction, 96.dp, earned = stats.platinum)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("TROFEO PLATINO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Violet, letterSpacing = 3.sp)
                Text(if (stats.platinum) "¡LO CONSEGUISTE!" else "$have / $total", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Navy)
                Text(
                    "Logros ${stats.trophiesUnlocked}/${stats.trophiesTotal}  ·  Hitos $milestonesDone/${PrestigeMilestones.all.size}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Navy.copy(alpha = 0.1f))) {
                    Box(Modifier.fillMaxWidth(fraction.coerceAtLeast(0.02f)).height(8.dp).background(Brush.horizontalGradient(listOf(Color(0xFF8E9AC4), Color(0xFF6A4CE0))), CircleShape))
                }
                Text(
                    if (stats.platinum) "Solo lo tienen los que completan TODO el juego" else "Todos los logros y todos los hitos. +${Platinum.REWARD_COINS} monedas, ${Platinum.REWARD_GEMS} gemas y 2 cofres épicos",
                    fontSize = 11.sp, color = Navy.copy(alpha = 0.55f), modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- lo siguiente

@Composable
private fun NextUpCard(stats: PrestigeStats, done: Set<String>) {
    val next = remember(stats, done) { PrestigeMilestones.nextUp(stats, done, 3) }
    if (next.isEmpty()) return
    PardosCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("LO SIGUIENTE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Terracotta, letterSpacing = 3.sp)
            for (m in next) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MetricTile(m.metric, Terracotta)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Bar(m.fraction(stats), Terracotta)
                        Text("${m.progress(stats)} / ${m.target}  ·  ${rewardText(m)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.55f), modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- rivales

@Composable
private fun RivalCard(me: UserProfile, myScore: Int, friends: List<UserProfile>) {
    if (friends.isEmpty()) return
    val (sorted, r) = remember(friends, myScore) {
        Rivals.rank(RankEntry("yo", me.name, myScore, true), friends.map { RankEntry(it.uid, it.name, it.prestige) })
    }
    PardosCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("TUS AMIGOS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Violet, letterSpacing = 3.sp)
            val headline = when {
                r.position == 1 -> "¡Eres el número 1 de tus amigos!"
                else -> "Eres el #${r.position} de tus amigos"
            }
            Text(headline, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
            r.above?.let { Text("${it.name} te saca ${r.gapToAbove} puntos: ¡alcánzale!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Terracotta) }
            r.below?.let {
                if (r.gapToBelow <= 150) Text("${it.name} te sigue a solo ${r.gapToBelow} puntos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(2.dp))
            sorted.take(5).forEachIndexed { i, e ->
                val fp = friends.firstOrNull { it.uid == e.uid }
                val entryRank = PrestigeRank.forScore(e.score)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (e.isMe) Violet.copy(alpha = 0.1f) else Color.Transparent).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (i == 0) Gold else Navy.copy(alpha = 0.5f), modifier = Modifier.width(22.dp))
                    RankDot(entryRank, 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (e.isMe) "${e.name} (tú)" else e.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val t = if (e.isMe) null else fp?.titleId?.let { ProfileTitles.byId(it) }
                        Text(t?.name ?: entryRank.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = (t?.rarity ?: Rarity.COMMON).color())
                    }
                    if (fp?.platinum == true) Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFF8E9AC4), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${e.score}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- hitos

@Composable
private fun MilestoneRow(m: Milestone, stats: PrestigeStats, isDone: Boolean) {
    val c = if (isDone) Gold else Sage
    PardosCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            MetricTile(m.metric, c)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(m.description, fontSize = 11.sp, color = Navy.copy(alpha = 0.55f), maxLines = 2)
                if (!isDone) {
                    Spacer(Modifier.height(5.dp))
                    Bar(m.fraction(stats), Sage)
                    Text("${m.progress(stats)} / ${m.target}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.5f), modifier = Modifier.padding(top = 2.dp))
                }
                Text(rewardText(m), fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isDone) Navy.copy(alpha = 0.35f) else Gold.copy(alpha = 0.95f), modifier = Modifier.padding(top = 2.dp))
            }
            if (isDone) {
                Box(Modifier.size(28.dp).background(Gold, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun rewardText(m: Milestone): String = buildList {
    add("+${m.coins} monedas")
    if (m.gems > 0) add("+${m.gems} gemas")
    if (m.chest != null) add("cofre")
}.joinToString(" · ")

@Composable
private fun MetricTile(metric: Metric, color: Color) {
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.14f)))), contentAlignment = Alignment.Center) {
        Icon(metric.icon(), null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceAtLeast(0.02f)).height(7.dp).background(color, CircleShape))
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (selected) Navy else Color.White).clickable(onClick = onClick).padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (selected) Color.White else Navy.copy(alpha = 0.6f), letterSpacing = 2.sp)
    }
}

@Composable
private fun LinkButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).clickable(onClick = onClick).padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Violet, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Navy)
    }
}

// ---------------------------------------------------------------- títulos

@Composable
private fun TitlesTab(manager: PrestigeManager, stats: PrestigeStats, rank: PrestigeRank, done: Set<String>, onChanged: () -> Unit) {
    val equipped = manager.equippedTitle()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "El título que luzcas aparece bajo tu nombre en tu perfil y en el ranking de tus amigos.",
            fontSize = 12.sp, color = Navy.copy(alpha = 0.6f), textAlign = TextAlign.Start
        )
        val ordered = ProfileTitles.all.sortedWith(compareByDescending<ProfileTitle> { manager.isOwned(it, stats) }.thenByDescending { it.rarity.ordinal })
        for (t in ordered) {
            val owned = manager.isOwned(t, stats)
            val isEquipped = equipped.id == t.id
            PardosCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(t.rarity.color(), CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (owned) t.rarity.color() else Navy.copy(alpha = 0.4f))
                        Text(
                            if (owned) t.rarity.label() else ProfileTitles.howTo(t),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.5f), maxLines = 2
                        )
                    }
                    when {
                        isEquipped -> Text("LUCIENDO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Gold, letterSpacing = 1.sp)
                        owned -> ActionPill("USAR", Sage) { if (manager.equip(t)) onChanged() }
                        t.unlock is TitleUnlock.Gems -> ActionPill("${(t.unlock as TitleUnlock.Gems).price} gemas", Violet) { if (manager.buy(t)) onChanged() }
                        else -> Icon(Icons.Rounded.Lock, null, tint = Navy.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
