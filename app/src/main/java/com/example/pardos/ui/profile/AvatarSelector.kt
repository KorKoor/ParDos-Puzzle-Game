package com.korkoor.pardos.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.korkoor.pardos.R

/**
 * Mejoramos MUCHO el diseño: Ahora es un Custom Dialog con estética
 * Café-Beige, bordes suaves y feedback visual al seleccionar.
 */
fun getAvatarResource(avatarId: Int): Int {
    return when (avatarId) {
        1 -> R.drawable.avatar_1
        2 -> com.korkoor.pardos.R.drawable.avatar_2
        3 -> com.korkoor.pardos.R.drawable.avatar_3
        4 -> com.korkoor.pardos.R.drawable.avatar_4
        5 -> com.korkoor.pardos.R.drawable.avatar_5
        6 -> com.korkoor.pardos.R.drawable.avatar_6
        7 -> com.korkoor.pardos.R.drawable.avatar_7
        8 -> com.korkoor.pardos.R.drawable.avatar_8
        9 -> com.korkoor.pardos.R.drawable.avatar_9
        10 -> com.korkoor.pardos.R.drawable.avatar_10
        else -> R.drawable.avatar_1 // Por si las moscas
    }
}
@Composable
fun AvatarSelectorDialog(
    currentAvatarId: Int,
    onAvatarSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Colores ParDos Zen
    val cafeOscuro = Color(0xFF5D4037)
    val cafeSuave = Color(0xFF8D6E63)
    val cremaFondo = Color(0xFFFDF8F1)
    val blancoPuro = Color(0xFFFFFFFF)

    val avataresDisponibles = (1..10).toList()

    // Usamos Dialog normal para tener control total del diseño (Custom UI)
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(32.dp), // Esquinas súper redondeadas
            color = cremaFondo,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título Estilizado
                Text(
                    text = "IDENTIDAD ZEN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = cafeSuave,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Elige tu nuevo avatar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = cafeOscuro
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Cuadrícula de Avatares
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(320.dp) // Altura fija para el scroll
                ) {
                    items(avataresDisponibles) { avatarId ->
                        val isSelected = currentAvatarId == avatarId

                        // Animación de escala al estar seleccionado
                        val scale by animateFloatAsState(if (isSelected) 1.15f else 1f, label = "scale")

                        Box(
                            modifier = Modifier
                                .size(85.dp)
                                .scale(scale)
                                .shadow(
                                    elevation = if (isSelected) 12.dp else 0.dp,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .background(if (isSelected) blancoPuro else Color.Transparent)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) cafeOscuro else cafeSuave.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { onAvatarSelected(avatarId) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = getAvatarResource(avatarId)),
                                contentDescription = "Avatar $avatarId",
                                modifier = Modifier
                                    .size(70.dp)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de Confirmación Estético
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = cafeOscuro),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        "CONFIRMAR SELECCIÓN",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}