package com.korkoor.pardos.ui.rewards

import com.korkoor.pardos.ui.design.CozyText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.data.local.RetentionManager
import com.korkoor.pardos.domain.retention.League
import com.korkoor.pardos.domain.retention.LeagueOutcome
import com.korkoor.pardos.domain.retention.LeagueResult
import com.korkoor.pardos.domain.retention.Leagues
import com.korkoor.pardos.domain.social.WeekCalendar
import com.korkoor.pardos.ui.collection.chestColor
import com.korkoor.pardos.ui.collection.chestName
import com.korkoor.pardos.ui.design.*

fun leagueColor(l: League): Color = when (l) {
    League.BRONZE -> Color(0xFFB87D4B)
    League.SILVER -> Color(0xFF8E9AA8)
    League.GOLD -> Gold
    League.SAPPHIRE -> GemBlue
    League.RUBY -> Color(0xFFC94C5F)
    League.DIAMOND -> Violet
}

private fun leagueIcon(l: League): ImageVector = if (l.isTop) Icons.Rounded.Diamond else Icons.Rounded.WorkspacePremium

/** Insignia redonda de una liga (con su número dentro del escudo). */
@Composable
fun LeagueBadge(league: League, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val c = leagueColor(league)
    Box(
        modifier = Modifier.size(size).background(c.gradient(), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(leagueIcon(league), null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

/** Tarjeta del menú: tu liga, avance de la semana y qué te falta para subir. */
@Composable
fun LeagueCard(retention: RetentionManager, modifier: Modifier = Modifier) {
    val tick by retention.tick.collectAsState()
    val league = remember(tick) { retention.league }
    val stars = remember(tick) { retention.leagueWeekStars() }
    val daysLeft = remember { WeekCalendar.daysLeft(LocalDay.today()) }
    var info by remember { mutableStateOf(false) }

    val color = leagueColor(league)
    val toGo = Leagues.starsToPromote(league, stars)
    val risk = Leagues.atRisk(league, stars)
    val next = league.next()

    JellySurface(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        onClick = { info = true }
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LeagueBadge(league)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("LIGA ${league.displayName.uppercase()}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.5.sp)
                    Text(
                        "Cierra en $daysLeft ${if (daysLeft == 1) "día" else "días"}",
                        fontSize = 10.sp, color = InkSecondary
                    )
                }
                Icon(Icons.Rounded.Info, null, tint = InkTertiary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(Navy.copy(alpha = 0.08f))) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(Leagues.progress(league, stars).coerceAtLeast(0.02f))
                            .background(if (toGo == 0) Sage else color, CircleShape)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("$stars/${league.promoteStars}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy)
                Icon(Icons.Rounded.Star, null, tint = Gold, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.height(8.dp))
            CozyText(
                when {
                    toGo == 0 && league.isTop -> "¡Defendiste la cima esta semana!"
                    toGo == 0 -> "¡Ascenso asegurado a ${next?.displayName}! Se cobra al cerrar la semana."
                    risk -> "Cuidado: con menos de ${league.keepStars} ★ bajarías a ${league.previous()?.displayName}."
                    else -> "Te faltan $toGo ★ para subir a ${next?.displayName ?: "la cima"}."
                },
                fontSize = 12.sp, color = if (risk) Terracotta else InkSecondary, fontWeight = if (risk) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(Modifier.height(12.dp))
            LeagueLadder(league)
        }
    }
    if (info) LeagueInfoDialog(league) { info = false }
}

/** Los seis escalones: el actual resaltado, los superados rellenos. */
@Composable
private fun LeagueLadder(current: League) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        League.entries.forEach { l ->
            val reached = l.ordinal <= current.ordinal
            Box(
                Modifier.weight(1f).height(if (l == current) 8.dp else 5.dp).clip(CircleShape)
                    .background(if (reached) leagueColor(l) else Navy.copy(alpha = 0.08f))
            )
        }
    }
}

@Composable
private fun LeagueInfoDialog(current: League, onDismiss: () -> Unit) {
    CardDialog(onDismiss) {
        LeagueBadge(current, size = 72.dp)
        Spacer(Modifier.height(12.dp))
        Text("Ligas semanales", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(
            "Gana estrellas en los niveles. Si llegas a la meta de tu liga al cerrar la semana (domingo) subes y cobras premios; si te quedas muy corto, bajas.",
            fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp
        )
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            League.entries.forEach { l ->
                val here = l == current
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (here) leagueColor(l).copy(alpha = 0.14f) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LeagueBadge(l, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(l.displayName, fontSize = 13.sp, fontWeight = if (here) FontWeight.Black else FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f))
                    CozyText(
                        if (l.isTop) "Defender: ${l.promoteStars} ★" else "Subir: ${l.promoteStars} ★",
                        fontSize = 11.sp, color = InkSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        DialogButton("Entendido", Sage, onClick = onDismiss)
    }
}

/** Resultado de la semana pasada con su premio. Se cierra reclamando. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeagueResultDialog(result: LeagueResult, onClaim: () -> Unit) {
    val up = result.outcome == LeagueOutcome.PROMOTED
    val down = result.outcome == LeagueOutcome.DEMOTED
    CardDialog(onClaim) {
        LeagueBadge(result.to, size = 76.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            when (result.outcome) {
                LeagueOutcome.PROMOTED -> "¡Subiste a ${result.to.displayName}!"
                LeagueOutcome.TOP_HELD -> "¡Sigues en la cima!"
                LeagueOutcome.STAYED -> "Te mantienes en ${result.to.displayName}"
                LeagueOutcome.DEMOTED -> "Bajas a ${result.to.displayName}"
            },
            fontSize = 21.sp, fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center
        )
        CozyText(
            "Semana pasada: ${result.stars} ★" + if (down) " · Esta semana se puede remontar" else "",
            fontSize = 12.sp, color = InkSecondary, textAlign = TextAlign.Center
        )
        if (result.hasReward) {
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (result.coins > 0) RewardChip(Icons.Rounded.MonetizationOn, "+${result.coins}", Gold)
                if (result.gems > 0) RewardChip(Icons.Rounded.Diamond, "+${result.gems}", GemBlue)
                result.chest?.let { RewardChip(Icons.Rounded.Inventory2, chestName(it), chestColor(it)) }
            }
        }
        Spacer(Modifier.height(18.dp))
        DialogButton(if (result.hasReward) "Reclamar" else "Seguir", if (up) Sage else Terracotta, onClick = onClaim)
    }
}
