package com.korkoor.pardos.ui.profile

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ProfileSetupDialog(
    onProfileSaved: (String, Int) -> Unit // Devuelve el Nombre y el ID del Avatar al guardar
) {
    val cafeOscuro = Color(0xFF5D4037)
    val cremaFondo = Color(0xFFF5F5DC)

    var nombreUsuario by remember { mutableStateOf("") }
    var avatarSeleccionado by remember { mutableStateOf(1) } // Avatar 1 por defecto
    var mostrarSelectorAvatar by remember { mutableStateOf(false) }

    // Dialog que NO se puede cerrar tocando fuera ni con el botón de "Atrás"
    Dialog(
        onDismissRequest = { /* No hacemos nada para forzar el registro */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cremaFondo),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¡Vas excelente! 🚀",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = cafeOscuro,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Crea tu perfil de jugador para guardar tu progreso, rachas y competir con amigos.",
                    fontSize = 14.sp,
                    color = cafeOscuro,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- SELECTOR DE AVATAR ---
                Box(contentAlignment = Alignment.BottomEnd) {
                    Image(
                        painter = painterResource(id = getAvatarResource(avatarSeleccionado)),
                        contentDescription = "Tu Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { mostrarSelectorAvatar = true }
                    )
                    SmallFloatingActionButton(
                        onClick = { mostrarSelectorAvatar = true },
                        containerColor = cafeOscuro,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✏️", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- CAMPO DE TEXTO PARA EL NOMBRE ---
                OutlinedTextField(
                    value = nombreUsuario,
                    onValueChange = { siTipea ->
                        if (siTipea.length <= 15) nombreUsuario = siTipea
                    },
                    label = { Text("¿Cómo te llamas?", color = cafeOscuro) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cafeOscuro,
                        cursorColor = cafeOscuro
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- BOTÓN DE GUARDAR ---
                Button(
                    onClick = { onProfileSaved(nombreUsuario, avatarSeleccionado) },
                    enabled = nombreUsuario.isNotBlank(), // Se activa solo si escribió algo
                    colors = ButtonDefaults.buttonColors(containerColor = cafeOscuro),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Guardar y Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Si toca el avatar, mostramos el componente que creamos antes
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