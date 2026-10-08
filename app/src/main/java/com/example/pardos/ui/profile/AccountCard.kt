package com.korkoor.pardos.ui.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.data.auth.AuthManager
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.ui.design.*
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Cuenta de Google: guarda el progreso en la nube de forma segura y activa amigos y ranking.
 * [onChanged] se llama tras iniciar o cerrar sesión (o restaurar progreso) para refrescar la pantalla.
 */
@Composable
fun AccountCard(modifier: Modifier = Modifier, onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { AuthManager(context) }
    val profileManager = remember { ProfileManager(context) }
    val user by auth.userFlow().collectAsState(initial = auth.currentUser)

    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    PardosCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.Large), elevation = 6.dp) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (user == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.CloudSync, GemBlue, size = 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Guarda tu progreso", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                        Text(
                            "Entra con Google para recuperar tu partida en otro teléfono y jugar con amigos.",
                            fontSize = 12.sp, color = InkSecondary, lineHeight = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(Radius.Medium))
                        .background(Navy)
                        .clickable(enabled = !busy) {
                            val activity = context.findActivity() ?: return@clickable
                            busy = true
                            message = null
                            scope.launch {
                                when (val result = auth.signInWithGoogle(activity)) {
                                    AuthManager.Result.Success -> {
                                        profileManager.syncAfterSignIn { restored ->
                                            busy = false
                                            message = if (restored) "¡Progreso restaurado!" else "Cuenta conectada"
                                            onChanged()
                                        }
                                    }
                                    AuthManager.Result.Cancelled -> busy = false
                                    AuthManager.Result.NoAccount -> {
                                        busy = false
                                        message = "Agrega una cuenta de Google en los ajustes del teléfono."
                                    }
                                    is AuthManager.Result.Error -> {
                                        busy = false
                                        message = "No se pudo iniciar sesión: ${result.message}"
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (busy) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("CONTINUAR CON GOOGLE", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.CloudDone, Sage, size = 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cuenta conectada", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Navy)
                        Text(
                            user?.email ?: user?.displayName ?: "Google",
                            fontSize = 12.sp, color = InkSecondary, maxLines = 1
                        )
                    }
                    Text(
                        "SALIR",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    auth.signOut()
                                    message = null
                                    onChanged()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Black, color = Terracotta, letterSpacing = 1.5.sp
                    )
                }
            }
            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sage, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
