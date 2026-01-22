package com.example.pardos.ui.menu

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pardos.domain.model.GameMode
import com.example.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.example.pardos.ui.theme.GameTheme
import com.example.pardos.R

@Composable
fun ModeSelectionScreen(
    onModeSelected: (GameMode) -> Unit,
    onBack: () -> Unit,
    currentTheme: GameTheme
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bgGradient = Brush.verticalGradient(colors = currentTheme.colors)

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        PicnicBackgroundOptimized(color = currentTheme.accentColor.copy(alpha = 0.05f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = if (isLandscape) 40.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- CABECERA ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isLandscape) 12.dp else 20.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF3D405B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.mode_selection_subtitle),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF3D405B).copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = stringResource(R.string.mode_selection_title),
                        fontSize = if (isLandscape) 20.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3D405B)
                    )
                }
            }

            // --- LISTADO DE MODOS (Adaptable) ---
            if (isLandscape) {
                // En horizontal usamos una Grid de 2 columnas
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { ModeItem(onModeSelected, GameMode.CLASICO, currentTheme.accentColor, Icons.Default.Bolt) }
                    item { ModeItem(onModeSelected, GameMode.TABLAS, Color(0xFF81B29A), Icons.Default.Calculate) }
                    item { ModeItem(onModeSelected, GameMode.DESAFIO, Color(0xFFE07A5F), Icons.Default.Timer) }
                    item { ModeItem(onModeSelected, GameMode.ZEN, Color(0xFFF2CC8F), Icons.Default.History) }
                }
            } else {
                // En vertical mantenmos la lista original con scroll
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModeSelectionCard(
                        title = stringResource(R.string.mode_campaign_title),
                        description = stringResource(R.string.mode_campaign_desc),
                        icon = Icons.Default.Bolt,
                        accentColor = currentTheme.accentColor,
                        onClick = { onModeSelected(GameMode.CLASICO) }
                    )

                    ModeSelectionCard(
                        title = stringResource(R.string.mode_tables_title),
                        description = stringResource(R.string.mode_tables_desc),
                        icon = Icons.Default.Calculate,
                        accentColor = Color(0xFF81B29A),
                        onClick = { onModeSelected(GameMode.TABLAS) }
                    )

                    ModeSelectionCard(
                        title = stringResource(R.string.mode_challenge_title),
                        description = stringResource(R.string.mode_challenge_desc),
                        icon = Icons.Default.Timer,
                        accentColor = Color(0xFFE07A5F),
                        onClick = { onModeSelected(GameMode.DESAFIO) }
                    )

                    ModeSelectionCard(
                        title = stringResource(R.string.mode_zen_title),
                        description = stringResource(R.string.mode_zen_desc),
                        icon = Icons.Default.History,
                        accentColor = Color(0xFFF2CC8F),
                        onClick = { onModeSelected(GameMode.ZEN) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // --- FOOTER ---
            Text(
                text = stringResource(R.string.mode_footer_hint),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF3D405B).copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = if (isLandscape) 8.dp else 24.dp)
            )
        }
    }
}

// Helper para no repetir código en el modo Grid
@Composable
fun ModeItem(onModeSelected: (GameMode) -> Unit, mode: GameMode, color: Color, icon: ImageVector) {
    val titleRes = when(mode) {
        GameMode.CLASICO -> R.string.mode_campaign_title
        GameMode.TABLAS -> R.string.mode_tables_title
        GameMode.DESAFIO -> R.string.mode_challenge_title
        else -> R.string.mode_zen_title
    }
    val descRes = when(mode) {
        GameMode.CLASICO -> R.string.mode_campaign_desc
        GameMode.TABLAS -> R.string.mode_tables_desc
        GameMode.DESAFIO -> R.string.mode_challenge_desc
        else -> R.string.mode_zen_desc
    }
    ModeSelectionCard(
        title = stringResource(titleRes),
        description = stringResource(descRes),
        icon = icon,
        accentColor = color,
        onClick = { onModeSelected(mode) }
    )
}

@Composable
fun ModeSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = 0.7f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(20.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D405B),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF3D405B).copy(alpha = 0.6f),
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }
        }
    }
}