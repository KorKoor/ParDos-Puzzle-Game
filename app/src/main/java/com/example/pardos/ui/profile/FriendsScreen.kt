package com.korkoor.pardos.ui.profile

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.domain.model.UserProfile
import kotlinx.coroutines.launch // 🔥 IMPORTANTE PARA LOS HILOS DE UI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val profileManager = remember { ProfileManager(context) }

    // 🔥 Permite ejecutar código en el Hilo Principal de UI (evita crasheos)
    val coroutineScope = rememberCoroutineScope()

    // --- COLORES CAFÉ PASTEL & BEIGE ---
    val beigeFondo = Color(0xFFFDF8F1)
    val cremaCard = Color(0xFFFFFFFF)
    val cafeOscuro = Color(0xFF5D4037)
    val cafeSuave = Color(0xFF8D6E63)
    val beigeBoton = Color(0xFFD7CCC8)
    val verdeZen = Color(0xFF81B29A)

    val profile by remember { mutableStateOf(profileManager.getProfile()) }
    val myUid = profile.uid
    val displayCode = if (myUid.length > 8) myUid.take(8).uppercase() else myUid

    var friendCodeInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var friendsList by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    // Cargar amigos al iniciar la pantalla
    LaunchedEffect(Unit) {
        profileManager.getFriendsProfiles { perfiles ->
            friendsList = perfiles
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(beigeFondo)
            .padding(horizontal = 24.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABECERA ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = cremaCard,
                shadowElevation = 4.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Regresar", tint = cafeOscuro)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "COMUNIDAD ZEN",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = cafeOscuro,
                letterSpacing = 2.sp
            )
        }

        // --- TARJETA: MI CÓDIGO ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = cremaCard,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, beigeBoton.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(cremaCard, beigeFondo.copy(alpha = 0.3f))))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "TU CÓDIGO DE JUGADOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = cafeSuave,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(beigeFondo, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (myUid.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(myUid))
                                    Toast.makeText(context, "¡Código copiado!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayCode,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = cafeOscuro,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Rounded.ContentCopy, null, tint = cafeSuave, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        onClick = { if (myUid.isNotEmpty()) shareMyCode(context, myUid) },
                        shape = CircleShape,
                        color = cafeOscuro,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Share, contentDescription = "Compartir", tint = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- BUSCADOR AESTHETIC ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = friendCodeInput,
                onValueChange = { if (it.length <= 30) friendCodeInput = it },
                placeholder = { Text("Pega el código de tu amigo...", color = cafeSuave.copy(alpha = 0.6f)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cafeOscuro,
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = cremaCard,
                    focusedContainerColor = cremaCard,
                    cursorColor = cafeOscuro
                ),
                modifier = Modifier
                    .weight(1f)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    val uidToSearch = friendCodeInput.trim()
                    if (uidToSearch.isNotBlank()) {
                        isSearching = true
                        profileManager.addFriendByCode(
                            friendUid = uidToSearch,
                            onSuccess = { name ->
                                // 🔥 Regresamos al hilo principal para tocar la UI y mostrar Toasts
                                coroutineScope.launch {
                                    isSearching = false
                                    Toast.makeText(context, "¡$name agregado a tu lista!", Toast.LENGTH_SHORT).show()

                                    // Chequeo de insignia
                                    val sharedPrefs = context.getSharedPreferences("pardos_profile", Context.MODE_PRIVATE)
                                    val wasSocialUnlocked = sharedPrefs.getBoolean("badge_social_unlocked", false)

                                    if (!wasSocialUnlocked) {
                                        Toast.makeText(context, "🏅 ¡Desbloqueaste la insignia SOCIAL!", Toast.LENGTH_LONG).show()
                                        sharedPrefs.edit().putBoolean("badge_social_unlocked", true).apply()
                                    }

                                    friendCodeInput = ""
                                    // 🔥 Refrescamos la lista de amigos para que aparezca inmediatamente
                                    profileManager.getFriendsProfiles { perfiles ->
                                        friendsList = perfiles
                                    }
                                }
                            },
                            onError = { error ->
                                // 🔥 Aseguramos que los errores también se muestren en el hilo principal
                                coroutineScope.launch {
                                    isSearching = false
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(22.dp),
                enabled = !isSearching,
                colors = ButtonDefaults.buttonColors(containerColor = verdeZen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isSearching) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Icon(Icons.Rounded.Add, contentDescription = "Agregar", modifier = Modifier.size(32.dp), tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- TÍTULO LISTA ---
        Text(
            text = "TU CÍRCULO ZEN (${friendsList.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = cafeSuave,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 16.dp)
        )

        // --- LISTA DE AMIGOS ---
        if (friendsList.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(32.dp),
                color = cremaCard.copy(alpha = 0.6f),
                border = BorderStroke(2.dp, beigeBoton.copy(alpha = 0.4f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(text = "☕", fontSize = 40.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = "Aún no hay nadie aquí.\n¡Comparte tu código para empezar!",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = cafeSuave,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = friendsList,
                    key = { it.uid } // Optimización importante
                ) { friend ->
                    FriendCardZen(friend, cafeOscuro, cafeSuave)
                }
            }
        }
    }
}

@Composable
fun FriendCardZen(friend: UserProfile, textColor: Color, subTextColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFEFEBE9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
            ) {
                Image(
                    painter = painterResource(id = getAvatarResource(friend.avatarId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = friend.name, fontSize = 17.sp, fontWeight = FontWeight.Black, color = textColor)
                Text(text = "Racha: ${friend.currentStreak} 🔥", fontSize = 12.sp, color = subTextColor, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = Color(0xFFFDF8F1),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NIVEL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = subTextColor)
                    Text(text = "${friend.playerLevel}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = textColor)
                }
            }
        }
    }
}

fun shareMyCode(context: Context, uid: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "¡Relájate conmigo en ParDos: Zen Math! ☕ Agregame con mi código de amigo: $uid")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Compartir con un amigo")
    context.startActivity(shareIntent)
}