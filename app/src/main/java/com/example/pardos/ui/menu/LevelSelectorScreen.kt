package com.korkoor.pardos.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.model.LevelInfo
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.theme.GameTheme

@Composable
fun LevelSelectorScreen(
    levels: List<LevelInfo>,
    currentTheme: GameTheme,
    onLevelSelected: (LevelInfo) -> Unit,
    onBack: () -> Unit,
    // 🔥 NUEVO: Función para pedir recarga de datos
    onRefresh: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val bgGradient = Brush.verticalGradient(colors = currentTheme.colors)
    val gridState = rememberLazyGridState()

    val lastUnlockedIndex = levels.indexOfLast { !it.isLocked }.coerceAtLeast(0)

    // 🔥 ESTA ES LA CLAVE:
    // Cada vez que esta pantalla se hace visible (incluso al volver del juego),
    // ejecutamos onRefresh() para obligar al ViewModel a leer las SharedPreferences.
    LaunchedEffect(Unit) {
        onRefresh()
        gridState.scrollToItem(lastUnlockedIndex)
    }

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        PicnicBackgroundOptimized(color = currentTheme.accentColor.copy(alpha = 0.05f))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // --- CABECERA ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = if (isLandscape) 12.dp else 20.dp
                    )
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Atrás",
                        tint = Color(0xFF3D405B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MODO CAMPAÑA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF3D405B).copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "2,400 Retos",
                        fontSize = if (isLandscape) 20.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3D405B),
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // --- GRID ---
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(if (isLandscape) 6 else 3),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = if (isLandscape) 20.dp else 40.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(levels) { level ->
                    LevelCardAesthetic(
                        level = level,
                        accentColor = currentTheme.accentColor,
                        onClick = { onLevelSelected(level) }
                    )
                }
            }
        }
    }
}

@Composable
fun LevelCardAesthetic(
    level: LevelInfo,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isLocked = level.isLocked
    val isCurrent = !isLocked && level.starsEarned == 0

    val cardColor = if (isLocked) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.7f)

    Surface(
        onClick = if (!isLocked) onClick else ({}),
        modifier = Modifier
            .aspectRatio(0.85f)
            .shadow(
                elevation = if (isCurrent) 12.dp else 0.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = accentColor.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(32.dp),
        color = cardColor,
        border = if (isCurrent) BorderStroke(2.dp, accentColor) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF3D405B).copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "${level.id}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D405B)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { index ->
                        val isFilled = index < level.starsEarned
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = if (isFilled) Color(0xFFFFD700) else Color(0xFF3D405B).copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}