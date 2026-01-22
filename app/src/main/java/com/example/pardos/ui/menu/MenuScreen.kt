package com.example.pardos.ui.menu

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.example.pardos.ui.theme.ThemeViewModel
import com.example.pardos.R

@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onCustomClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onDailyChallengeClick: () -> Unit,
    themeViewModel: ThemeViewModel
) {
    // Detección de orientación
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentTheme = themeViewModel.currentTheme
    val bgColor = currentTheme.colors.first().copy(alpha = 0.98f)
    val textColor = currentTheme.mainTextColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        PicnicBackgroundOptimized(currentTheme.accentColor.copy(alpha = 0.06f))

        if (isLandscape) {
            // --- DISEÑO HORIZONTAL (LANDSCAPE) ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 40.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lado Izquierdo: Título y Slogan
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedTitle("PARDOS", textColor, fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.menu_slogan),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor.copy(alpha = 0.4f),
                        letterSpacing = 4.sp
                    )
                }

                // Lado Derecho: Botones en rejilla
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DailyChallengeButton(onDailyChallengeClick, modifier = Modifier.fillMaxWidth().height(60.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AestheticMenuButton(
                            text = stringResource(R.string.menu_play),
                            color = currentTheme.accentColor,
                            onClick = onPlayClick,
                            modifier = Modifier.weight(1f).height(60.dp)
                        )
                        AestheticMenuButton(
                            text = stringResource(R.string.menu_customize),
                            color = Color(0xFF81B29A),
                            onClick = onCustomClick,
                            modifier = Modifier.weight(1f).height(60.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniMenuButton(
                            text = stringResource(R.string.menu_records),
                            color = Color(0xFFE07A5F),
                            modifier = Modifier.weight(1f),
                            onClick = onRecordsClick
                        )
                        MiniMenuButton(
                            text = stringResource(R.string.menu_achievements),
                            color = Color(0xFF6C63FF),
                            modifier = Modifier.weight(1f),
                            onClick = onAchievementsClick
                        )
                    }
                }
            }
        } else {
            // --- DISEÑO VERTICAL (PORTRAIT) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                AnimatedTitle("PARDOS", textColor)

                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(currentTheme.accentColor, CircleShape)
                )

                Text(
                    text = stringResource(R.string.menu_slogan),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor.copy(alpha = 0.4f),
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DailyChallengeButton(onDailyChallengeClick)

                    AestheticMenuButton(
                        text = stringResource(R.string.menu_play),
                        color = currentTheme.accentColor,
                        onClick = onPlayClick
                    )

                    AestheticMenuButton(
                        text = stringResource(R.string.menu_customize),
                        color = Color(0xFF81B29A),
                        onClick = onCustomClick
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MiniMenuButton(
                            text = stringResource(R.string.menu_records),
                            color = Color(0xFFE07A5F),
                            modifier = Modifier.weight(1f),
                            onClick = onRecordsClick
                        )
                        MiniMenuButton(
                            text = stringResource(R.string.menu_achievements),
                            color = Color(0xFF6C63FF),
                            modifier = Modifier.weight(1f),
                            onClick = onAchievementsClick
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.8f))

                Text(
                    text = stringResource(R.string.menu_version_info),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor.copy(alpha = 0.3f),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun AestheticMenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(0.92f).height(74.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "Scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = color.copy(alpha = 0.5f)
            )
            .background(
                brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF9FAFB))),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = 4.sp
            )
        }
    }
}

@Composable
fun DailyChallengeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(0.92f).height(74.dp)
) {
    Button(
        onClick = onClick,
        modifier = modifier.shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D405B))
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF2CC8F))
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.menu_daily_challenge),
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            fontSize = 15.sp
        )
    }
}

@Composable
fun MiniMenuButton(
    text: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        border = BorderStroke(2.dp, color.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, color.copy(alpha = 0.05f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnimatedTitle(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 56.sp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            val infiniteTransition = rememberInfiniteTransition(label = "Title$index")
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = index * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Bounce"
            )

            Text(
                text = char.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = color,
                modifier = Modifier
                    .offset(y = yOffset.dp)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}