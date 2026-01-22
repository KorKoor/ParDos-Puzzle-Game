package com.korkoor.pardos.ui.game.components

import com.korkoor.pardos.R
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.ui.theme.GameTheme
import kotlinx.coroutines.delay
import android.content.res.Configuration

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isLandscape) 0.9f else 0.85f),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    vertical = if (isLandscape) 16.dp else 32.dp,
                    horizontal = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLandscape) {
                    // --- DISEÑO HORIZONTAL (LANDSCAPE) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Lado Izquierdo: Estrellas y Título
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedStarsRow(stars)

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = if (stars >= 2) stringResource(R.string.victory_mastered)
                                else stringResource(R.string.victory_well_done),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.accentColor,
                                letterSpacing = 2.sp
                            )

                            Text(
                                text = if (modeName.contains("Tabla") || modeName.contains("Multi"))
                                    stringResource(R.string.mode_tables_with_value, base)
                                else modeName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF3D405B)
                            )
                        }

                        // Lado Derecho: Estadísticas y Botones
                        Column(modifier = Modifier.weight(1.2f)) {
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
                                StatDetail(
                                    label = stringResource(R.string.time_label),
                                    value = formatTime(timeElapsed),
                                    color = Color(0xFF3D405B),
                                    delayMillis = 600
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            PersonalRecordsBox(currentTheme, bestMoves, bestTime)

                            Spacer(Modifier.height(16.dp))

                            ActionButtons(currentTheme, onRetry, onDismiss)
                        }
                    }
                } else {
                    // --- DISEÑO VERTICAL (PORTRAIT) ---
                    AnimatedStarsRow(stars)

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (stars >= 2) stringResource(R.string.victory_mastered)
                        else stringResource(R.string.victory_well_done),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.accentColor,
                        letterSpacing = 3.sp
                    )

                    Text(
                        text = if (modeName.contains("Tabla") || modeName.contains("Multi"))
                            stringResource(R.string.mode_tables_with_value, base)
                        else modeName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3D405B)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 20.dp).alpha(0.1f),
                        color = Color(0xFF3D405B)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatDetail(label = stringResource(R.string.moves_label), value = moves.toString(), color = Color(0xFF3D405B), delayMillis = 800)
                        StatDetail(label = stringResource(R.string.time_label), value = formatTime(timeElapsed), color = Color(0xFF3D405B), delayMillis = 1000)
                    }

                    Spacer(Modifier.height(20.dp))
                    PersonalRecordsBox(currentTheme, bestMoves, bestTime)

                    Spacer(Modifier.height(32.dp))
                    ActionButtons(currentTheme, onRetry, onDismiss)
                }
            }
        }
    }
}

@Composable
private fun AnimatedStarsRow(stars: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val isFilled = index < stars
            var startAnim by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(index * 250L)
                startAnim = true
            }

            val scale by animateFloatAsState(
                targetValue = if (startAnim && isFilled) 1f else if (startAnim) 0.8f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "StarAnim"
            )

            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (isFilled) Color(0xFFFFD700) else Color.LightGray.copy(alpha = 0.3f),
                modifier = Modifier.size(54.dp).scale(scale).padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun PersonalRecordsBox(currentTheme: GameTheme, bestMoves: Int, bestTime: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(currentTheme.accentColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.personal_records),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = currentTheme.accentColor.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.records_combined_format, bestMoves, formatTime(bestTime)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF3D405B).copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ActionButtons(currentTheme: GameTheme, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, currentTheme.accentColor.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry), tint = currentTheme.accentColor)
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.weight(2f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
            shape = RoundedCornerShape(18.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(stringResource(R.string.next_button), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun StatDetail(label: String, value: String, color: Color, delayMillis: Int = 0) {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        startAnim = true
    }

    val alpha by animateFloatAsState(targetValue = if (startAnim) 1f else 0f, animationSpec = tween(500), label = "StatAlpha")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer(alpha = alpha).clearAndSetSemantics { contentDescription = "$value, $label" }
    ) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
        Text(text = label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.5f), letterSpacing = 1.sp)
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}