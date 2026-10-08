package com.korkoor.pardos.ui.game

import FloatingScore
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pardos.ui.game.components.SakuraBackgroundAnimation
import com.korkoor.pardos.R
import com.korkoor.pardos.domain.achievements.AchievementPopUp
import com.korkoor.pardos.domain.logic.Direction
import com.korkoor.pardos.domain.model.BoardState
import com.korkoor.pardos.domain.model.GameMode
import com.korkoor.pardos.ui.game.components.*
import com.korkoor.pardos.ui.game.logic.AdManager
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.profile.ProfileSetupDialog
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.ui.theme.ThemeSelector
import com.korkoor.pardos.ui.theme.ThemeViewModel
import com.korkoor.pardos.data.local.ProfileManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun GameHeader(
    state: BoardState,
    currentTheme: GameTheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = currentTheme.accentColor.copy(alpha = 0.12f),
            border = BorderStroke(0.5.dp, currentTheme.accentColor.copy(alpha = 0.3f)),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = currentTheme.name.uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = currentTheme.mainTextColor,
                letterSpacing = 1.5.sp
            )
        }

        Box(modifier = Modifier.scale(0.9f)) {
            AnimatedLevelDisplay(
                level = state.currentLevel,
                textColor = currentTheme.mainTextColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Box(modifier = Modifier.scale(0.85f)) {
            ObjectiveCard(
                targetPiece = state.levelLimit,
                boardSize = state.boardSize,
                backgroundColor = currentTheme.surfaceColor,
                modifier = Modifier.padding(top = 0.dp)
            )
        }

        if (state.maxTime != null) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .scale(0.8f)
            ) {
                TimeDisplay(
                    elapsedTime = state.elapsedTime,
                    accentColor = currentTheme.accentColor,
                    textColor = currentTheme.mainTextColor
                )
            }
        }
    }
}

@Composable
internal fun TimerDisplay(
    seconds: Long,
    isLowTime: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFE07A5F),
    textColor: Color = Color(0xFF3D405B)
) {
    val animatedTextColor by animateColorAsState(
        targetValue = if (isLowTime) accentColor else textColor.copy(alpha = 0.7f),
        animationSpec = tween(300),
        label = "TimerColor"
    )

    val isCritical = seconds <= 5_000 && isLowTime

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .graphicsLayer {
                if (isCritical) translationX = shakeOffset
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = stringResource(R.string.time_label),
            tint = animatedTextColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = seconds.formatTime(),
            color = animatedTextColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
internal fun TimeDisplay(
    elapsedTime: Long,
    modifier: Modifier = Modifier,
    accentColor: Color,
    textColor: Color
) {
    val isUrgent = elapsedTime <= 10_000
    val isCritical = elapsedTime <= 5_000

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Column(
        modifier = modifier
            .padding(top = 16.dp)
            .graphicsLayer {
                if (isCritical) {
                    scaleX = scale
                    scaleY = scale
                    translationX = shakeOffset
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isUrgent) stringResource(R.string.hurry_up) else stringResource(R.string.time_remaining),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isUrgent) Color(0xFFE07A5F) else Color(0xFF3D405B).copy(alpha = 0.4f),
            letterSpacing = 1.2.sp
        )

        Text(
            text = elapsedTime.formatTime(),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = if (isUrgent) Color(0xFFE07A5F) else Color(0xFF3D405B)
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")

@Composable
internal fun GameBoard(
    state: BoardState,
    selectedShapeType: String,
    viewModel: GameViewModel,
    haptic: HapticFeedback,
    currentTheme: GameTheme,
    onMoveSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = state.boardSize
    val isLargeGrid = gridSize >= 4

    val boardPadding = when {
        gridSize >= 5 -> 0.dp
        gridSize == 4 -> 2.dp
        else -> 8.dp
    }

    Box(
        modifier = modifier
            .padding(boardPadding)
            .background(
                color = currentTheme.surfaceColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (isLargeGrid) 1.dp else 3.dp,
                color = currentTheme.accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // 🔥 OPTIMIZACIÓN: Solo se recrea el display si cambia el tamaño
        key(gridSize) {
            BoardDisplay(
                state = state,
                viewModel = viewModel,
                shapeType = selectedShapeType,
                haptic = haptic,
                currentTheme = currentTheme,
                onMoveSound = onMoveSound,
                modifier = Modifier.fillMaxSize()
            )
        }

        viewModel.floatingScores.forEach { score ->
            key(score.id) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val tileSize = maxWidth / gridSize

                    FloatingScore(
                        score = score,
                        tileSize = tileSize,
                        onFinished = { id -> viewModel.removeFloatingScore(id) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun GameFooter(
    state: BoardState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCard(
            icon = Icons.Default.Flag,
            value = state.moveCount.toString(),
            label = stringResource(R.string.moves_label),
            color = Color(0xFF81B29A)
        )

        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            TimerDisplay(
                seconds = state.elapsedTime,
                isLowTime = state.gameMode == GameMode.DESAFIO && state.elapsedTime in 1..10_000,
                modifier = Modifier.scale(0.85f)
            )
        }

        if (state.score > 0) {
            StatCard(
                icon = Icons.Default.Flag,
                value = state.score.toString(),
                label = stringResource(R.string.points_label),
                color = Color(0xFFE07A5F)
            )
        }
    }
}

@Composable
internal fun AnimatedLevelDisplay(
    level: Int,
    modifier: Modifier = Modifier,
    textColor: Color
) {
    AnimatedContent(
        targetState = level,
        transitionSpec = {
            slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> -height } + fadeOut()
        },
        label = "LevelSlotAnimation"
    ) { targetLevel ->
        Text(
            text = stringResource(R.string.level_label, targetLevel),
            modifier = modifier,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF3D405B)
        )
    }
}

@Composable
internal fun ObjectiveCard(
    targetPiece: Int,
    boardSize: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF3D405B).copy(alpha = 0.05f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = stringResource(R.string.objective_title),
                modifier = Modifier.size(18.dp),
                tint = Color(0xFFE07A5F)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.goal_label, targetPiece, boardSize),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3D405B).copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
internal fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            BouncingText(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D405B)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF3D405B).copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
    }
}
