package com.korkoor.pardos.ui.menu

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.korkoor.pardos.data.local.MissionManager
import com.korkoor.pardos.data.local.ProfileManager
import com.korkoor.pardos.ui.game.menu.PicnicBackgroundOptimized
import com.korkoor.pardos.ui.theme.ThemeViewModel
import com.korkoor.pardos.ui.theme.GameTheme
import com.korkoor.pardos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onCustomClick: () -> Unit,
    onRecordsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onDailyChallengeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // --- INSTANCIAS DE MANAGERS (Para Misiones y Perfil) ---
    val missionManager = remember { MissionManager(context) }
    val profileManager = remember { ProfileManager(context) }

    val currentTheme = themeViewModel.currentTheme
    val bgColor = currentTheme.colors.first().copy(alpha = 0.98f)
    val textColor = currentTheme.mainTextColor
    val prefs = context.getSharedPreferences("pardos_prefs", android.content.Context.MODE_PRIVATE)

// Estados para mostrar los diálogos
    var showPrivacyDisclaimer by remember { mutableStateOf(!prefs.getBoolean("privacy_accepted", false)) }
    var showTutorial by remember { mutableStateOf(false) }

// 1. Mostrar el Privacy Disclaimer si no ha aceptado
    if (showPrivacyDisclaimer) {
        PrivacyDisclaimerDialog(
            onAccept = {
                prefs.edit().putBoolean("privacy_accepted", true).apply()
                showPrivacyDisclaimer = false
                // Opcional: Mostrar tutorial justo después de aceptar términos
                showTutorial = true
            }
        )
    }

// 2. Mostrar el tutorial
    if (showTutorial) {
        TutorialDialog(onDismiss = { showTutorial = false })
    }
    var showSupportDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Fondo de patrón picnic aesthetic
        PicnicBackgroundOptimized(currentTheme.accentColor.copy(alpha = 0.06f))

        if (isLandscape) {
            // --- DISEÑO HORIZONTAL (LANDSCAPE) ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 40.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lado Izquierdo: Branding y Soporte
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedTitle("PARDOS", textColor, fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.menu_slogan),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor.copy(alpha = 0.4f),
                        letterSpacing = 4.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    SupportHeartButton(currentTheme.accentColor) { showSupportDialog = true }
                }

                // Lado Derecho: Panel de botones con Scroll
                Column(
                    modifier = Modifier
                        .weight(1.5f)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DailyChallengeButton(onDailyChallengeClick, modifier = Modifier.fillMaxWidth().height(60.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AestheticMenuButton(
                            text = stringResource(R.string.menu_play),
                            color = currentTheme.accentColor,
                            onClick = onPlayClick,
                            modifier = Modifier.weight(1f).height(60.dp)
                        )
                        AestheticMenuButton(
                            text = stringResource(R.string.menu_customize),
                            color = Color(0xFF81B29A),
                            onClick = onCustomClick,
                            modifier = Modifier.weight(1f).height(60.dp)
                        )
                    }

                    // Tarjeta de Misiones
                    DailyMissionsCard(missionManager, profileManager)

                    // Cuadrícula Social y Progreso
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniMenuButton("PERFIL", Color(0xFF457B9D), Modifier.weight(1f), onProfileClick)
                        MiniMenuButton("AMIGOS", Color(0xFF2A9D8F), Modifier.weight(1f), onFriendsClick)
                        MiniMenuButton(stringResource(R.string.menu_records), Color(0xFFE07A5F), Modifier.weight(1f), onRecordsClick)
                        MiniMenuButton(stringResource(R.string.menu_achievements), Color(0xFF6C63FF), Modifier.weight(1f), onAchievementsClick)
                    }
                }
            }
        } else {
            // --- DISEÑO VERTICAL (PORTRAIT) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // 🔥 SCROLL PARA QUE NO SE CORTE NADA
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 1. BRANDING
                AnimatedTitle("PARDOS", textColor)
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(currentTheme.accentColor, CircleShape)
                )
                Text(
                    text = stringResource(R.string.menu_slogan),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor.copy(alpha = 0.4f),
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                )

                // 2. ACCIONES PRINCIPALES
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AestheticMenuButton(
                        text = stringResource(R.string.menu_play),
                        color = currentTheme.accentColor,
                        onClick = onPlayClick
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AestheticMenuButton(
                            text = stringResource(R.string.menu_customize),
                            color = Color(0xFF81B29A),
                            onClick = onCustomClick,
                            modifier = Modifier.weight(1f).height(64.dp),
                            fontSize = 12.sp
                        )
                        DailyChallengeButton(
                            onClick = onDailyChallengeClick,
                            modifier = Modifier.weight(1f).height(64.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. MISIONES DIARIAS (Centro de Retención)
                DailyMissionsCard(missionManager = missionManager, profileManager = profileManager)

                Spacer(modifier = Modifier.height(24.dp))

                // 4. ZONA SOCIAL Y PROGRESO (Grid 2x2 Aesthetic)
                Column(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniMenuButton(
                            text = "MI PERFIL",
                            color = Color(0xFF457B9D), // Azul sereno
                            modifier = Modifier.weight(1f),
                            onClick = onProfileClick
                        )
                        MiniMenuButton(
                            text = "AMIGOS",
                            color = Color(0xFF2A9D8F), // Verde agua
                            modifier = Modifier.weight(1f),
                            onClick = onFriendsClick
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniMenuButton(
                            text = stringResource(R.string.menu_records),
                            color = Color(0xFFE07A5F), // Terracota
                            modifier = Modifier.weight(1f),
                            onClick = onRecordsClick
                        )
                        MiniMenuButton(
                            text = stringResource(R.string.menu_achievements),
                            color = Color(0xFF6C63FF), // Morado
                            modifier = Modifier.weight(1f),
                            onClick = onAchievementsClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 5. FOOTER
                SupportHeartButton(currentTheme.accentColor) { showSupportDialog = true }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.menu_version_info),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor.copy(alpha = 0.3f),
                    letterSpacing = 2.sp
                )
            }
        }

        // --- DIÁLOGO DE APOYO ---
        if (showSupportDialog) {
            AlertDialog(
                onDismissRequest = { showSupportDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                content = {
                    SupportCreatorContent(
                        currentTheme = currentTheme,
                        onDismiss = { showSupportDialog = false }
                    )
                }
            )
        }
    }
}

// ============================================================================
// COMPONENTES DEL DIÁLOGO DE APOYO
// ============================================================================

@Composable
fun SupportCreatorContent(currentTheme: GameTheme, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White.copy(alpha = 0.98f),
        shadowElevation = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HECHO CON ❤️ POR KOR (CARLOS) :)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = currentTheme.accentColor
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "ParDos es un proyecto independiente creado para relajarte. Si te gusta, considera apoyarme para seguir mejorando el juego sin publicidad intrusiva.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = Color(0xFF3D405B).copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { uriHandler.openUri("https://www.instagram.com/kourkoour/") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C))
            ) {
                Text("SÍGUEME EN INSTAGRAM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { uriHandler.openUri("https://ko-fi.com/korkor0209") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
            ) {
                Text("INVÍTAME UN CAFÉ ☕", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("CERRAR", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SupportHeartButton(color: Color, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartPulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Surface(
            modifier = Modifier.size(44.dp).scale(scale),
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Apoyar",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "APOYAR",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = color.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
    }
}

// ============================================================================
// COMPONENTES DE BOTONES (Ajustados para diseño Responsivo)
// ============================================================================

@Composable
fun AestheticMenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(0.92f).height(74.dp),
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp // Permitimos ajustar la fuente dinámicamente
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "Scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = color.copy(alpha = 0.5f)
            )
            .background(
                brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF9FAFB))),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text.uppercase(),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = 2.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DailyChallengeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(0.92f).height(74.dp),
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp
) {
    Button(
        onClick = onClick,
        modifier = modifier.shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D405B)),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF2CC8F), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.menu_daily_challenge).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontSize = fontSize,
            maxLines = 1
        )
    }
}

@Composable
fun MiniMenuButton(
    text: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        border = BorderStroke(2.dp, color.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color.White, color.copy(alpha = 0.05f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnimatedTitle(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 56.sp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            val infiniteTransition = rememberInfiniteTransition(label = "Title$index")
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = index * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Bounce"
            )

            Text(
                text = char.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = color,
                modifier = Modifier
                    .offset(y = yOffset.dp)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun PrivacyDisclaimerDialog(
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* No se puede quitar sin aceptar */ },
        containerColor = Color(0xFFFDF8F1), // fondoBeige
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = Color(0xFFE07A5F))
                Spacer(modifier = Modifier.width(8.dp))
                Text("El Acuerdo de Armonía", fontWeight = FontWeight.Black, color = Color(0xFF5D4037))
            }
        },
        text = {
            Column {
                Text(
                    text = "Para asegurar que tu progreso en ParDos sea eterno, guardamos algunos datos en la nube (Firebase).",
                    fontSize = 14.sp,
                    color = Color(0xFF8D6E63)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Lista de datos
                val items = listOf(
                    "Identificador anónimo de tu dispositivo.",
                    "Tu apodo Zen y Avatar elegido.",
                    "Tu nivel, XP y récords de la vitrina."
                )
                items.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF81B29A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item, fontSize = 13.sp, color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No recopilamos correos, contraseñas ni ubicación. ¡Tu paz mental es nuestra prioridad!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE07A5F)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81B29A))
            ) {
                Text("ACEPTAR Y JUGAR", fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    )
}
@Composable
fun TutorialDialog(
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    val totalSteps = 3
    val (title, description, icon, color) = when(step) {
        1 -> listOf("CONECTA NÚMEROS", "Desliza para sumar fichas del mismo valor y alcanzar la meta del nivel.", Icons.Rounded.TouchApp, Color(0xFF81B29A))
        2 -> listOf("CUIDA EL TIEMPO", "En el modo desafío, el reloj es tu único enemigo. ¡Piensa rápido!", Icons.Rounded.Timer, Color(0xFFE07A5F))
        else -> listOf("USA TU PODER", "Si te quedas atascado, usa los Power-Ups en la parte inferior para despejar el tablero.", Icons.Rounded.Bolt, Color(0xFFF2CC8F))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = (color as Color).copy(alpha = 0.15f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null, tint = color, modifier = Modifier.padding(20.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(title as String, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF5D4037))
                Spacer(Modifier.height(8.dp))
                Text(description as String, textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Gray)

                Spacer(Modifier.height(24.dp))
                // Indicadores de paso (Puntitos)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..totalSteps) {
                        Box(modifier = Modifier
                            .size(if (i == step) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (i == step) color else Color.LightGray)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step < totalSteps) step++ else onDismiss()
                }
            ) {
                Text(if (step < totalSteps) "SIGUIENTE" else "¡ENTENDIDO!", fontWeight = FontWeight.Black, color = color as Color)
            }
        }
    )
}