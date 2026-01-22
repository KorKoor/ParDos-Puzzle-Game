package com.korkoor.pardos.ui.menu

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.R

@Composable
fun CustomLevelScreen(
    onStartCustom: (Int, Int, Boolean, String) -> Unit,
    onBack: () -> Unit,
    currentTheme: GameTheme
) {
    var size by remember { mutableIntStateOf(4) }
    var target by remember { mutableIntStateOf(128) }
    var allowPowerUps by remember { mutableStateOf(true) }
    var selectedTimeMode by remember { mutableStateOf("Normal") }

    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bgGradient = Brush.verticalGradient(colors = currentTheme.colors)

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        PicnicBackgroundOptimized(color = currentTheme.accentColor.copy(alpha = 0.05f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = if (isLandscape) 48.dp else 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection(onBack)

            if (isLandscape) {
                // --- DISEÑO HORIZONTAL (LANDSCAPE) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Columna Izquierda: Configuración
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = stringResource(R.string.custom_title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF3D405B)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        CustomOptionsContent(
                            currentTheme = currentTheme,
                            size = size,
                            onSizeChange = { size = it },
                            target = target,
                            onTargetChange = { target = it },
                            allowPowerUps = allowPowerUps,
                            onPowerUpsChange = { allowPowerUps = it },
                            selectedTimeMode = selectedTimeMode,
                            onTimeModeChange = { selectedTimeMode = it }
                        )
                    }

                    // Columna Derecha: Botón de Acción
                    Column(
                        modifier = Modifier.weight(1f).padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { onStartCustom(size, target, allowPowerUps, selectedTimeMode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = currentTheme.accentColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
                        ) {
                            Text(
                                text = stringResource(R.string.start_game_button),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "MODO: ${size}x${size} • OBJETIVO: $target",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3D405B).copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // --- DISEÑO VERTICAL (PORTRAIT) ---
                Text(
                    text = stringResource(R.string.custom_subtitle),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF3D405B).copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
                Text(
                    text = stringResource(R.string.custom_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D405B)
                )

                Spacer(modifier = Modifier.height(32.dp))

                CustomOptionsContent(
                    currentTheme = currentTheme,
                    size = size,
                    onSizeChange = { size = it },
                    target = target,
                    onTargetChange = { target = it },
                    allowPowerUps = allowPowerUps,
                    onPowerUpsChange = { allowPowerUps = it },
                    selectedTimeMode = selectedTimeMode,
                    onTimeModeChange = { selectedTimeMode = it }
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { onStartCustom(size, target, allowPowerUps, selectedTimeMode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = currentTheme.accentColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
                ) {
                    Text(
                        text = stringResource(R.string.start_game_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CustomOptionsContent(
    currentTheme: GameTheme,
    size: Int,
    onSizeChange: (Int) -> Unit,
    target: Int,
    onTargetChange: (Int) -> Unit,
    allowPowerUps: Boolean,
    onPowerUpsChange: (Boolean) -> Unit,
    selectedTimeMode: String,
    onTimeModeChange: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // --- TAMAÑO ---
    SectionHeader(Icons.Default.GridView, stringResource(R.string.section_board_size), currentTheme.accentColor)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(3, 4, 5, 6).forEach { option ->
            SelectableCard("${option}x${option}", size == option, currentTheme.accentColor, Modifier.weight(1f)) {
                onSizeChange(option)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // --- META ---
    SectionHeader(Icons.Default.Star, stringResource(R.string.section_target_tile), currentTheme.accentColor)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(64, 128, 256, 512).forEach { option ->
            SelectableCard(option.toString(), target == option, currentTheme.accentColor, Modifier.weight(1f)) {
                onTargetChange(option)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // --- REGLAS ---
    SectionHeader(Icons.Default.FlashOn, stringResource(R.string.section_game_rules), currentTheme.accentColor)
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(32.dp), color = Color.White.copy(alpha = 0.7f)) {
        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.powerups_active), color = Color(0xFF3D405B), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Switch(checked = allowPowerUps, onCheckedChange = { onPowerUpsChange(it); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = currentTheme.accentColor))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // --- DIFICULTAD ---
    SectionHeader(Icons.Default.Timer, stringResource(R.string.section_time_pressure), currentTheme.accentColor)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val modes = listOf("Zen", "Normal", "Pro")
        modes.forEach { mode ->
            val display = when(mode) {
                "Zen" -> stringResource(R.string.diff_zen)
                "Normal" -> stringResource(R.string.diff_normal)
                else -> stringResource(R.string.diff_pro)
            }
            SelectableCard(display, selectedTimeMode == mode, currentTheme.accentColor, Modifier.weight(1f)) {
                onTimeModeChange(mode)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
}

@Composable
private fun HeaderSection(onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)) {
            Icon(Icons.Default.ArrowBackIosNew, stringResource(R.string.back), tint = Color(0xFF3D405B), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SelectableCard(text: String, isSelected: Boolean, accentColor: Color, modifier: Modifier, onSelect: () -> Unit) {
    val backgroundColor by animateColorAsState(if (isSelected) accentColor else Color.White.copy(alpha = 0.7f), label = "bg")
    val textColor by animateColorAsState(if (isSelected) Color.White else Color(0xFF3D405B), label = "text")

    Surface(
        onClick = onSelect,
        modifier = modifier.height(68.dp).scale(if (isSelected) 1.05f else 1f),
        shape = RoundedCornerShape(32.dp),
        color = backgroundColor,
        shadowElevation = if (isSelected) 12.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1)
        }
    }
}

@Composable
fun SectionHeader(icon: ImageVector, title: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3D405B).copy(alpha = 0.4f), letterSpacing = 1.2.sp)
    }
}