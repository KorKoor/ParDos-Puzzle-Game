package com.korkoor.pardos.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.ProfileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {} // Para que el botón de regresar funcione
) {
    // 1. CONEXIÓN REAL A TUS DATOS (El cerebro)
    val context = LocalContext.current
    val profileManager = remember { ProfileManager(context) }

    // Estado reactivo: Si profile cambia, la pantalla se redibuja sola
    var profile by remember { mutableStateOf(profileManager.getProfile()) }

    // Controles de ventanas emergentes
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    // Paleta Aesthetic (Combinando con tu Menú)
    val bgColor = Color(0xFFF9FAFB) // Blanco hueso muy suave
    val textColor = Color(0xFF3D405B) // Azul marino oscuro / Gris
    val accentColor = Color(0xFFE07A5F) // Terracota suave
    val greenAccent = Color(0xFF81B29A) // Verde agua

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 24.dp)
            .systemBarsPadding(), // Respeta la barra de estado del celular
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- BARRA SUPERIOR (Botón Atrás) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Regresar", tint = textColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "MI PERFIL",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TARJETA PRINCIPAL (Avatar, Nombre y XP) ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // AVATAR
                Box(contentAlignment = Alignment.BottomEnd) {
                    Image(
                        painter = painterResource(id = getAvatarResource(profile.avatarId)),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable { showAvatarDialog = true }
                    )
                    // Botoncito de editar
                    Surface(
                        shape = CircleShape,
                        color = accentColor,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(36.dp)
                            .offset(x = 8.dp, y = 8.dp)
                            .clickable { showAvatarDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // NOMBRE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showNameDialog = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = profile.name, fontSize = 26.sp, fontWeight = FontWeight.Black, color = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BARRA DE EXPERIENCIA AESTHETIC
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nivel ${profile.playerLevel}", fontWeight = FontWeight.Bold, color = greenAccent)
                        Text("${profile.currentXp} / 100 XP", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = profile.currentXp.toFloat() / 100f, // Asumiendo 100 como meta base
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        color = greenAccent,
                        trackColor = bgColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TARJETAS DE RACHAS (Estilo Menú) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AestheticStatCard(
                title = "RACHA ACTUAL",
                value = "${profile.currentStreak} 🔥",
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            AestheticStatCard(
                title = "MEJOR RACHA",
                value = "${profile.bestStreak} 🏆",
                color = Color(0xFFF2CC8F), // Amarillo suave
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN DE AMIGOS ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "MIS AMIGOS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            // Aquí en el futuro cargaremos los de Firebase. Por ahora un estado vacío estético.
            Surface(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aún no tienes amigos agregados.\n¡Invítalos pronto!",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // --- DIÁLOGOS (FUNCIONALES) ---

    if (showAvatarDialog) {
        AvatarSelectorDialog(
            currentAvatarId = profile.avatarId,
            onAvatarSelected = { nuevoId ->
                // Guardamos en local
                val nuevoPerfil = profile.copy(avatarId = nuevoId)
                profileManager.saveProfile(nuevoPerfil)
                // Actualizamos la pantalla
                profile = nuevoPerfil
                showAvatarDialog = false
            },
            onDismissRequest = { showAvatarDialog = false }
        )
    }

    if (showNameDialog) {
        var tempName by remember { mutableStateOf(profile.name) }

        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Tu Nombre Zen", color = textColor, fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { if (it.length <= 15) tempName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        cursorColor = accentColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nuevoPerfil = profile.copy(name = tempName)
                        profileManager.saveProfile(nuevoPerfil)
                        profile = nuevoPerfil
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

// --- COMPONENTE EXTRA PARA LAS RACHAS ESTILO AESTHETIC ---
@Composable
fun AestheticStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, color.copy(alpha = 0.05f)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            }
        }
    }
}