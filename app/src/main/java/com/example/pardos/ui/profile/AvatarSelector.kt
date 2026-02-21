package com.korkoor.pardos.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.korkoor.pardos.R

// 1. FUNCIÓN PARA OBTENER LA IMAGEN (Se usa para ti y para tus amigos)
fun getAvatarResource(avatarId: Int): Int {
    return when (avatarId) {
        1 -> R.drawable.avatar_1
        2 -> R.drawable.avatar_2
        3 -> R.drawable.avatar_3
        4 -> R.drawable.avatar_4
        5 -> R.drawable.avatar_5
        6 -> R.drawable.avatar_6
        7 -> R.drawable.avatar_7
        8 -> R.drawable.avatar_8
        9 -> R.drawable.avatar_9
        10 -> R.drawable.avatar_10
        else -> R.drawable.avatar_1 // Avatar por defecto si hay algún error
    }
}

// 2. EL COMPONENTE VISUAL PARA SELECCIONAR AVATAR
@Composable
fun AvatarSelectorDialog(
    currentAvatarId: Int,
    onAvatarSelected: (Int) -> Unit, // Se dispara cuando tocas un nuevo avatar
    onDismissRequest: () -> Unit // Se dispara para cerrar la ventanita
) {
    val cafeOscuro = Color(0xFF5D4037)
    val cremaFondo = Color(0xFFF5F5DC)

    // Generamos una lista del 1 al 10
    val avataresDisponibles = (1..10).toList()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = cremaFondo,
        title = {
            Text(text = "Elige tu Avatar Zen", color = cafeOscuro)
        },
        text = {
            // Un grid (cuadrícula) para acomodar los 10 avatares de forma elegante
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // 3 columnas
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(avataresDisponibles) { avatarId ->
                    val isSelected = currentAvatarId == avatarId

                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            // Le ponemos un borde si está seleccionado para que resalte
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) cafeOscuro else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onAvatarSelected(avatarId) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = getAvatarResource(avatarId)),
                            contentDescription = "Avatar $avatarId",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cerrar", color = cafeOscuro)
            }
        }
    )
}