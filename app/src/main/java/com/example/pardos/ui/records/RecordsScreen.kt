package com.korkoor.pardos.ui.records

import android.content.res.Configuration // ✅ Importado para orientación
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration // ✅ Importado para configuración local
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.model.Record
import com.korkoor.pardos.ui.theme.GameTheme
import java.text.SimpleDateFormat
import java.util.*
import com.korkoor.pardos.R

@Composable
fun RecordsScreen(
    records: List<Record>,
    currentTheme: GameTheme,
    onBack: () -> Unit
) {
    val bgGradient = Brush.verticalGradient(colors = currentTheme.colors)

    // ✅ Detectamos la orientación
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 🌍 Agrupamos usando identificadores de modo consistentes
    val groupedRecords = remember(records) {
        records.groupBy { record ->
            when {
                record.mode.contains("Tablas") || record.mode.contains("Tables") -> "TABLES"
                record.mode.uppercase().contains("DESAFIO") || record.mode.uppercase().contains("CHALLENGE") -> "CHALLENGE"
                record.mode.uppercase().contains("ZEN") -> "ZEN"
                else -> "CAMPAIGN"
            }
        }.mapValues { entry ->
            entry.value.sortedByDescending { it.score }.take(5)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {

        if (isLandscape) {
            // ---------------------------------------------------------
            // 🔄 DISEÑO HORIZONTAL (LANDSCAPE)
            // ---------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // COLUMNA IZQUIERDA: Info estática + Botón
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HALL OF FAME",
                        fontSize = 28.sp, // Un poco más pequeño para landscape
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3D405B),
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.records_subtitle),
                        fontSize = 12.sp,
                        color = Color(0xFF3D405B).copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón en la columna izquierda
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.back_to_menu),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                // COLUMNA DERECHA: La lista de récords
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                ) {
                    if (records.isEmpty()) {
                        EmptyStateView()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            groupedRecords.forEach { (modeKey, topRecords) ->
                                item {
                                    SectionHeader(modeKey, currentTheme.accentColor)
                                }
                                items(topRecords) { record ->
                                    RecordCard(record)
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // ---------------------------------------------------------
            // 📱 DISEÑO VERTICAL (PORTRAIT) - ORIGINAL
            // ---------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "HALL OF FAME",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D405B),
                    letterSpacing = 6.sp
                )
                Text(
                    text = stringResource(R.string.records_subtitle),
                    fontSize = 13.sp,
                    color = Color(0xFF3D405B).copy(alpha = 0.5f),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (records.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateView()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        groupedRecords.forEach { (modeKey, topRecords) ->
                            item {
                                SectionHeader(modeKey, currentTheme.accentColor)
                            }
                            items(topRecords) { record ->
                                RecordCard(record)
                            }
                        }
                    }
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(bottom = 32.dp, top = 16.dp)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.back_to_menu),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// 🛠️ COMPONENTE AUXILIAR PARA ESTADO VACÍO (Reutilizado en ambas vistas)
@Composable
private fun EmptyStateView() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Star,
            null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFF3D405B).copy(alpha = 0.1f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.records_empty),
            color = Color(0xFF3D405B).copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SectionHeader(modeKey: String, themeAccent: Color) {
    // 🌍 Traducimos el nombre del grupo
    val (title, color, icon) = when (modeKey) {
        "TABLES" -> Triple(stringResource(R.string.mode_tables), Color(0xFF6C63FF), Icons.Default.Calculate)
        "CHALLENGE" -> Triple(stringResource(R.string.mode_challenge), Color(0xFFE07A5F), Icons.Default.Bolt)
        "ZEN" -> Triple(stringResource(R.string.mode_zen), Color(0xFF6C63FF), Icons.Default.Spa)
        else -> Triple(stringResource(R.string.mode_campaign_title), themeAccent, Icons.Default.EmojiEvents)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun RecordCard(record: Record) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.75f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFFD700), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format("%,d", record.score),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3D405B)
                    )
                }

                // 🌍 Traducimos el detalle de la ficha máxima
                val detailText = if (record.mode.contains("Tabla") || record.mode.contains("Table")) {
                    val tableName = record.mode.substringAfter("(").replace(")", "")
                    "${stringResource(R.string.records_level)} ${record.level} • $tableName"
                } else {
                    "${stringResource(R.string.max_tile_label)}: ${record.level}"
                }

                Text(
                    text = detailText,
                    fontSize = 12.sp,
                    color = Color(0xFF3D405B).copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(12.dp), tint = Color(0xFF3D405B).copy(alpha = 0.3f))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatDate(record.date),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF3D405B).copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}