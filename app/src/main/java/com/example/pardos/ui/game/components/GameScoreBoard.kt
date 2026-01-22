package com.example.pardos.ui.game.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pardos.domain.model.BoardState
/*
@Composable
fun HeaderSection(state: BoardState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PARDOS", fontSize = 58.sp, fontWeight = FontWeight.Black, color = Color(0xFF3D405B), letterSpacing = 12.sp)
        Surface(color = Color(0xFF81B29A).copy(alpha = 0.12f), shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 8.dp)) {
            Text("NIVEL ${state.currentLevel}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color(0xFF81B29A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun FooterSection(state: BoardState, formattedTime: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard("PUNTOS", state.score.toString(), Modifier.weight(1f))
        InfoCard("META", state.levelLimit.toString(), Modifier.weight(1f))
        InfoCard("RELOJ", formattedTime, Modifier.weight(1f))
    }
}
*/
@Composable
private fun InfoCard(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = Color(0xFF3D405B).copy(0.4f), fontWeight = FontWeight.Bold)
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3D405B))
        }
    }
}