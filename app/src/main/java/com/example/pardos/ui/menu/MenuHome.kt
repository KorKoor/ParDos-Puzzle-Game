package com.korkoor.pardos.ui.menu

import com.korkoor.pardos.ui.profile.AvatarImage

import com.korkoor.pardos.ui.design.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import com.korkoor.pardos.ui.design.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.korkoor.pardos.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.model.UserProfile
import com.korkoor.pardos.ui.profile.getAvatarResource

// Paleta de identidad (misma que el resto del juego)

/** Tarjeta de jugador: avatar, nivel, barra de XP y racha. */
@Composable
fun PlayerHeader(profile: UserProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val xpProgress = (profile.currentXp.toFloat() / profile.xpToNextLevel.coerceAtLeast(1)).coerceIn(0f, 1f)
    JellyCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Box {
        // Tu banner asoma por la derecha; a la izquierda un velo claro mantiene el texto legible
        com.korkoor.pardos.ui.profile.ProfileBanner(profile.bannerId, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.94f), Color.White.copy(alpha = 0.80f), Color.White.copy(alpha = 0.22f)))))
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.korkoor.pardos.ui.profile.AvatarFramed(profile.avatarId, modifier = Modifier.size(56.dp), ring = 3.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Navy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NV ${profile.playerLevel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Sage,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(Navy.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(xpProgress.coerceAtLeast(0.04f))
                                .background(Brush.horizontalGradient(listOf(Sage, Sand)), CircleShape)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            StreakChip(profile.currentStreak)
        }
        }
    }
}

@Composable
fun StreakChip(days: Int) {
    val active = days > 0
    val pulse by rememberInfiniteTransition(label = "streak").animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "streakPulse"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Terracotta.copy(alpha = 0.12f) else Navy.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (active) Terracotta else Navy.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp).scale(pulse)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$days",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (active) Terracotta else Navy.copy(alpha = 0.4f)
        )
    }
}

/** Ficha pequeña decorativa del botón principal: flota y se balancea como si el tablero estuviera vivo. */
@Composable
private fun FloatingTile(value: Int, size: androidx.compose.ui.unit.Dp, tilt: Float, phase: Int, modifier: Modifier = Modifier) {
    val bob by rememberInfiniteTransition(label = "tileBob$value").animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1700 + phase * 260, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob"
    )
    val face = when (value) { 2 -> Color(0xFFFFF3E2); 4 -> Color(0xFFFBDDB0); else -> Color(0xFFF2B27A) }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { translationY = bob.dp.toPx(); rotationZ = tilt + bob * 0.8f }
            .background(Color.Black.copy(alpha = 0.10f), RoundedCornerShape(size * 0.26f))
            .padding(bottom = 3.dp)
            .background(Brush.verticalGradient(listOf(face.lighten(0.35f), face)), RoundedCornerShape(size * 0.26f)),
        contentAlignment = Alignment.Center
    ) {
        Text("$value", fontSize = (size.value * 0.46f).sp, fontWeight = FontWeight.Black, color = Color(0xFF7A4B2A))
    }
}

/** Botón principal: continúa la campaña desde el nivel actual. Pieza de juguete grande con fichas flotando. */
@Composable
fun HeroPlayCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val base = Sage
    val halloween = Season.halloween
    JellyCard(
        modifier = modifier.fillMaxWidth().height(124.dp),
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        fill = base,
        lip = if (halloween) SageDark.darker(0.62f) else Color(0xFF3F6B57),
        lipHeight = 7.dp,
        brush = Brush.verticalGradient(listOf(SageLight, Sage, SageDark))
    ) {
        // brillo de plástico
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 6.dp, start = 26.dp, end = 26.dp)
                .fillMaxWidth().height(14.dp).background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(50))
        )
        // fichas flotantes (en Noche de brujas: calabaza, fantasma y murciélago)
        if (halloween) {
            FloatingSprite(com.korkoor.pardos.R.drawable.ico_pumpkin, 50.dp, 8f, 2, Modifier.align(Alignment.TopEnd).offset(x = (-16).dp, y = 14.dp))
            FloatingSprite(com.korkoor.pardos.R.drawable.ico_ghost, 40.dp, -10f, 0, Modifier.align(Alignment.CenterEnd).offset(x = (-70).dp, y = 18.dp))
            FloatingSprite(com.korkoor.pardos.R.drawable.ico_bat, 36.dp, 10f, 1, Modifier.align(Alignment.BottomEnd).offset(x = (-24).dp, y = (-10).dp))
        } else {
            FloatingTile(8, 40.dp, 10f, 2, Modifier.align(Alignment.TopEnd).offset(x = (-22).dp, y = 22.dp))
            FloatingTile(2, 34.dp, -12f, 0, Modifier.align(Alignment.CenterEnd).offset(x = (-78).dp, y = 12.dp))
            FloatingTile(4, 30.dp, 8f, 1, Modifier.align(Alignment.BottomEnd).offset(x = (-26).dp, y = (-18).dp))
        }
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(58.dp).breathing(0.06f, 1300)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.16f))), CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title.uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 3.sp
                )
                Text(
                    text = if (halloween) "$subtitle  ·  ¡BUU!" else subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/** Acceso rápido (Reto diario / Personalizar). */
@Composable
fun QuickActionCard(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false
) {
    JellyCard(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(24.dp),
        fill = if (dark) Navy else Color.White
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (dark) Color.White.copy(alpha = 0.12f) else color.copy(alpha = 0.14f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (dark) Sand else color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (dark) Color.White else Navy,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

data class DockItem(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

/** Barra de navegación flotante inferior. */
@Composable
fun BottomDock(items: List<DockItem>, modifier: Modifier = Modifier) {
    JellyCard(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = item.onClick)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).background(item.color.copy(alpha = 0.13f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = item.color, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.label.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Navy.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}


/** Banner del evento activo con cuenta atrás de días. */
@Composable
fun EventBanner(
    event: com.korkoor.pardos.domain.events.GameEvent,
    today: Int,
    modifier: Modifier = Modifier,
    /** Si el evento trae skin: el progreso ("1/3") y qué hacer al tocar. */
    skinProgress: String? = null,
    onClick: (() -> Unit)? = null
) {
    val (title, desc, icon, color) = when (event.type) {
        com.korkoor.pardos.domain.events.EventType.WEEKEND_GOLD ->
            EventStyle(R.string.event_weekend_gold_title, R.string.event_weekend_gold_desc, Icons.Rounded.MonetizationOn, Gold)
        com.korkoor.pardos.domain.events.EventType.XP_WEDNESDAY ->
            EventStyle(R.string.event_xp_wednesday_title, R.string.event_xp_wednesday_desc, Icons.Rounded.AutoAwesome, GemBlue)
        com.korkoor.pardos.domain.events.EventType.FESTIVAL_WEEK ->
            EventStyle(R.string.event_festival_week_title, R.string.event_festival_week_desc, Icons.Rounded.Celebration, Terracotta)
        com.korkoor.pardos.domain.events.EventType.HALLOWEEN ->
            EventStyle(R.string.event_halloween_title, R.string.event_halloween_desc, Icons.Rounded.Nightlight, Color(0xFFE0782F))
        com.korkoor.pardos.domain.events.EventType.DAY_OF_THE_DEAD ->
            EventStyle(R.string.event_dead_title, R.string.event_dead_desc, Icons.Rounded.LocalFlorist, Color(0xFFE08A2E))
        com.korkoor.pardos.domain.events.EventType.CHRISTMAS ->
            EventStyle(R.string.event_christmas_title, R.string.event_christmas_desc, Icons.Rounded.CardGiftcard, Color(0xFFC94C5F))
        com.korkoor.pardos.domain.events.EventType.NEW_YEAR ->
            EventStyle(R.string.event_new_year_title, R.string.event_new_year_desc, Icons.Rounded.Celebration, Gold)
        com.korkoor.pardos.domain.events.EventType.VALENTINE ->
            EventStyle(R.string.event_valentine_title, R.string.event_valentine_desc, Icons.Rounded.Favorite, Color(0xFFD96C8C))
        com.korkoor.pardos.domain.events.EventType.SPRING ->
            EventStyle(R.string.event_spring_title, R.string.event_spring_desc, Icons.Rounded.LocalFlorist, Color(0xFF6BBF8A))
        com.korkoor.pardos.domain.events.EventType.SUMMER ->
            EventStyle(R.string.event_summer_title, R.string.event_summer_desc, Icons.Rounded.WbSunny, Color(0xFFFF8A5B))
        com.korkoor.pardos.domain.events.EventType.INDEPENDENCE ->
            EventStyle(R.string.event_independence_title, R.string.event_independence_desc, Icons.Rounded.Celebration, Color(0xFF1B8A5A))
    }
    val left = event.daysLeft(today)
    val art = when (event.type) {
        com.korkoor.pardos.domain.events.EventType.HALLOWEEN -> R.drawable.ico_pumpkin
        com.korkoor.pardos.domain.events.EventType.DAY_OF_THE_DEAD -> R.drawable.ico_skull
        com.korkoor.pardos.domain.events.EventType.CHRISTMAS -> R.drawable.ico_xmas
        com.korkoor.pardos.domain.events.EventType.NEW_YEAR -> R.drawable.ico_fireworks
        com.korkoor.pardos.domain.events.EventType.VALENTINE -> R.drawable.ico_cupid
        com.korkoor.pardos.domain.events.EventType.SPRING -> R.drawable.ico_tulip
        com.korkoor.pardos.domain.events.EventType.SUMMER -> R.drawable.ico_beach
        com.korkoor.pardos.domain.events.EventType.INDEPENDENCE -> R.drawable.ico_balloon
        com.korkoor.pardos.domain.events.EventType.WEEKEND_GOLD -> R.drawable.ico_star
        com.korkoor.pardos.domain.events.EventType.XP_WEDNESDAY -> R.drawable.ico_sparkles
        com.korkoor.pardos.domain.events.EventType.FESTIVAL_WEEK -> R.drawable.ico_party
    }
    JellyRow(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        fill = color.lighten(0.88f),
        lip = color.copy(alpha = 0.45f),
        lipHeight = 5.dp,
        borderColor = color.copy(alpha = 0.40f),
        padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(art),
            contentDescription = null,
            modifier = Modifier.size(48.dp).wobble(6f, 1900)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(title).uppercase(),
                fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 1.sp, maxLines = 1
            )
            Text(
                if (skinProgress != null) skinProgress else stringResource(desc),
                fontSize = 11.sp, color = if (skinProgress != null) color else Navy.copy(alpha = 0.6f),
                fontWeight = if (skinProgress != null) FontWeight.Bold else FontWeight.Normal, maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (left == 0) stringResource(R.string.event_ends_today) else stringResource(R.string.event_ends_in, left),
            fontSize = 10.sp, fontWeight = FontWeight.Black, color = color
        )
    }
}

private data class EventStyle(val title: Int, val desc: Int, val icon: ImageVector, val color: Color)
