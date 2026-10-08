package com.korkoor.pardos.ui.profile

import com.korkoor.pardos.ui.design.*

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.local.LocalDay
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.domain.model.UserProfile
import com.korkoor.pardos.domain.social.Leaderboard
import com.korkoor.pardos.domain.social.RankEntry
import com.korkoor.pardos.domain.social.RankedPlayer
import com.korkoor.pardos.domain.social.WeekCalendar
import kotlinx.coroutines.launch


@Composable
fun FriendsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val profileManager = remember { ProfileManager(context) }

    var me by remember { mutableStateOf(profileManager.getProfile()) }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var codeInput by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }

    fun reload() {
        loading = true
        me = profileManager.getProfile()
        profileManager.getFriendsProfiles {
            friends = it
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    val today = LocalDay.today()
    val week = WeekCalendar.weekId(today)
    // La semana termina el domingo: días que faltan para el próximo lunes
    val daysLeft = ((week + 1) * 7 - 3) - today

    // Cuenta con sesión: código propio de 8 caracteres. Invitado: el código legacy (prefijo del id del dispositivo)
    val myCode = if (me.friendCode.isNotBlank()) me.friendCode else if (me.uid.length > 8) me.uid.take(8).uppercase() else me.uid.uppercase()

    val ranking: List<RankedPlayer> = remember(me, friends, week) {
        val entries = buildList {
            add(RankEntry(me.uid, me.name, me.weeklyStars, me.weekId, isMe = true))
            friends.forEach { add(RankEntry(it.uid, it.name, it.weeklyStars, it.weekId)) }
        }
        Leaderboard.weekly(entries, week)
    }
    val profilesByUid = remember(me, friends) { (friends + me).associateBy { it.uid } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF3EFE6), Cream)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Barra superior ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(onClick = onBack, shape = CircleShape, color = Color.White, shadowElevation = 6.dp, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Navy)
                }
            }
            Spacer(Modifier.width(14.dp))
            Text("AMIGOS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Navy, letterSpacing = 3.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (me.friendCode.isBlank()) {
                item { AccountCard(onChanged = { reload() }) }
            }

            // --- Mi código + invitar ---
            item {
                Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("TU CÓDIGO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.45f), letterSpacing = 3.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                myCode,
                                fontSize = 30.sp, fontWeight = FontWeight.Black, color = Navy,
                                letterSpacing = 6.sp, modifier = Modifier.weight(1f)
                            )
                            SmallRoundButton(Icons.Rounded.ContentCopy, Navy.copy(alpha = 0.08f), Navy) {
                                if (me.uid.isNotEmpty()) {
                                    clipboard.setText(AnnotatedString(me.uid))
                                    Toast.makeText(context, "¡Código copiado!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            SmallRoundButton(Icons.Rounded.Share, Sage, Color.White) {
                                if (me.uid.isNotEmpty()) shareMyCode(context, me.uid, myCode)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        // Campo para agregar amigo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Navy.copy(alpha = 0.05f))
                                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (codeInput.isEmpty()) {
                                    Text("Código de tu amigo", fontSize = 14.sp, color = Navy.copy(alpha = 0.35f))
                                }
                                BasicTextField(
                                    value = codeInput,
                                    onValueChange = { if (it.length <= 40) codeInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy, letterSpacing = 2.sp),
                                    cursorBrush = SolidColor(Navy),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Surface(
                                onClick = {
                                    val code = codeInput.trim()
                                    if (code.isNotBlank() && !adding) {
                                        adding = true
                                        profileManager.addFriendByCode(
                                            friendUid = code,
                                            onSuccess = { name ->
                                                scope.launch {
                                                    adding = false
                                                    codeInput = ""
                                                    Toast.makeText(context, "¡$name ahora es tu amigo!", Toast.LENGTH_SHORT).show()
                                                    reload()
                                                }
                                            },
                                            onError = { msg ->
                                                scope.launch {
                                                    adding = false
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = Sage,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (adding) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                    else Icon(Icons.Rounded.PersonAdd, contentDescription = "Agregar", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // --- Título del ranking ---
            item {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("RANKING SEMANAL", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Navy.copy(alpha = 0.55f), letterSpacing = 3.sp)
                        Text(
                            if (daysLeft <= 0) "Termina hoy" else if (daysLeft == 1) "Termina mañana" else "Termina en $daysLeft días",
                            fontSize = 11.sp, color = Navy.copy(alpha = 0.4f)
                        )
                    }
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("estrellas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy.copy(alpha = 0.55f))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (loading && friends.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Sage, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    }
                }
            }

            itemsIndexed(ranking, key = { _, p -> p.entry.uid.ifEmpty { "me" } }) { _, player ->
                RankRow(player, profilesByUid[player.entry.uid])
            }

            if (!loading && friends.isEmpty()) {
                item {
                    EmptyFriends(onInvite = { if (me.uid.isNotEmpty()) shareMyCode(context, me.uid, myCode) })
                }
            }
        }
    }
}

@Composable
private fun SmallRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, tint: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = bg, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RankRow(player: RankedPlayer, profile: UserProfile?) {
    val medal = when (player.position) {
        1 -> Gold
        2 -> Color(0xFF9AA3B2)
        3 -> Color(0xFFC98B5E)
        else -> null
    }
    val isMe = player.entry.isMe
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isMe) Sage.copy(alpha = 0.10f) else Color.White)
            .then(if (isMe) Modifier.border(1.5.dp, Sage.copy(alpha = 0.6f), shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Posición
        Box(
            modifier = Modifier.size(32.dp).background((medal ?: Navy).copy(alpha = if (medal != null) 0.18f else 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (player.position == 1 && player.score > 0) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
            } else {
                Text("${player.position}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = medal ?: Navy.copy(alpha = 0.5f))
            }
        }
        Spacer(Modifier.width(12.dp))
        Image(
            painter = painterResource(getAvatarResource(profile?.avatarId ?: 1)),
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF5F5F5))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) "${player.entry.name} (tú)" else player.entry.name,
                fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NV ${profile?.playerLevel ?: 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Sage)
                val streak = profile?.currentStreak ?: 0
                if (streak > 0) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Terracotta, modifier = Modifier.size(13.dp))
                    Text("$streak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Terracotta)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFC83D), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(3.dp))
            Text("${player.score}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
        }
    }
}

@Composable
private fun EmptyFriends(onInvite: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(Sage.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = Sage, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Juega con amigos", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Navy)
            Text(
                "Invítalos con tu código y compitan cada semana por ver quién gana más estrellas.",
                fontSize = 12.sp, color = Navy.copy(alpha = 0.55f), textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp), lineHeight = 17.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7FB69C), Color(0xFF5A8C74))))
                    .clickable(onClick = onInvite)
                    .padding(horizontal = 26.dp, vertical = 13.dp)
            ) {
                Text("INVITAR AMIGOS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
            }
        }
    }
}

fun shareMyCode(context: Context, uid: String, shortCode: String = uid.take(8).uppercase()) {
    val text = "¡Juega conmigo en ParDos! Agrégame con mi código $shortCode y compitamos por el ranking de la semana."
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Invitar a un amigo"))
}
