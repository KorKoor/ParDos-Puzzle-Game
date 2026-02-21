package com.korkoor.pardos.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image

@Composable
fun ProfileSetupDialog(
    onProfileSaved: (String, Int) -> Unit
) {
    // --- NUEVA PALETA DE COLORES ---
    val cafeOscuro = Color(0xFF5D4037)
    val cafeSuave = Color(0xFF8D6E63)
    val cremaFondo = Color(0xFFFDF8F1)
    val terracota = Color(0xFFE07A5F)

    var nombreUsuario by remember { mutableStateOf("") }
    var avatarSeleccionado by remember { mutableStateOf(1) }
    var mostrarSelectorAvatar by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(24.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = cremaFondo
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White, cremaFondo)
                        )
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icono decorativo arriba del título
                Text("✨", fontSize = 32.sp)

                Text(
                    text = "¡BIENVENIDO A PARDOS!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = terracota,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Crea tu perfil para guardar tus rachas y competir con amigos.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = cafeSuave,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- AVATAR CON SOMBRA Y BORDE ---
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.size(110.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, cremaFondo)
                    ) {
                        Image(
                            painter = painterResource(id = getAvatarResource(avatarSeleccionado)),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .padding(12.dp)
                                .clip(CircleShape)
                                .clickable { mostrarSelectorAvatar = true }
                        )
                    }

                    // Botón flotante estilizado
                    Surface(
                        onClick = { mostrarSelectorAvatar = true },
                        color = cafeOscuro,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✏️", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- INPUT AESTHETIC ---
                Text(
                    text = "¿CÓMO TE LLAMAS?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = cafeSuave,
                    modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    value = nombreUsuario,
                    onValueChange = { if (it.length <= 15) nombreUsuario = it },
                    placeholder = { Text("Nombre de jugador...", color = cafeSuave.copy(alpha = 0.5f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cafeOscuro,
                        unfocusedBorderColor = cafeSuave.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = cafeOscuro
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- BOTÓN PRINCIPAL ---
                Button(
                    onClick = { onProfileSaved(nombreUsuario, avatarSeleccionado) },
                    enabled = nombreUsuario.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cafeOscuro,
                        disabledContainerColor = cafeSuave.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        "COMENZAR AVENTURA ZEN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    if (mostrarSelectorAvatar) {
        AvatarSelectorDialog(
            currentAvatarId = avatarSeleccionado,
            onAvatarSelected = { nuevoId ->
                avatarSeleccionado = nuevoId
                mostrarSelectorAvatar = false
            },
            onDismissRequest = { mostrarSelectorAvatar = false }
        )
    }
}