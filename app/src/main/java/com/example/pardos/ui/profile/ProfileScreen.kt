package com.korkoor.pardos.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.domain.model.UserProfile
import com.korkoor.pardos.domain.model.Record
import com.korkoor.pardos.ui.game.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    gameViewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val profileManager = remember { ProfileManager(context) }

    var profile by remember { mutableStateOf(profileManager.getProfile()) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    // ESTADOS PARA LA VITRINA DE RÉCORDS
    val savedRecords by gameViewModel.allRecords.collectAsState(initial = emptyList())
    var showRecordSelector by remember { mutableStateOf(false) }
    var activeSlot by remember { mutableIntStateOf(0) }

    // ESTADO PARA EL RANKING
    var fullRanking by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    // Cargar amigos y armar el ranking al entrar
    LaunchedEffect(Unit) {
        profileManager.getFriendsProfiles { amigos ->
            val todos = amigos.toMutableList()
            todos.add(profile)
            val ordenados = todos.sortedWith(compareByDescending<UserProfile> { it.playerLevel }.thenByDescending { it.currentXp })
            fullRanking = ordenados
        }
    }

    // --- PALETA ZEN ---
    val fondoBeige = Color(0xFFFDF8F1)
    val cafeProfundo = Color(0xFF5D4037)
    val cafeSuave = Color(0xFF8D6E63)
    val cremaPuro = Color(0xFFFFFFFF)
    val terracota = Color(0xFFE07A5F)
    val verdeMenta = Color(0xFF81B29A)
    val dorado = Color(0xFFF2CC8F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoBeige)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(cafeSuave.copy(alpha = 0.15f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = cremaPuro,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = cafeProfundo)
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Text("TU ESPACIO ZEN", fontSize = 20.sp, fontWeight = FontWeight.Black, color = cafeProfundo, letterSpacing = 1.5.sp)
            }

            // --- PERFIL CARD PRINCIPAL ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(36.dp),
                color = cremaPuro,
                shadowElevation = 20.dp,
                border = BorderStroke(1.dp, cafeSuave.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = fondoBeige,
                            border = BorderStroke(4.dp, Brush.sweepGradient(listOf(terracota, verdeMenta, terracota)))
                        ) {
                            Image(
                                painter = painterResource(id = getAvatarResource(profile.avatarId)),
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp).clip(CircleShape).clickable { showAvatarDialog = true }
                            )
                        }
                        Surface(
                            shape = CircleShape, color = cafeProfundo, modifier = Modifier.size(38.dp).offset(x = 4.dp, y = 4.dp).clickable { showAvatarDialog = true }, shadowElevation = 4.dp
                        ) {
                            Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Nombre
                    Surface(
                        onClick = { showNameDialog = true },
                        color = cafeSuave.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.name, fontSize = 28.sp, fontWeight = FontWeight.Black, color = cafeProfundo)
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(Icons.Rounded.Edit, null, tint = cafeSuave, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Barra de XP
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text("Nivel ${profile.playerLevel}", fontWeight = FontWeight.Black, color = verdeMenta, fontSize = 16.sp)
                            Text("${profile.currentXp} / ${profile.xpToNextLevel} XP", fontWeight = FontWeight.Bold, color = cafeSuave, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = if (profile.xpToNextLevel > 0) profile.currentXp.toFloat() / profile.xpToNextLevel.toFloat() else 0f,
                            modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(20.dp)),
                            color = verdeMenta, trackColor = fondoBeige
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔥 NUEVO: SECCIÓN DE ESTADÍSTICAS (RACHAS) 🔥
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Whatshot, // Fuego
                    title = "RACHA ACTUAL",
                    value = "${profile.currentStreak} días",
                    color = terracota
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.EmojiEvents, // Copa/Trofeo
                    title = "MEJOR RACHA",
                    value = "${profile.bestStreak} días",
                    color = dorado
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN DE INSIGNIAS ---
            val prefs = context.getSharedPreferences("pardos_profile", android.content.Context.MODE_PRIVATE)
            val influencerUnlocked = prefs.getBoolean("badge_influencer_unlocked", false)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                BadgeItem(Icons.Rounded.Group, "Social", profile.friendsUids.isNotEmpty(), terracota)
                BadgeItem(Icons.Rounded.LocalFireDepartment, "Constante", profile.currentStreak >= 5, dorado)
                BadgeItem(Icons.Rounded.RecordVoiceOver, "Influencer", influencerUnlocked, Color(0xFF6C63FF))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🔥 SECCIÓN: VITRINA DE GLORIA 🔥
            Text(
                text = "VITRINA DE GLORIA",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = cafeSuave,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                profile.pinnedRecords.forEachIndexed { index, record ->
                    RecordSlot(
                        recordText = record,
                        color = when(index) {
                            0 -> terracota
                            1 -> verdeMenta
                            else -> Color(0xFF6C63FF)
                        },
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activeSlot = index
                            showRecordSelector = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🔥 SECCIÓN: RANKING ZEN 🔥
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PODIO DE AMIGOS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = cafeSuave,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Icon(Icons.Rounded.Leaderboard, null, tint = terracota)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (fullRanking.size <= 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = cremaPuro,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🥇 Estás en 1er lugar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cafeProfundo)
                        Spacer(Modifier.height(8.dp))
                        Text("¡Invita amigos con tu código para que empiece la competencia!", textAlign = TextAlign.Center, fontSize = 12.sp, color = cafeSuave)
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = cremaPuro,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, cafeSuave.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        fullRanking.take(5).forEachIndexed { index, user ->
                            RankingRow(
                                rank = index + 1,
                                user = user,
                                isMe = user.uid == profile.uid,
                                cafeProfundo = cafeProfundo,
                                cafeSuave = cafeSuave
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS ---
    if (showAvatarDialog) {
        AvatarSelectorDialog(
            currentAvatarId = profile.avatarId,
            onAvatarSelected = { nuevoId ->
                val profileConId = profileManager.getProfile().copy(avatarId = nuevoId)
                profileManager.saveProfile(profileConId)
                profile = profileConId
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
            title = { Text("Tu Identidad Zen", fontWeight = FontWeight.Black, color = Color(0xFF5D4037)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { if (it.length <= 15) tempName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val profileConId = profileManager.getProfile().copy(name = tempName)
                    profileManager.saveProfile(profileConId)
                    profile = profileConId
                    showNameDialog = false
                }) { Text("GUARDAR", fontWeight = FontWeight.Black, color = Color(0xFFE07A5F)) }
            }
        )
    }

    // DIÁLOGO SELECTOR DE RÉCORDS PARA VITRINA
    if (showRecordSelector) {
        AlertDialog(
            onDismissRequest = { showRecordSelector = false },
            containerColor = Color.White,
            title = { Text("Elegir para Vitrina", fontWeight = FontWeight.Black) },
            text = {
                if (savedRecords.isEmpty()) {
                    Text("Aún no tienes récords guardados.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(savedRecords) { rec ->
                            ListItem(
                                headlineContent = {
                                    Text("${rec.score} PTS", fontWeight = FontWeight.Bold)
                                },
                                supportingContent = {
                                    Text("Modo ${rec.mode}")
                                },
                                modifier = Modifier.clickable {
                                    val formattedText = "${rec.score} PTS\n${rec.mode}"
                                    profileManager.pinRecordToSlot(activeSlot, formattedText)
                                    profile = profileManager.getProfile()
                                    showRecordSelector = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecordSelector = false }) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// 🔥 COMPONENTE DE ESTADÍSTICAS (RACHA)
@Composable
fun StatCard(modifier: Modifier, icon: ImageVector, title: String, value: String, color: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF5D4037))
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
        }
    }
}

// COMPONENTE DE LA FILA DEL RANKING
@Composable
fun RankingRow(rank: Int, user: UserProfile, isMe: Boolean, cafeProfundo: Color, cafeSuave: Color) {
    val bgColor = if (isMe) Color(0xFF81B29A).copy(alpha = 0.1f) else Color.Transparent
    val borderColor = if (isMe) Color(0xFF81B29A) else Color.Transparent

    val (medalIcon, medalTint) = when (rank) {
        1 -> Icons.Rounded.EmojiEvents to Color(0xFFFFD700) // Oro
        2 -> Icons.Rounded.EmojiEvents to Color(0xFFC0C0C0) // Plata
        3 -> Icons.Rounded.EmojiEvents to Color(0xFFCD7F32) // Bronce
        else -> Icons.Rounded.StarBorder to cafeSuave.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número / Medalla
            Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                if (rank <= 3) {
                    Icon(medalIcon, contentDescription = null, tint = medalTint, modifier = Modifier.size(24.dp))
                } else {
                    Text("$rank", fontSize = 16.sp, fontWeight = FontWeight.Black, color = cafeSuave)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Avatar en pequeño
            Image(
                painter = painterResource(id = getAvatarResource(user.avatarId)),
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF5F5F5))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Nombre y (Tú)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cafeProfundo, maxLines = 1)
                    if (isMe) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = Color(0xFF81B29A), shape = RoundedCornerShape(8.dp)) {
                            Text("TÚ", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("${user.currentXp} XP", fontSize = 10.sp, color = cafeSuave, fontWeight = FontWeight.Medium)
            }

            // Nivel
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NIVEL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = cafeSuave)
                Text("${user.playerLevel}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = cafeProfundo)
            }
        }
    }
}

@Composable
fun BadgeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, unlocked: Boolean, activeColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = if (unlocked) activeColor.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.1f),
            border = BorderStroke(2.dp, if (unlocked) activeColor else Color.LightGray.copy(alpha = 0.3f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (unlocked) activeColor else Color.LightGray,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = if (unlocked) Color.DarkGray else Color.LightGray,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun RecordSlot(
    recordText: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(
            2.dp,
            if (recordText.isEmpty()) Color.LightGray.copy(alpha = 0.3f) else color.copy(alpha = 0.4f)
        ),
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            if (recordText.isEmpty()) {
                Icon(Icons.Rounded.Add, null, tint = Color.LightGray.copy(alpha = 0.6f))
            } else {
                Text(
                    text = recordText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}