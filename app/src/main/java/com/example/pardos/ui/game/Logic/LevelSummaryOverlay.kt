package com.korkoor.pardos.ui.game.components

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.R
import com.korkoor.pardos.ui.theme.GameTheme
import kotlinx.coroutines.delay

@Composable
fun LevelSummaryOverlay(
    modeName: String,
    base: Int,
    moves: Int,
    timeElapsed: Long,
    bestMoves: Int,
    bestTime: Long,
    stars: Int,
    currentTheme: GameTheme,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "CardPop"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "CardFade"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.85f else 0.88f)
                .scale(scale)
                .alpha(alpha)
                .shadow(30.dp, RoundedCornerShape(32.dp), spotColor = currentTheme.accentColor.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            border = BorderStroke(2.dp, Brush.linearGradient(
                colors = listOf(Color.White, currentTheme.accentColor.copy(alpha = 0.3f))
            ))
        ) {
            Column(
                modifier = Modifier.padding(
                    vertical = if (isLandscape) 20.dp else 32.dp,
                    horizontal = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedStarsRow(stars, currentTheme)
                            Spacer(Modifier.height(12.dp))
                            VictoryHeader(stars, modeName, base, currentTheme)
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(180.dp)
                                .background(Color.LightGray.copy(alpha = 0.4f))
                        )

                        Column(
                            modifier = Modifier.weight(1.3f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StatsRow(moves, timeElapsed)
                            Spacer(Modifier.height(16.dp))
                            PersonalRecordsBox(currentTheme, bestMoves, bestTime)
                            Spacer(Modifier.height(20.dp))
                            ActionButtons(currentTheme, onRetry, onDismiss)
                        }
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(currentTheme.accentColor.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                        )
                        AnimatedStarsRow(stars, currentTheme)
                    }

                    Spacer(Modifier.height(12.dp))
                    VictoryHeader(stars, modeName, base, currentTheme)
                    Spacer(modifier = Modifier.height(24.dp))
                    StatsRow(moves, timeElapsed)
                    Spacer(Modifier.height(24.dp))
                    PersonalRecordsBox(currentTheme, bestMoves, bestTime)
                    Spacer(Modifier.height(32.dp))
                    ActionButtons(currentTheme, onRetry, onDismiss)
                }
            }
        }
    }
}

@Composable
private fun VictoryHeader(stars: Int, modeName: String, base: Int, currentTheme: GameTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (stars >= 2) stringResource(R.string.victory_mastered).uppercase()
            else stringResource(R.string.victory_well_done).uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = currentTheme.accentColor,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (modeName.contains("Tabla") || modeName.contains("Multi"))
                stringResource(R.string.mode_tables_with_value, base)
            else modeName,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF3D405B),
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun StatsRow(moves: Int, timeElapsed: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatDetail(
            label = stringResource(R.string.moves_label),
            value = moves.toString(),
            color = Color(0xFF3D405B),
            delayMillis = 400
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Color.LightGray.copy(alpha = 0.5f))
        )
        StatDetail(
            label = stringResource(R.string.time_label),
            value = formatTime(timeElapsed),
            color = Color(0xFF3D405B),
            delayMillis = 600
        )
    }
}

@Composable
private fun AnimatedStarsRow(stars: Int, currentTheme: GameTheme) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val isFilled = index < stars
            var startAnim by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(index * 200L)
                startAnim = true
            }

            val scale by animateFloatAsState(
                targetValue = if (startAnim && isFilled) 1f else if (startAnim) 0.8f else 0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
                label = "StarAnim"
            )

            val rotation by animateFloatAsState(
                targetValue = if (startAnim && isFilled) 0f else -30f,
                animationSpec = tween(500),
                label = "StarRot"
            )

            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isFilled) Color(0xFFFFD700) else Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        }
                        .padding(horizontal = 4.dp)
                        .then(if (isFilled) Modifier.shadow(8.dp, CircleShape, spotColor = Color(0xFFFFD700)) else Modifier)
                )
            }
        }
    }
}

@Composable
private fun PersonalRecordsBox(currentTheme: GameTheme, bestMoves: Int, bestTime: Long) {
    // 🔥 FIX: Manejamos los guiones como String y evitamos pasarlos a stringResource
    // para prevenir la IllegalFormatConversionException (%d != String)
    val displayTime = if (bestTime <= 0L || bestTime == Long.MAX_VALUE) "--:--" else formatTime(bestTime)
    val displayMoves = if (bestMoves <= 0 || bestMoves == Int.MAX_VALUE) "--" else bestMoves.toString()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(currentTheme.accentColor.copy(alpha = 0.08f))
            .border(1.dp, currentTheme.accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.personal_records).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.accentColor.copy(alpha = 0.8f),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))

            // 🛠️ FIX FINAL: Concatenamos los valores ya formateados como String
            // Esto evita que el sistema truene si el XML espera un número (%d)
            Text(
                text = "$displayMoves mov.  |  $displayTime",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3D405B).copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ActionButtons(currentTheme: GameTheme, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .weight(1f)
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, currentTheme.accentColor.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = currentTheme.accentColor
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.retry),
                modifier = Modifier.size(24.dp)
            )
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .weight(2f)
                .height(60.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = currentTheme.accentColor.copy(alpha = 0.4f)),
            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = stringResource(R.string.next_button),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun StatDetail(
    label: String,
    value: String,
    color: Color,
    delayMillis: Int = 0
) {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        startAnim = true
    }

    val alpha by animateFloatAsState(targetValue = if (startAnim) 1f else 0f, animationSpec = tween(500), label = "StatAlpha")
    val offsetY by animateFloatAsState(targetValue = if (startAnim) 0f else 20f, animationSpec = spring(dampingRatio = 0.6f), label = "StatSlide")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
            .clearAndSetSemantics { contentDescription = "$value, $label" }
    ) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = (-1).sp)
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.4f), letterSpacing = 1.5.sp)
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"

    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes > 99) {
        "99:59"
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}