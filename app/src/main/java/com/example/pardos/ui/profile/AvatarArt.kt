package com.korkoor.pardos.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.korkoor.pardos.domain.shop.Accessory
import com.korkoor.pardos.domain.shop.AnimalKind
import com.korkoor.pardos.domain.shop.AvatarDef
import com.korkoor.pardos.domain.shop.AvatarFrame
import com.korkoor.pardos.domain.shop.AvatarScene
import com.korkoor.pardos.domain.shop.AvatarVariant
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.ui.design.LocalCozyClock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Avatar de un jugador. Los ids 1..10 son las imágenes clásicas; del 11 en adelante son animalitos dibujados
 * a mano (con parpadeo suave). Úsalo en lugar de `painterResource(getAvatarResource(...))`.
 */
@Composable
fun AvatarImage(avatarId: Int, modifier: Modifier = Modifier, animate: Boolean = true) {
    val def = Avatars.byId(avatarId)
    if (def.animal == null) {
        Image(painter = painterResource(getAvatarResource(def.id)), contentDescription = def.name, modifier = modifier)
    } else {
        DrawnAvatar(def, modifier.clip(CircleShape).semantics { contentDescription = def.name }, animate)
    }
}

@Composable
private fun DrawnAvatar(def: AvatarDef, modifier: Modifier, animate: Boolean) {
    val clock = LocalCozyClock.current
    Canvas(modifier) {
        val u = size.minDimension / 100f
        val t = if (animate) (clock.value + def.id * 0.137f) % 1f else 0f
        val blink = animate && t in 0.90f..0.96f
        scale(u, u, pivot = Offset.Zero) { drawAvatar(def, blink, clock.value) }
    }
}

// ------------------------------------------------------------------ paletas

private class Look(
    val head: Color, val light: Color, val dark: Color, val inner: Color,
    val bg1: Color, val bg2: Color, val shirt: Color
)

private fun look(def: AvatarDef): Look {
    val a = def.animal ?: AnimalKind.CAT
    val base = when (a) {
        AnimalKind.FOX -> Look(Color(0xFFF29A4A), Color(0xFFFFF2DC), Color(0xFF5C3A21), Color(0xFFF7C79A), Color(0xFFFFE6C9), Color(0xFFFFCB9A), Color(0xFF6B9E86))
        AnimalKind.CAT -> Look(Color(0xFFF4C48E), Color(0xFFFFF0DC), Color(0xFF8A5A2B), Color(0xFFFFB3C1), Color(0xFFE3EEFF), Color(0xFFC9DCFF), Color(0xFFE5576B))
        AnimalKind.PANDA -> Look(Color(0xFFFFFFFF), Color(0xFFF1F1F4), Color(0xFF34353F), Color(0xFFFFB3C1), Color(0xFFD9F2E3), Color(0xFFB5E3C8), Color(0xFF6B9E86))
        AnimalKind.BUNNY -> Look(Color(0xFFF8F0F8), Color(0xFFFFFFFF), Color(0xFF8A6A7A), Color(0xFFFFB3C9), Color(0xFFFFE0EE), Color(0xFFFFC4DD), Color(0xFF8E7CC3))
        AnimalKind.BEAR -> Look(Color(0xFFBE8A52), Color(0xFFF1D3A8), Color(0xFF4F3320), Color(0xFFE2B98C), Color(0xFFF6E3CC), Color(0xFFEBC9A0), Color(0xFFE07A5F))
        AnimalKind.FROG -> Look(Color(0xFF7FCB7A), Color(0xFFF1F8D8), Color(0xFF2F6B3A), Color(0xFFB8E5A0), Color(0xFFD9F5CF), Color(0xFFB2E8A2), Color(0xFFE0A93B))
        AnimalKind.OWL -> Look(Color(0xFFB58B5C), Color(0xFFF1DDB8), Color(0xFF4A3322), Color(0xFFD9B88A), Color(0xFFE7DDF5), Color(0xFFCDBBEB), Color(0xFF6C63FF))
        AnimalKind.PENGUIN -> Look(Color(0xFF3E4A6B), Color(0xFFFFFFFF), Color(0xFF232B45), Color(0xFFF2A93B), Color(0xFFD6ECFA), Color(0xFFB0DAF2), Color(0xFFE5576B))
        AnimalKind.KOALA -> Look(Color(0xFFA9AFBF), Color(0xFFE9ECF3), Color(0xFF3B3B4A), Color(0xFFD7DBE6), Color(0xFFE5E8F5), Color(0xFFCDD3EB), Color(0xFF6B9E86))
        AnimalKind.RACCOON -> Look(Color(0xFFA9A9B8), Color(0xFFF4F4F8), Color(0xFF3F4050), Color(0xFFD0D0DC), Color(0xFFE8E0F0), Color(0xFFD3C5E6), Color(0xFFE0A93B))
        AnimalKind.CHICK -> Look(Color(0xFFFFD84A), Color(0xFFFFF0A8), Color(0xFFB87510), Color(0xFFF29A2E), Color(0xFFFFF3C4), Color(0xFFFFE28A), Color(0xFFE07A5F))
        AnimalKind.UNICORN -> Look(Color(0xFFFFF0F6), Color(0xFFFFFFFF), Color(0xFF8A5A8E), Color(0xFFFFB3D1), Color(0xFFF0E0FF), Color(0xFFD9C2FF), Color(0xFF8E7CC3))
        AnimalKind.DRAGON -> Look(Color(0xFF6CC59A), Color(0xFFE8F7D9), Color(0xFF2A6B50), Color(0xFFF2D58A), Color(0xFFD6F5E8), Color(0xFFA8E6CC), Color(0xFFE0A93B))
        AnimalKind.AXOLOTL -> Look(Color(0xFFFFB7C9), Color(0xFFFFE3EA), Color(0xFF8A4A63), Color(0xFFFF7FA3), Color(0xFFD8F1FF), Color(0xFFB5E3FA), Color(0xFF6C63FF))
        AnimalKind.CAPYBARA -> Look(Color(0xFFB98A5E), Color(0xFFE6CBA3), Color(0xFF5A3D24), Color(0xFF8A6240), Color(0xFFE3F2D9), Color(0xFFCBE6BA), Color(0xFF6B9E86))
        AnimalKind.TIGER -> Look(Color(0xFFF5A03C), Color(0xFFFFF0DA), Color(0xFF3A2414), Color(0xFFFFC38A), Color(0xFFFFEBC9), Color(0xFFFFD08A), Color(0xFF3E7C5A))
        AnimalKind.LION -> Look(Color(0xFFF2C36B), Color(0xFFFFF0C8), Color(0xFF8A4A16), Color(0xFFE8A24A), Color(0xFFFFF0CC), Color(0xFFFFD98A), Color(0xFFB8473A))
        AnimalKind.WOLF -> Look(Color(0xFF9AA3B5), Color(0xFFE9EDF5), Color(0xFF2E3446), Color(0xFFCDD3E0), Color(0xFFE0E6F5), Color(0xFFBFCBE8), Color(0xFF455A9E))
        AnimalKind.SHEEP -> Look(Color(0xFFF3D9C2), Color(0xFFFFF7EC), Color(0xFF5A4636), Color(0xFFFFB3C1), Color(0xFFE6F3FF), Color(0xFFCDE5FA), Color(0xFFE5576B))
        AnimalKind.HEDGEHOG -> Look(Color(0xFFE9C9A0), Color(0xFFFFF0DC), Color(0xFF6B4A33), Color(0xFFFFB3C1), Color(0xFFF3E6D2), Color(0xFFE6CFAE), Color(0xFF6B9E86))
        AnimalKind.TURTLE -> Look(Color(0xFF8ED08A), Color(0xFFF2F8D8), Color(0xFF2F6B3A), Color(0xFFB8E5A0), Color(0xFFD6F3F5), Color(0xFFA8E0E8), Color(0xFFE0A93B))
        AnimalKind.PIG -> Look(Color(0xFFF7B6C6), Color(0xFFFFE3EA), Color(0xFF8A3E55), Color(0xFFF29BB0), Color(0xFFFFE9EE), Color(0xFFFFCCDA), Color(0xFF6B9E86))
        AnimalKind.MONKEY -> Look(Color(0xFFB5794A), Color(0xFFF4D9B5), Color(0xFF4F3320), Color(0xFFE8B98A), Color(0xFFE6F3D2), Color(0xFFCBE6A8), Color(0xFFE5576B))
        AnimalKind.HAMSTER -> Look(Color(0xFFF2C48A), Color(0xFFFFF3E0), Color(0xFF8A5A2B), Color(0xFFFFB3C1), Color(0xFFFFF0D6), Color(0xFFFFDDA8), Color(0xFF6C63FF))
        AnimalKind.DEER -> Look(Color(0xFFC98E5A), Color(0xFFF6E6CF), Color(0xFF5A3A22), Color(0xFFF1C9A0), Color(0xFFE7F2D8), Color(0xFFCDE6B0), Color(0xFFB8473A))
        AnimalKind.COW -> Look(Color(0xFFFFFFFF), Color(0xFFF4F4F8), Color(0xFF2E2E3A), Color(0xFFFFB3C1), Color(0xFFDFF0D2), Color(0xFFBFE3A8), Color(0xFF3E7CD9))
        AnimalKind.DUCK -> Look(Color(0xFFFFD84A), Color(0xFFFFF0A8), Color(0xFFB87510), Color(0xFFF29A2E), Color(0xFFD6EEFF), Color(0xFFA8D8F5), Color(0xFF6B9E86))
        AnimalKind.BAT -> Look(Color(0xFF6A5A8E), Color(0xFFA394C8), Color(0xFF2A2145), Color(0xFFC9A6E8), Color(0xFF3B2563), Color(0xFF241447), Color(0xFF3A2A5E))
        AnimalKind.GHOST -> Look(Color(0xFFF6F6FF), Color(0xFFE2E4F8), Color(0xFF3A3F66), Color(0xFFC9CCF0), Color(0xFF3F3470), Color(0xFF241A4E), Color(0xFFB8BCE8))
        AnimalKind.PUMPKIN -> Look(Color(0xFFF28A1F), Color(0xFFFFC76B), Color(0xFF4A2208), Color(0xFFFFD34A), Color(0xFF3A1F5C), Color(0xFF1E1136), Color(0xFF3E7C3A))
        AnimalKind.ROBOT -> Look(Color(0xFFBBC6D8), Color(0xFFE9EFF8), Color(0xFF3A4560), Color(0xFF5CC8FF), Color(0xFFD8E4F5), Color(0xFFB0C4E2), Color(0xFF6C63FF))
        AnimalKind.ALIEN -> Look(Color(0xFF8EDC8A), Color(0xFFC8F5C4), Color(0xFF2A6B3E), Color(0xFFFFE066), Color(0xFF2A1F5C), Color(0xFF14103A), Color(0xFF8E7CC3))
        AnimalKind.DINO -> Look(Color(0xFF9ACD5A), Color(0xFFEAF5C4), Color(0xFF3F6B22), Color(0xFFF2A93B), Color(0xFFE2F5CF), Color(0xFFBFE39A), Color(0xFFE07A5F))
        AnimalKind.PHOENIX -> Look(Color(0xFFF2762E), Color(0xFFFFD08A), Color(0xFF5A1D0F), Color(0xFFFFB02E), Color(0xFF3A140E), Color(0xFF8A2A10), Color(0xFFB8321F))
    }
    return when (def.variant) {
        AvatarVariant.NORMAL -> base
        AvatarVariant.GOLD -> Look(Color(0xFFF6C844), Color(0xFFFFF1B5), Color(0xFF9A5F0C), Color(0xFFFFE08A), Color(0xFFFFF1C0), Color(0xFFFFD36E), Color(0xFFB8473A))
        AvatarVariant.MIDNIGHT -> Look(
            lerp(base.head, Color(0xFF5B5F9E), 0.55f), lerp(base.light, Color(0xFFC9CCF5), 0.6f), Color(0xFF1F2150),
            Color(0xFFE6E8FF), Color(0xFF2A2E63), Color(0xFF454B94), Color(0xFF6C63FF)
        )
        AvatarVariant.JADE -> Look(Color(0xFF4FBF92), Color(0xFFC6F0DC), Color(0xFF1F7A5A), Color(0xFFFFD36E), Color(0xFFD2F2E4), Color(0xFF8ED8B8), Color(0xFFD9A21B))
        AvatarVariant.RAINBOW -> Look(Color(0xFFFFF0F6), Color(0xFFFFFFFF), Color(0xFF8A5A8E), Color(0xFFFFB3D1), Color(0xFFFFE0F0), Color(0xFFD0E4FF), Color(0xFFB57CF0))
        AvatarVariant.SAKURA -> Look(Color(0xFFFFD1DC), Color(0xFFFFF0F4), Color(0xFF8A3E55), Color(0xFFFF9EBB), Color(0xFFFFE3EE), Color(0xFFFFC4D8), Color(0xFFF29BB0))
        AvatarVariant.FROST -> Look(Color(0xFFDDEBFA), Color(0xFFFFFFFF), Color(0xFF3F6A9E), Color(0xFFA8D4F5), Color(0xFFD6EEFF), Color(0xFFA8D2F5), Color(0xFF5D9BD9))
        AvatarVariant.EMBER -> Look(Color(0xFFFF7A3A), Color(0xFFFFD08A), Color(0xFF5A1D0F), Color(0xFFFFB347), Color(0xFF3A140E), Color(0xFF8A2A10), Color(0xFF2B1010))
        AvatarVariant.SHADOW -> Look(Color(0xFF5B5470), Color(0xFF9A92B5), Color(0xFF17132B), Color(0xFFC9A6E8), Color(0xFF1B1030), Color(0xFF3A1A5E), Color(0xFF2A1F45))
        AvatarVariant.CANDY -> Look(Color(0xFFFFC1E3), Color(0xFFFFF0FA), Color(0xFF8A3E70), Color(0xFF9BE3D0), Color(0xFFFFE6F5), Color(0xFFD9F5EA), Color(0xFFB57CF0))
        AvatarVariant.GALAXY -> Look(Color(0xFF6A4FD6), Color(0xFFC9B8FF), Color(0xFF1B1250), Color(0xFFFFD36E), Color(0xFF12093A), Color(0xFF3A1A8F), Color(0xFF2A1F7A))
        AvatarVariant.PLATINUM -> Look(Color(0xFFE4ECFA), Color(0xFFFFFFFF), Color(0xFF4A5B8A), Color(0xFFBBCBF2), Color(0xFFDDE7FA), Color(0xFFB5C7EE), Color(0xFF8EA0D0))
    }
}

// ------------------------------------------------------------------ dibujo

private fun DrawScope.tri(a: Offset, b: Offset, c: Offset, color: Color) {
    drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }, color)
}

private fun DrawScope.avatarStar(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = (-90.0 + i * 36.0) * PI / 180.0
        val x = cx + (rr * cos(a)).toFloat(); val y = cy + (rr * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
    drawPath(p, color, style = Stroke(2f, join = StrokeJoin.Round))
}

private fun DrawScope.drawAvatar(def: AvatarDef, blink: Boolean, time: Float = 0f) {
    val a = def.animal ?: return
    val l = look(def)
    val night = def.variant == AvatarVariant.MIDNIGHT

    // ---- fondo
    drawRect(Brush.verticalGradient(listOf(l.bg1, l.bg2)), size = Size(100f, 100f))
    if (def.scene != AvatarScene.AUTO) {
        drawScene(def.scene, l, time, def.id)
    } else {
        if (night) {
            listOf(Offset(14f, 14f) to 1.8f, Offset(86f, 22f) to 1.4f, Offset(78f, 8f) to 1.1f, Offset(10f, 44f) to 1.2f, Offset(92f, 54f) to 1.5f)
                .forEach { (p, r) -> drawCircle(Color.White.copy(alpha = 0.85f), r, p) }
        }
        if (def.variant == AvatarVariant.GOLD) {
            listOf(Offset(14f, 18f), Offset(88f, 30f), Offset(84f, 8f)).forEach { drawCircle(Color.White.copy(alpha = 0.8f), 2f, it) }
        }
    }

    // ---- hombros / ropa
    drawOval(l.shirt, Offset(10f, 82f), Size(80f, 44f))
    drawOval(Color.White.copy(alpha = 0.18f), Offset(22f, 84f), Size(30f, 10f))

    // ---- orejas y partes que van detrás de la cabeza
    val earInk = l.dark
    when (a) {
        AnimalKind.FOX -> {
            tri(Offset(18f, 42f), Offset(26f, 6f), Offset(46f, 32f), l.head); tri(Offset(82f, 42f), Offset(74f, 6f), Offset(54f, 32f), l.head)
            tri(Offset(24f, 34f), Offset(27f, 14f), Offset(38f, 31f), earInk); tri(Offset(76f, 34f), Offset(73f, 14f), Offset(62f, 31f), earInk)
        }
        AnimalKind.CAT -> {
            tri(Offset(18f, 44f), Offset(24f, 8f), Offset(46f, 32f), l.head); tri(Offset(82f, 44f), Offset(76f, 8f), Offset(54f, 32f), l.head)
            tri(Offset(24f, 36f), Offset(26f, 16f), Offset(38f, 32f), l.inner); tri(Offset(76f, 36f), Offset(74f, 16f), Offset(62f, 32f), l.inner)
        }
        AnimalKind.PANDA -> {
            drawCircle(l.dark, 12f, Offset(24f, 34f)); drawCircle(l.dark, 12f, Offset(76f, 34f))
        }
        AnimalKind.BUNNY -> {
            rotate(-9f, pivot = Offset(33f, 38f)) {
                drawOval(l.head, Offset(24f, -4f), Size(18f, 46f)); drawOval(l.inner, Offset(28f, 2f), Size(10f, 32f))
            }
            rotate(9f, pivot = Offset(67f, 38f)) {
                drawOval(l.head, Offset(58f, -4f), Size(18f, 46f)); drawOval(l.inner, Offset(62f, 2f), Size(10f, 32f))
            }
        }
        AnimalKind.BEAR -> {
            drawCircle(l.head, 12f, Offset(24f, 34f)); drawCircle(l.inner, 6.5f, Offset(24f, 35f))
            drawCircle(l.head, 12f, Offset(76f, 34f)); drawCircle(l.inner, 6.5f, Offset(76f, 35f))
        }
        AnimalKind.KOALA -> {
            drawCircle(l.head, 16f, Offset(19f, 38f)); drawCircle(l.inner, 9.5f, Offset(19f, 39f))
            drawCircle(l.head, 16f, Offset(81f, 38f)); drawCircle(l.inner, 9.5f, Offset(81f, 39f))
        }
        AnimalKind.RACCOON -> {
            drawCircle(l.head, 10f, Offset(24f, 33f)); drawCircle(l.dark, 5.5f, Offset(24f, 34f))
            drawCircle(l.head, 10f, Offset(76f, 33f)); drawCircle(l.dark, 5.5f, Offset(76f, 34f))
        }
        AnimalKind.OWL -> {
            tri(Offset(20f, 42f), Offset(22f, 10f), Offset(44f, 32f), l.head); tri(Offset(80f, 42f), Offset(78f, 10f), Offset(56f, 32f), l.head)
        }
        AnimalKind.UNICORN -> {
            // melena de colores a un lado
            listOf(Color(0xFFFF9EC4), Color(0xFFC59BFF), Color(0xFF8EC9FF), Color(0xFFFFD36E)).forEachIndexed { i, c ->
                drawOval(c, Offset(6f + i * 2f, 30f + i * 9f), Size(26f - i * 2f, 26f))
            }
            tri(Offset(24f, 40f), Offset(26f, 14f), Offset(42f, 32f), l.head); tri(Offset(76f, 40f), Offset(74f, 14f), Offset(58f, 32f), l.head)
            tri(Offset(28f, 34f), Offset(29f, 20f), Offset(38f, 31f), l.inner); tri(Offset(72f, 34f), Offset(71f, 20f), Offset(62f, 31f), l.inner)
        }
        AnimalKind.DRAGON -> {
            tri(Offset(28f, 34f), Offset(20f, 6f), Offset(42f, 28f), l.inner); tri(Offset(72f, 34f), Offset(80f, 6f), Offset(58f, 28f), l.inner)
            // púas de la cabeza
            for (k in -1..1) tri(Offset(50f + k * 11f - 5f, 32f), Offset(50f + k * 11f, 16f - (if (k == 0) 4f else 0f)), Offset(50f + k * 11f + 5f, 32f), lerp(l.head, l.dark, 0.35f))
        }
        AnimalKind.AXOLOTL -> {
            // branquias plumosas a cada lado
            for (sd in listOf(-1f, 1f)) {
                val cx = 50f + sd * 33f
                for (k in 0..2) {
                    rotate(sd * (62f - k * 24f), pivot = Offset(cx, 56f)) {
                        drawOval(l.inner, Offset(cx - 3.4f, 34f - k * 1.5f), Size(6.8f, 22f))
                        drawCircle(Color.White.copy(alpha = 0.55f), 2.1f, Offset(cx, 35f - k * 1.5f))
                    }
                }
            }
        }
        AnimalKind.CAPYBARA -> {
            drawCircle(lerp(l.head, l.dark, 0.25f), 7f, Offset(30f, 32f)); drawCircle(l.inner, 3.4f, Offset(30f, 33f))
            drawCircle(lerp(l.head, l.dark, 0.25f), 7f, Offset(70f, 32f)); drawCircle(l.inner, 3.4f, Offset(70f, 33f))
        }
        AnimalKind.TIGER -> {
            drawCircle(l.head, 11f, Offset(25f, 35f)); drawCircle(l.light, 6f, Offset(25f, 36f))
            drawCircle(l.head, 11f, Offset(75f, 35f)); drawCircle(l.light, 6f, Offset(75f, 36f))
        }
        AnimalKind.LION -> {
            val mane = Color(0xFFB5661E)
            for (i in 0 until 14) {
                val ang = (i * 360f / 14f) * PI.toFloat() / 180f
                drawCircle(if (i % 2 == 0) mane else lerp(mane, Color(0xFFD98A33), 0.5f), 12.5f, Offset(50f + cos(ang) * 35f, 58f + sin(ang) * 33f))
            }
            drawCircle(mane, 36f, Offset(50f, 58f))
            drawCircle(l.head, 7f, Offset(30f, 27f)); drawCircle(l.inner, 3.4f, Offset(30f, 28f))
            drawCircle(l.head, 7f, Offset(70f, 27f)); drawCircle(l.inner, 3.4f, Offset(70f, 28f))
        }
        AnimalKind.WOLF -> {
            tri(Offset(18f, 48f), Offset(23f, 3f), Offset(46f, 32f), lerp(l.head, l.dark, 0.35f)); tri(Offset(82f, 48f), Offset(77f, 3f), Offset(54f, 32f), lerp(l.head, l.dark, 0.35f))
            tri(Offset(24f, 38f), Offset(26f, 14f), Offset(38f, 32f), l.inner); tri(Offset(76f, 38f), Offset(74f, 14f), Offset(62f, 32f), l.inner)
        }
        AnimalKind.SHEEP -> {
            drawOval(l.head, Offset(5f, 48f), Size(22f, 11f)); drawOval(l.head, Offset(73f, 48f), Size(22f, 11f))
            listOf(
                Offset(26f, 36f), Offset(40f, 26f), Offset(55f, 24f), Offset(70f, 29f), Offset(80f, 42f),
                Offset(17f, 52f), Offset(85f, 58f), Offset(17f, 68f)
            ).forEach { drawCircle(l.light, 13f, it); drawCircle(l.dark.copy(alpha = 0.06f), 13f, it, style = Stroke(1.2f)) }
        }
        AnimalKind.HEDGEHOG -> {
            val p = Path()
            for (i in 0..20) {
                val ang = PI + i * PI / 20.0
                val r = if (i % 2 == 0) 53f else 41f
                val x = 50f + (r * cos(ang)).toFloat(); val y = 66f + (r * sin(ang)).toFloat()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            drawPath(p, l.dark)
            drawCircle(l.head, 6.5f, Offset(25f, 40f)); drawCircle(l.inner, 3.2f, Offset(25f, 41f))
            drawCircle(l.head, 6.5f, Offset(75f, 40f)); drawCircle(l.inner, 3.2f, Offset(75f, 41f))
        }
        AnimalKind.TURTLE -> {
            drawOval(Color(0xFF6B8E4E), Offset(6f, 20f), Size(88f, 74f))
            drawOval(Color(0xFF86A862), Offset(15f, 26f), Size(70f, 56f))
            drawLine(Color(0xFF4F6E36), Offset(50f, 26f), Offset(50f, 40f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawLine(Color(0xFF4F6E36), Offset(24f, 40f), Offset(36f, 34f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawLine(Color(0xFF4F6E36), Offset(76f, 40f), Offset(64f, 34f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        }
        AnimalKind.PIG -> {
            tri(Offset(20f, 44f), Offset(18f, 14f), Offset(44f, 32f), l.head); tri(Offset(80f, 44f), Offset(82f, 14f), Offset(56f, 32f), l.head)
            tri(Offset(24f, 38f), Offset(23f, 21f), Offset(38f, 32f), l.inner); tri(Offset(76f, 38f), Offset(77f, 21f), Offset(62f, 32f), l.inner)
        }
        AnimalKind.MONKEY -> {
            drawCircle(l.head, 13f, Offset(15f, 58f)); drawCircle(l.light, 8f, Offset(15f, 59f))
            drawCircle(l.head, 13f, Offset(85f, 58f)); drawCircle(l.light, 8f, Offset(85f, 59f))
        }
        AnimalKind.HAMSTER -> {
            drawCircle(l.head, 9.5f, Offset(27f, 33f)); drawCircle(l.inner, 5.4f, Offset(27f, 34f))
            drawCircle(l.head, 9.5f, Offset(73f, 33f)); drawCircle(l.inner, 5.4f, Offset(73f, 34f))
        }
        AnimalKind.DEER -> {
            val ant = Color(0xFF8A5A33)
            for (sd in listOf(-1f, 1f)) {
                drawLine(ant, Offset(50f + sd * 14f, 34f), Offset(50f + sd * 26f, 6f), strokeWidth = 3.4f, cap = StrokeCap.Round)
                drawLine(ant, Offset(50f + sd * 21f, 16f), Offset(50f + sd * 34f, 14f), strokeWidth = 2.8f, cap = StrokeCap.Round)
                drawLine(ant, Offset(50f + sd * 24f, 9f), Offset(50f + sd * 14f, 2f), strokeWidth = 2.4f, cap = StrokeCap.Round)
            }
            rotate(-38f, pivot = Offset(21f, 40f)) { drawOval(l.head, Offset(5f, 34f), Size(22f, 12f)); drawOval(l.inner, Offset(9f, 37f), Size(14f, 6f)) }
            rotate(38f, pivot = Offset(79f, 40f)) { drawOval(l.head, Offset(73f, 34f), Size(22f, 12f)); drawOval(l.inner, Offset(77f, 37f), Size(14f, 6f)) }
        }
        AnimalKind.COW -> {
            rotate(-14f, pivot = Offset(15f, 40f)) { drawOval(l.dark, Offset(2f, 33f), Size(26f, 13f)); drawOval(l.inner, Offset(7f, 36f), Size(15f, 7f)) }
            rotate(14f, pivot = Offset(85f, 40f)) { drawOval(l.dark, Offset(72f, 33f), Size(26f, 13f)); drawOval(l.inner, Offset(78f, 36f), Size(15f, 7f)) }
            tri(Offset(30f, 36f), Offset(25f, 14f), Offset(41f, 31f), Color(0xFFF1E3C4)); tri(Offset(70f, 36f), Offset(75f, 14f), Offset(59f, 31f), Color(0xFFF1E3C4))
        }
        AnimalKind.DUCK -> {
            drawOval(l.head, Offset(42f, 20f), Size(9f, 16f)); drawOval(l.head, Offset(49f, 18f), Size(9f, 17f))
        }
        AnimalKind.BAT -> {
            val wingC = lerp(l.head, l.dark, 0.5f)
            for (sd in listOf(-1f, 1f)) {
                val wing = Path().apply {
                    moveTo(50f + sd * 30f, 62f); lineTo(50f + sd * 55f, 24f); lineTo(50f + sd * 47f, 46f); lineTo(50f + sd * 58f, 54f)
                    lineTo(50f + sd * 45f, 62f); lineTo(50f + sd * 52f, 78f); lineTo(50f + sd * 34f, 70f); close()
                }
                drawPath(wing, wingC)
            }
            tri(Offset(20f, 46f), Offset(23f, 3f), Offset(44f, 32f), l.head); tri(Offset(80f, 46f), Offset(77f, 3f), Offset(56f, 32f), l.head)
            tri(Offset(25f, 38f), Offset(25f, 14f), Offset(38f, 32f), l.inner); tri(Offset(75f, 38f), Offset(75f, 14f), Offset(62f, 32f), l.inner)
        }
        AnimalKind.GHOST -> {
            val sheet = Path().apply {
                moveTo(10f, 62f); quadraticTo(8f, 20f, 50f, 18f); quadraticTo(92f, 20f, 90f, 62f)
                lineTo(92f, 100f); lineTo(78f, 92f); lineTo(64f, 100f); lineTo(50f, 92f); lineTo(36f, 100f); lineTo(22f, 92f); lineTo(8f, 100f); close()
            }
            drawPath(sheet, Brush.verticalGradient(listOf(Color.White, l.light), 18f, 100f))
            drawPath(sheet, l.dark.copy(alpha = 0.14f), style = Stroke(1.2f))
        }
        AnimalKind.PUMPKIN -> {
            drawRoundRect(Color(0xFF6B4A2A), Offset(45f, 14f), Size(10f, 18f), CornerRadius(4f))
            rotate(-28f, pivot = Offset(58f, 24f)) { drawOval(Color(0xFF5CAD4A), Offset(56f, 17f), Size(18f, 8f)) }
        }
        AnimalKind.ROBOT -> {
            drawLine(l.dark, Offset(50f, 32f), Offset(50f, 12f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawCircle(Color(0xFFFF5A5A), 4.6f, Offset(50f, 10f)); drawCircle(Color.White.copy(alpha = 0.7f), 1.4f, Offset(48.6f, 8.8f))
            drawRoundRect(lerp(l.head, l.dark, 0.35f), Offset(7f, 46f), Size(13f, 24f), CornerRadius(5f))
            drawRoundRect(lerp(l.head, l.dark, 0.35f), Offset(80f, 46f), Size(13f, 24f), CornerRadius(5f))
        }
        AnimalKind.ALIEN -> {
            for (sd in listOf(-1f, 1f)) {
                drawLine(l.dark, Offset(50f + sd * 12f, 34f), Offset(50f + sd * 21f, 10f), strokeWidth = 2.6f, cap = StrokeCap.Round)
                drawCircle(if (sd < 0) l.inner else Color(0xFFFF8FC0), 4.8f, Offset(50f + sd * 21f, 9f))
            }
        }
        AnimalKind.DINO -> {
            for (k in -2..2) {
                val x = 50f + k * 12f
                tri(Offset(x - 6.5f, 36f - (if (k == 0) 0f else 1f)), Offset(x, 17f + kotlin.math.abs(k) * 4f), Offset(x + 6.5f, 36f - (if (k == 0) 0f else 1f)), l.inner)
            }
        }
        AnimalKind.PHOENIX -> {
            val flame = listOf(Color(0xFFE5382B), Color(0xFFFF8A1F), Color(0xFFFFD34A))
            for (sd in listOf(-1f, 1f)) {
                rotate(sd * 64f, pivot = Offset(50f + sd * 34f, 72f)) {
                    drawOval(flame[0], Offset(50f + sd * 34f - 9f, 36f), Size(18f, 38f)); drawOval(flame[1], Offset(50f + sd * 34f - 6f, 44f), Size(12f, 26f))
                }
            }
            for (i in 0..2) {
                val sc = 1f - i * 0.25f
                rotate(-28f, pivot = Offset(38f, 40f)) { drawOval(flame[i], Offset(38f - 7f * sc, 40f - 34f * sc), Size(14f * sc, 34f * sc)) }
                rotate(28f, pivot = Offset(62f, 40f)) { drawOval(flame[i], Offset(62f - 7f * sc, 40f - 34f * sc), Size(14f * sc, 34f * sc)) }
                drawOval(flame[i], Offset(50f - 8f * sc, 40f - 40f * sc), Size(16f * sc, 40f * sc))
            }
        }
        AnimalKind.FROG, AnimalKind.PENGUIN, AnimalKind.CHICK -> Unit
    }
    if (a == AnimalKind.FROG) {
        // ojos saltones por encima de la cabeza
        drawCircle(l.head, 13f, Offset(33f, 35f)); drawCircle(l.head, 13f, Offset(67f, 35f))
    }

    // ---- cabeza
    val headRect = Offset(17f, 29f)
    val headSize = Size(66f, 58f)
    val headBrush = Brush.verticalGradient(listOf(lerp(l.head, Color.White, 0.18f), l.head, lerp(l.head, l.dark, 0.12f)), 29f, 87f)
    when (a) {
        AnimalKind.PUMPKIN -> {
            drawOval(headBrush, Offset(8f, 30f), Size(84f, 58f))
            for (dx in listOf(-26f, -13f, 13f, 26f)) {
                drawArc(l.dark.copy(alpha = 0.22f), if (dx < 0) 100f else 280f, 60f, false, Offset(50f + dx - 18f, 30f), Size(36f, 58f), style = Stroke(1.6f, cap = StrokeCap.Round))
            }
            drawOval(l.dark.copy(alpha = 0.18f), Offset(8f, 30f), Size(84f, 58f), style = Stroke(1.4f))
        }
        AnimalKind.ROBOT -> {
            drawRoundRect(headBrush, headRect, headSize, CornerRadius(18f))
            drawRoundRect(l.dark.copy(alpha = 0.3f), headRect, headSize, CornerRadius(18f), style = Stroke(1.8f))
            for (pt in listOf(Offset(24f, 35f), Offset(76f, 35f), Offset(24f, 82f), Offset(76f, 82f))) { drawCircle(l.dark.copy(alpha = 0.45f), 2f, pt); drawCircle(Color.White.copy(alpha = 0.6f), 0.8f, pt - Offset(0.5f, 0.5f)) }
        }
        else -> {
            drawOval(headBrush, headRect, headSize)
            drawOval(l.dark.copy(alpha = 0.16f), headRect, headSize, style = Stroke(1.4f))
        }
    }
    // brillo suave arriba a la izquierda y sombrita bajo la barbilla: da volumen a todos
    drawOval(Color.White.copy(alpha = 0.22f), Offset(27f, 32f), Size(26f, 11f))
    drawOval(l.dark.copy(alpha = 0.07f), Offset(26f, 78f), Size(48f, 11f))

    // ---- marcas por animal (bajo los ojos)
    when (a) {
        AnimalKind.FOX -> {
            drawOval(l.light, Offset(14f, 58f), Size(40f, 28f)); drawOval(l.light, Offset(46f, 58f), Size(40f, 28f))
            drawOval(l.light, Offset(38f, 64f), Size(24f, 20f))
        }
        AnimalKind.CAT -> {
            for (dx in listOf(-7f, 0f, 7f)) drawLine(lerp(l.head, l.dark, 0.35f), Offset(50f + dx, 33f), Offset(50f + dx * 0.9f, 41f), strokeWidth = 2.6f, cap = StrokeCap.Round)
            drawOval(l.light, Offset(36f, 66f), Size(28f, 18f))
        }
        AnimalKind.PANDA -> {
            rotate(22f, pivot = Offset(36f, 57f)) { drawOval(l.dark, Offset(27f, 46f), Size(19f, 24f)) }
            rotate(-22f, pivot = Offset(64f, 57f)) { drawOval(l.dark, Offset(54f, 46f), Size(19f, 24f)) }
        }
        AnimalKind.BUNNY -> drawOval(l.light, Offset(38f, 64f), Size(24f, 18f))
        AnimalKind.BEAR -> drawOval(l.light, Offset(35f, 62f), Size(30f, 23f))
        AnimalKind.KOALA -> Unit
        AnimalKind.RACCOON -> {
            drawOval(l.light, Offset(43f, 30f), Size(14f, 22f))
            drawRoundRect(l.dark, Offset(20f, 50f), Size(60f, 17f), CornerRadius(9f))
            drawOval(l.light, Offset(35f, 64f), Size(30f, 20f))
        }
        AnimalKind.UNICORN -> drawOval(l.light, Offset(36f, 64f), Size(28f, 20f))
        AnimalKind.DRAGON -> drawOval(l.light, Offset(33f, 62f), Size(34f, 24f))
        AnimalKind.PENGUIN -> {
            drawOval(l.light, Offset(22f, 44f), Size(32f, 40f)); drawOval(l.light, Offset(46f, 44f), Size(32f, 40f))
        }
        AnimalKind.OWL -> {
            drawCircle(l.light, 15f, Offset(36f, 56f)); drawCircle(l.light, 15f, Offset(64f, 56f))
            drawCircle(lerp(l.head, l.dark, 0.3f), 15f, Offset(36f, 56f), style = Stroke(2.4f)); drawCircle(lerp(l.head, l.dark, 0.3f), 15f, Offset(64f, 56f), style = Stroke(2.4f))
        }
        AnimalKind.FROG -> drawOval(l.light, Offset(30f, 66f), Size(40f, 17f))
        AnimalKind.AXOLOTL -> drawOval(l.light, Offset(36f, 66f), Size(28f, 16f))
        AnimalKind.CAPYBARA -> drawOval(lerp(l.head, l.light, 0.4f), Offset(30f, 58f), Size(40f, 30f))
        AnimalKind.TIGER -> {
            for (dx in listOf(-8f, 0f, 8f)) drawLine(l.dark, Offset(50f + dx, 31f), Offset(50f + dx * 0.8f, 42f), strokeWidth = 3.2f, cap = StrokeCap.Round)
            for (sd in listOf(-1f, 1f)) for (k in 0..1) drawLine(l.dark, Offset(50f + sd * 33f, 54f + k * 9f), Offset(50f + sd * 21f, 57f + k * 9f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawOval(l.light, Offset(34f, 62f), Size(32f, 23f))
        }
        AnimalKind.LION -> drawOval(l.light, Offset(35f, 62f), Size(30f, 23f))
        AnimalKind.WOLF -> {
            drawOval(l.dark.copy(alpha = 0.35f), Offset(42f, 30f), Size(16f, 14f))
            tri(Offset(17f, 56f), Offset(7f, 67f), Offset(23f, 73f), l.light); tri(Offset(83f, 56f), Offset(93f, 67f), Offset(77f, 73f), l.light)
            drawOval(l.light, Offset(33f, 60f), Size(34f, 27f))
        }
        AnimalKind.SHEEP -> {
            drawCircle(l.light, 7.5f, Offset(41f, 32f)); drawCircle(l.light, 8f, Offset(52f, 30f)); drawCircle(l.light, 7f, Offset(61f, 33f))
        }
        AnimalKind.HEDGEHOG -> drawOval(l.light, Offset(26f, 48f), Size(48f, 40f))
        AnimalKind.TURTLE -> {
            drawCircle(lerp(l.head, l.dark, 0.22f), 3.4f, Offset(29f, 42f)); drawCircle(lerp(l.head, l.dark, 0.22f), 3.4f, Offset(71f, 42f))
            drawOval(l.light, Offset(36f, 66f), Size(28f, 17f))
        }
        AnimalKind.CHICK -> {
            for (k in -1..1) drawOval(l.head, Offset(46f + k * 6f, 20f - (if (k == 0) 3f else 0f)), Size(8f, 14f))
        }
        AnimalKind.PIG -> {
            drawOval(l.inner, Offset(35f, 60f), Size(30f, 21f)); drawOval(l.dark.copy(alpha = 0.65f), Offset(42f, 66f), Size(4.4f, 7f)); drawOval(l.dark.copy(alpha = 0.65f), Offset(53.6f, 66f), Size(4.4f, 7f))
        }
        AnimalKind.MONKEY -> {
            drawOval(l.light, Offset(22f, 46f), Size(30f, 26f)); drawOval(l.light, Offset(48f, 46f), Size(30f, 26f)); drawOval(l.light, Offset(33f, 60f), Size(34f, 26f))
        }
        AnimalKind.HAMSTER -> {
            drawOval(l.light, Offset(13f, 58f), Size(32f, 27f)); drawOval(l.light, Offset(55f, 58f), Size(32f, 27f)); drawOval(l.light, Offset(37f, 62f), Size(26f, 20f))
            drawLine(lerp(l.head, l.dark, 0.35f), Offset(50f, 31f), Offset(50f, 44f), strokeWidth = 4f, cap = StrokeCap.Round)
        }
        AnimalKind.DEER -> {
            listOf(Offset(36f, 40f) to 2.3f, Offset(64f, 40f) to 2.3f, Offset(50f, 36f) to 2.6f, Offset(43f, 44f) to 1.8f, Offset(57f, 44f) to 1.8f).forEach { (pt, r) -> drawCircle(l.light, r, pt) }
            drawOval(l.light, Offset(34f, 62f), Size(32f, 24f))
        }
        AnimalKind.COW -> {
            rotate(-18f, pivot = Offset(28f, 40f)) { drawOval(l.dark, Offset(16f, 32f), Size(26f, 20f)) }
            drawOval(l.dark, Offset(60f, 50f), Size(18f, 18f))
            drawOval(l.inner, Offset(27f, 62f), Size(46f, 25f)); drawOval(l.dark.copy(alpha = 0.5f), Offset(37f, 69f), Size(5f, 7f)); drawOval(l.dark.copy(alpha = 0.5f), Offset(58f, 69f), Size(5f, 7f))
        }
        AnimalKind.DUCK -> {
            drawOval(l.inner, Offset(29f, 62f), Size(42f, 20f)); drawLine(l.dark.copy(alpha = 0.45f), Offset(32f, 72f), Offset(68f, 72f), strokeWidth = 1.2f, cap = StrokeCap.Round)
        }
        AnimalKind.BAT -> drawOval(l.light, Offset(35f, 63f), Size(30f, 20f))
        AnimalKind.GHOST -> Unit
        AnimalKind.PUMPKIN -> Unit
        AnimalKind.ROBOT -> {
            drawRoundRect(Color(0xFF1B2540), Offset(23f, 44f), Size(54f, 38f), CornerRadius(11f))
            drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(26f, 46f), Size(30f, 5f), CornerRadius(2.5f))
        }
        AnimalKind.ALIEN -> drawOval(Color.White.copy(alpha = 0.2f), Offset(40f, 31f), Size(20f, 8f))
        AnimalKind.DINO -> {
            drawOval(l.light, Offset(30f, 60f), Size(40f, 27f))
            listOf(Offset(27f, 42f), Offset(73f, 44f), Offset(35f, 36f)).forEach { drawCircle(lerp(l.head, l.dark, 0.28f), 3.2f, it) }
        }
        AnimalKind.PHOENIX -> drawOval(l.light, Offset(30f, 56f), Size(40f, 31f))
    }

    // ---- mejillas
    drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.5f), 5.6f, Offset(27f, 68f)); drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.5f), 5.6f, Offset(73f, 68f))

    // ---- ojos
    val ink = Color(0xFF2B2B3A)
    fun eye(cx: Float, cy: Float, r: Float, white: Boolean = false) {
        if (white) { drawCircle(Color.White, r * 1.7f, Offset(cx, cy)); drawCircle(ink.copy(alpha = 0.25f), r * 1.7f, Offset(cx, cy), style = Stroke(1.2f)) }
        val h = if (blink) r * 0.18f else r
        drawOval(ink, Offset(cx - r, cy - h), Size(r * 2, h * 2))
        if (!blink) { drawCircle(Color.White, r * 0.38f, Offset(cx - r * 0.3f, cy - r * 0.35f)); drawCircle(Color.White.copy(alpha = 0.7f), r * 0.17f, Offset(cx + r * 0.4f, cy + r * 0.42f)) }
    }
    when (a) {
        AnimalKind.FROG -> { eye(33f, 35f, 4.6f, white = true); eye(67f, 35f, 4.6f, white = true) }
        AnimalKind.OWL -> { eye(36f, 56f, 6.5f, white = true); eye(64f, 56f, 6.5f, white = true) }
        AnimalKind.PANDA -> { eye(36f, 57f, 3.6f, white = true); eye(64f, 57f, 3.6f, white = true) }
        AnimalKind.RACCOON -> { eye(35f, 58f, 3.8f, white = true); eye(65f, 58f, 3.8f, white = true) }
        AnimalKind.PENGUIN -> { eye(38f, 58f, 4f); eye(62f, 58f, 4f) }
        AnimalKind.GHOST -> { eye(36f, 56f, 5.2f); eye(64f, 56f, 5.2f) }
        AnimalKind.BAT -> { eye(37f, 57f, 3.8f, white = true); eye(63f, 57f, 3.8f, white = true) }
        AnimalKind.DINO -> { eye(35f, 53f, 4.2f, white = true); eye(65f, 53f, 4.2f, white = true) }
        AnimalKind.PUMPKIN -> {
            val glow = Color(0xFFFFD34A); val dk = Color(0xFF2B1608)
            for (cx in listOf(37f, 63f)) {
                val h = if (blink) 3f else 12f
                tri(Offset(cx - 8f, 58f), Offset(cx + 8f, 58f), Offset(cx, 58f - h), dk)
                if (!blink) tri(Offset(cx - 4.5f, 56.5f), Offset(cx + 4.5f, 56.5f), Offset(cx, 49f), glow.copy(alpha = 0.85f))
            }
        }
        AnimalKind.ROBOT -> {
            val h = if (blink) 2.4f else 11f
            drawRoundRect(l.inner.copy(alpha = 0.35f), Offset(31f, 52.5f - h / 2f - 2f), Size(16f, h + 4f), CornerRadius(5f))
            drawRoundRect(l.inner, Offset(33f, 54f - h / 2f), Size(12f, h), CornerRadius(3f))
            drawRoundRect(l.inner.copy(alpha = 0.35f), Offset(53f, 52.5f - h / 2f - 2f), Size(16f, h + 4f), CornerRadius(5f))
            drawRoundRect(l.inner, Offset(55f, 54f - h / 2f), Size(12f, h), CornerRadius(3f))
        }
        AnimalKind.ALIEN -> {
            val ek = Color(0xFF14142B); val h = if (blink) 3f else 17f
            rotate(-20f, pivot = Offset(33f, 56f)) { drawOval(ek, Offset(23f, 56f - h / 2f), Size(20f, h)) }
            rotate(20f, pivot = Offset(67f, 56f)) { drawOval(ek, Offset(57f, 56f - h / 2f), Size(20f, h)) }
            if (!blink) { drawCircle(Color.White.copy(alpha = 0.85f), 2.4f, Offset(30f, 52f)); drawCircle(Color.White.copy(alpha = 0.85f), 2.4f, Offset(64f, 52f)) }
        }
        AnimalKind.HAMSTER -> { eye(37f, 56f, 3.7f); eye(63f, 56f, 3.7f) }
        else -> { eye(37f, 57f, 4.4f); eye(63f, 57f, 4.4f) }
    }

    // ---- nariz y boca
    fun smile(cx: Float, cy: Float, w: Float, color: Color = l.dark) {
        drawArc(color, 15f, 150f, false, Offset(cx - w, cy - 3f), Size(w, 7f), style = Stroke(1.9f, cap = StrokeCap.Round))
        drawArc(color, 15f, 150f, false, Offset(cx, cy - 3f), Size(w, 7f), style = Stroke(1.9f, cap = StrokeCap.Round))
    }
    when (a) {
        AnimalKind.FOX, AnimalKind.RACCOON -> { drawOval(Color(0xFF2B2B3A), Offset(45f, 62f), Size(10f, 7.5f)); smile(50f, 71f, 5.5f, Color(0xFF2B2B3A)) }
        AnimalKind.CAT -> { drawOval(l.inner, Offset(46.5f, 63f), Size(7f, 5.5f)); smile(50f, 70f, 5.5f) ; for (s in listOf(-1f, 1f)) for (k in 0..2) drawLine(l.dark.copy(alpha = 0.5f), Offset(50f + s * 15f, 66f + k * 3f), Offset(50f + s * 30f, 62f + k * 6f), strokeWidth = 1.2f, cap = StrokeCap.Round) }
        AnimalKind.PANDA, AnimalKind.BEAR -> { drawOval(Color(0xFF2B2B3A), Offset(45f, 62f), Size(10f, 7f)); smile(50f, 70f, 5.5f, Color(0xFF2B2B3A)) }
        AnimalKind.BUNNY -> { drawOval(l.inner, Offset(46.5f, 63f), Size(7f, 5.5f)); smile(50f, 70f, 5f); drawRoundRect(Color.White, Offset(46.5f, 73f), Size(3.2f, 5f), CornerRadius(1f)); drawRoundRect(Color.White, Offset(50.3f, 73f), Size(3.2f, 5f), CornerRadius(1f)) }
        AnimalKind.KOALA -> drawRoundRect(Color(0xFF3B3B4A), Offset(42f, 58f), Size(16f, 22f), CornerRadius(8f))
        AnimalKind.FROG -> drawArc(l.dark, 15f, 150f, false, Offset(34f, 62f), Size(32f, 14f), style = Stroke(2.2f, cap = StrokeCap.Round))
        AnimalKind.OWL -> tri(Offset(44f, 64f), Offset(56f, 64f), Offset(50f, 76f), Color(0xFFF2A93B))
        AnimalKind.PENGUIN -> tri(Offset(43f, 65f), Offset(57f, 65f), Offset(50f, 74f), Color(0xFFF2A93B))
        AnimalKind.CHICK -> tri(Offset(44f, 63f), Offset(56f, 63f), Offset(50f, 72f), Color(0xFFF29A2E))
        AnimalKind.UNICORN -> { drawCircle(l.inner, 1.6f, Offset(46f, 71f)); drawCircle(l.inner, 1.6f, Offset(54f, 71f)); smile(50f, 74f, 4f) }
        AnimalKind.DRAGON -> { drawCircle(l.dark.copy(alpha = 0.7f), 1.7f, Offset(45f, 70f)); drawCircle(l.dark.copy(alpha = 0.7f), 1.7f, Offset(55f, 70f)); smile(50f, 78f, 6f) }
        AnimalKind.AXOLOTL -> smile(50f, 71f, 7f)
        AnimalKind.CAPYBARA -> { drawOval(Color(0xFF3B2A1E), Offset(43f, 62f), Size(14f, 9f)); drawCircle(Color.White.copy(alpha = 0.35f), 1.3f, Offset(46f, 65f)); smile(50f, 76f, 5f) }
        AnimalKind.TIGER -> { tri(Offset(44f, 62f), Offset(56f, 62f), Offset(50f, 69f), Color(0xFFE5576B)); smile(50f, 72f, 6f); for (sd in listOf(-1f, 1f)) for (k in 0..1) drawLine(l.dark.copy(alpha = 0.45f), Offset(50f + sd * 14f, 68f + k * 3f), Offset(50f + sd * 28f, 65f + k * 6f), strokeWidth = 1.2f, cap = StrokeCap.Round) }
        AnimalKind.LION -> { tri(Offset(44f, 62f), Offset(56f, 62f), Offset(50f, 69f), Color(0xFF5C3A21)); smile(50f, 72f, 6f) }
        AnimalKind.WOLF -> { drawOval(Color(0xFF2B2B3A), Offset(44.5f, 62f), Size(11f, 7.5f)); smile(50f, 73f, 5.5f, Color(0xFF2B2B3A)) }
        AnimalKind.SHEEP -> { drawOval(l.dark, Offset(46f, 63f), Size(8f, 5.5f)); smile(50f, 70f, 4.5f) }
        AnimalKind.HEDGEHOG -> { drawCircle(Color(0xFF2B2B3A), 4.2f, Offset(50f, 66f)); drawCircle(Color.White.copy(alpha = 0.5f), 1.2f, Offset(48.6f, 64.8f)); smile(50f, 74f, 4f) }
        AnimalKind.PIG -> smile(50f, 77f, 5f)
        AnimalKind.MONKEY -> { drawCircle(l.dark.copy(alpha = 0.6f), 1.4f, Offset(46.5f, 66f)); drawCircle(l.dark.copy(alpha = 0.6f), 1.4f, Offset(53.5f, 66f)); smile(50f, 74f, 7f) }
        AnimalKind.HAMSTER -> {
            drawOval(l.inner, Offset(46.5f, 61f), Size(7f, 5.5f)); smile(50f, 68f, 4f)
            drawRoundRect(Color.White, Offset(47.2f, 70.5f), Size(2.7f, 4.6f), CornerRadius(1f)); drawRoundRect(Color.White, Offset(50.1f, 70.5f), Size(2.7f, 4.6f), CornerRadius(1f))
        }
        AnimalKind.DEER -> { drawOval(Color(0xFF2B2B3A), Offset(44f, 63f), Size(12f, 8f)); smile(50f, 77f, 5f) }
        AnimalKind.COW -> smile(50f, 80f, 6f)
        AnimalKind.DUCK -> { drawCircle(l.dark.copy(alpha = 0.6f), 1.2f, Offset(44.5f, 67f)); drawCircle(l.dark.copy(alpha = 0.6f), 1.2f, Offset(55.5f, 67f)) }
        AnimalKind.BAT -> {
            smile(50f, 71f, 6f)
            tri(Offset(44f, 73.5f), Offset(48.4f, 73.5f), Offset(46.2f, 80f), Color.White); tri(Offset(51.6f, 73.5f), Offset(56f, 73.5f), Offset(53.8f, 80f), Color.White)
        }
        AnimalKind.GHOST -> {
            drawOval(l.dark, Offset(44f, 66f), Size(12f, 14f)); drawOval(Color(0xFFFF8FA3).copy(alpha = 0.6f), Offset(46f, 73f), Size(8f, 5f))
        }
        AnimalKind.PUMPKIN -> {
            val mouth = Path().apply {
                moveTo(28f, 70f); lineTo(37f, 65f); lineTo(43f, 73f); lineTo(50f, 66f); lineTo(57f, 73f); lineTo(63f, 65f); lineTo(72f, 70f)
                lineTo(66f, 82f); lineTo(58f, 78f); lineTo(50f, 82f); lineTo(42f, 78f); lineTo(34f, 82f); close()
            }
            drawPath(mouth, Color(0xFF2B1608)); drawPath(mouth, Color(0xFFFFD34A).copy(alpha = 0.55f), style = Stroke(1.2f, join = StrokeJoin.Round))
        }
        AnimalKind.ROBOT -> {
            drawRoundRect(l.inner, Offset(37f, 70f), Size(26f, 4.2f), CornerRadius(2f))
            for (x in listOf(41f, 47f, 53f, 59f)) drawRect(Color(0xFF1B2540), Offset(x - 0.8f, 69.5f), Size(1.6f, 5.2f))
        }
        AnimalKind.ALIEN -> smile(50f, 73f, 5f, Color(0xFF2A6B3E))
        AnimalKind.DINO -> {
            drawCircle(l.dark.copy(alpha = 0.7f), 1.7f, Offset(43f, 66f)); drawCircle(l.dark.copy(alpha = 0.7f), 1.7f, Offset(57f, 66f)); smile(50f, 77f, 7f)
            tri(Offset(40f, 78.5f), Offset(44f, 78.5f), Offset(42f, 83f), Color.White); tri(Offset(56f, 78.5f), Offset(60f, 78.5f), Offset(58f, 83f), Color.White)
        }
        AnimalKind.PHOENIX -> {
            tri(Offset(43f, 62f), Offset(57f, 62f), Offset(50f, 76f), Color(0xFFFFB02E)); drawLine(Color(0xFFB87510), Offset(44f, 65f), Offset(56f, 65f), strokeWidth = 1.3f, cap = StrokeCap.Round)
        }
        AnimalKind.TURTLE -> { drawCircle(l.dark.copy(alpha = 0.7f), 1.5f, Offset(46f, 67f)); drawCircle(l.dark.copy(alpha = 0.7f), 1.5f, Offset(54f, 67f)); smile(50f, 73f, 7f) }
    }
    // cuerno del unicornio (delante de la cabeza)
    if (a == AnimalKind.UNICORN) {
        tri(Offset(43f, 34f), Offset(50f, 2f), Offset(57f, 34f), Color(0xFFFFD36E))
        listOf(12f, 20f, 27f).forEach { y -> drawLine(Color(0xFFE0A93B), Offset(46.5f + (y - 12f) * 0.1f, y + 2f), Offset(53.5f - (y - 12f) * 0.1f, y - 2f), strokeWidth = 1.8f) }
    }

    // ---- accesorios
    when (def.accessory) {
        Accessory.NONE -> Unit
        Accessory.CROWN -> {
            val crown = Path().apply { moveTo(33f, 29f); lineTo(30f, 12f); lineTo(41f, 21f); lineTo(50f, 8f); lineTo(59f, 21f); lineTo(70f, 12f); lineTo(67f, 29f); close() }
            drawPath(crown, Color(0xFFFFD34A)); drawPath(crown, Color(0xFFB87510), style = Stroke(2f, join = StrokeJoin.Round))
            listOf(Offset(30f, 12f), Offset(50f, 8f), Offset(70f, 12f)).forEach { drawCircle(Color(0xFFFF6B8A), 2.8f, it) }
            drawLine(Color(0xFFB87510), Offset(33f, 26f), Offset(67f, 26f), strokeWidth = 1.6f)
        }
        Accessory.HELMET -> {
            drawCircle(Color(0xFFBDE6FF).copy(alpha = 0.28f), 42f, Offset(50f, 56f))
            drawCircle(Color.White.copy(alpha = 0.75f), 42f, Offset(50f, 56f), style = Stroke(3.2f))
            drawArc(Color.White.copy(alpha = 0.6f), 205f, 50f, false, Offset(20f, 26f), Size(46f, 46f), style = Stroke(3f, cap = StrokeCap.Round))
            drawRoundRect(Color(0xFFD7DEE8), Offset(18f, 84f), Size(64f, 8f), CornerRadius(4f))
        }
        Accessory.HEADBAND -> {
            drawRoundRect(Color(0xFFD94F4F), Offset(17f, 38f), Size(66f, 8f), CornerRadius(3f))
            drawCircle(Color.White, 4.2f, Offset(50f, 42f)); drawCircle(Color(0xFFD94F4F), 2.6f, Offset(50f, 42f))
            drawRoundRect(Color(0xFFD94F4F), Offset(80f, 40f), Size(14f, 5f), CornerRadius(2.5f)); rotate(18f, pivot = Offset(82f, 43f)) { drawRoundRect(Color(0xFFD94F4F), Offset(80f, 44f), Size(14f, 5f), CornerRadius(2.5f)) }
        }
        Accessory.MOON -> {
            drawCircle(Color(0xFFFFE08A), 10f, Offset(26f, 20f))
            drawCircle(l.bg1, 8.5f, Offset(30f, 17f))
            avatarStar(78f, 16f, 5f, Color(0xFFFFE08A))
        }
        Accessory.STAR -> avatarStar(50f, 14f, 10f, Color(0xFFFFD34A))
        Accessory.SCARF -> {
            drawRoundRect(Color(0xFFE5576B), Offset(24f, 80f), Size(52f, 11f), CornerRadius(5.5f))
            drawRoundRect(Color(0xFFFFFFFF).copy(alpha = 0.6f), Offset(34f, 80f), Size(5f, 11f), CornerRadius(2f))
            drawRoundRect(Color(0xFFE5576B), Offset(62f, 86f), Size(10f, 18f), CornerRadius(4f))
        }
        Accessory.BOW -> {
            val bc = Color(0xFFFF6B8A); val bx = 72f; val by = 33f
            tri(Offset(bx, by), Offset(bx - 14f, by - 9f), Offset(bx - 14f, by + 9f), bc)
            tri(Offset(bx, by), Offset(bx + 14f, by - 9f), Offset(bx + 14f, by + 9f), bc)
            drawCircle(Color(0xFFE5456B), 4.4f, Offset(bx, by)); drawCircle(Color.White.copy(alpha = 0.5f), 1.4f, Offset(bx - 1f, by - 1.4f))
        }
        Accessory.GLASSES -> {
            val gc = Color(0xFF2B2B3A)
            listOf(37f, 63f).forEach { gx ->
                drawCircle(Color.White.copy(alpha = 0.28f), 10f, Offset(gx, 57f)); drawCircle(gc, 10f, Offset(gx, 57f), style = Stroke(2.4f))
                drawArc(Color.White.copy(alpha = 0.8f), 200f, 50f, false, Offset(gx - 7f, 50f), Size(14f, 14f), style = Stroke(1.4f, cap = StrokeCap.Round))
            }
            drawLine(gc, Offset(47f, 57f), Offset(53f, 57f), strokeWidth = 2.4f)
        }
        Accessory.WIZARD -> {
            val hat = Path().apply { moveTo(24f, 38f); quadraticTo(40f, 30f, 62f, 3f); quadraticTo(70f, 10f, 76f, 38f); close() }
            drawPath(hat, Color(0xFF6C4FD6))
            drawRoundRect(Color(0xFF4F38A8), Offset(17f, 32f), Size(66f, 10f), CornerRadius(5f))
            drawRect(Color(0xFFFFD34A), Offset(31f, 28f), Size(40f, 4.5f))
            avatarStar(52f, 20f, 4.6f, Color(0xFFFFD34A)); drawCircle(Color(0xFFFFD34A), 1.5f, Offset(63f, 24f)); drawCircle(Color(0xFFFFD34A), 1.2f, Offset(44f, 26f))
        }
        Accessory.HEADPHONES -> {
            val hc = Color(0xFF3D405B)
            drawArc(hc, 184f, 172f, false, Offset(15f, 20f), Size(70f, 72f), style = Stroke(5.2f, cap = StrokeCap.Round))
            drawRoundRect(hc, Offset(9f, 46f), Size(12f, 24f), CornerRadius(6f)); drawRoundRect(Color(0xFFE07A5F), Offset(12f, 50f), Size(6f, 16f), CornerRadius(3f))
            drawRoundRect(hc, Offset(79f, 46f), Size(12f, 24f), CornerRadius(6f)); drawRoundRect(Color(0xFFE07A5F), Offset(82f, 50f), Size(6f, 16f), CornerRadius(3f))
        }
        Accessory.FLOWER_CROWN -> {
            val cols = listOf(Color(0xFFFF9EC4), Color.White, Color(0xFFFFB347), Color(0xFFB9A6FF))
            for (i in 0..8) {
                val ang = (198f + i * 18f) * PI.toFloat() / 180f
                val fx = 50f + cos(ang) * 34f; val fy = 62f + sin(ang) * 33f
                drawCircle(Color(0xFF6FBF73), 3.3f, Offset(fx + 3f, fy + 2f))
                drawCircle(cols[i % cols.size], 4.6f, Offset(fx, fy)); drawCircle(Color(0xFFFFD34A), 1.7f, Offset(fx, fy))
            }
        }
        Accessory.WITCH -> {
            drawOval(Color(0xFF2D1B4E), Offset(11f, 30f), Size(78f, 15f))
            val cone = Path().apply { moveTo(27f, 36f); quadraticTo(42f, 22f, 52f, 3f); quadraticTo(66f, 6f, 73f, 36f); close() }
            drawPath(cone, Color(0xFF4A2D82))
            drawPath(Path().apply { moveTo(52f, 3f); quadraticTo(60f, -2f, 66f, 2f); quadraticTo(60f, 4f, 58f, 10f); close() }, Color(0xFF4A2D82))
            drawRect(Color(0xFFFF8A1F), Offset(30f, 28f), Size(42f, 6.5f))
            drawRoundRect(Color(0xFFFFD34A), Offset(45f, 27f), Size(11f, 8.5f), CornerRadius(2f)); drawRoundRect(Color(0xFF4A2D82), Offset(47.5f, 29.5f), Size(6f, 3.5f), CornerRadius(1f))
        }
        Accessory.PIRATE -> {
            val hat = Path().apply { moveTo(13f, 38f); lineTo(30f, 13f); quadraticTo(50f, 3f, 70f, 13f); lineTo(87f, 38f); quadraticTo(50f, 27f, 13f, 38f); close() }
            drawPath(hat, Color(0xFF2B2B3A)); drawPath(hat, Color(0xFFE0A93B), style = Stroke(1.8f, join = StrokeJoin.Round))
            drawCircle(Color.White, 4.6f, Offset(50f, 21f)); drawRect(Color.White, Offset(46.5f, 24f), Size(7f, 3f))
            drawCircle(Color(0xFF2B2B3A), 1f, Offset(48.4f, 20.4f)); drawCircle(Color(0xFF2B2B3A), 1f, Offset(51.6f, 20.4f))
            drawLine(Color.White, Offset(41f, 29f), Offset(59f, 33.5f), strokeWidth = 1.6f, cap = StrokeCap.Round); drawLine(Color.White, Offset(59f, 29f), Offset(41f, 33.5f), strokeWidth = 1.6f, cap = StrokeCap.Round)
        }
        Accessory.CHEF -> {
            listOf(Offset(35f, 21f) to 12f, Offset(50f, 14f) to 13.5f, Offset(65f, 21f) to 12f).forEach { (pt, r) -> drawCircle(Color.White, r, pt); drawCircle(Color(0xFFC9CCDC).copy(alpha = 0.5f), r, pt, style = Stroke(1.2f)) }
            drawRoundRect(Color.White, Offset(31f, 22f), Size(38f, 16f), CornerRadius(3f)); drawRoundRect(Color(0xFFC9CCDC).copy(alpha = 0.6f), Offset(31f, 22f), Size(38f, 16f), CornerRadius(3f), style = Stroke(1.2f))
            drawRect(Color(0xFFE5576B), Offset(31f, 33f), Size(38f, 3.4f))
        }
        Accessory.SANTA -> {
            val hat = Path().apply { moveTo(19f, 38f); quadraticTo(22f, 8f, 56f, 8f); quadraticTo(80f, 12f, 82f, 38f); close() }
            drawPath(hat, Color(0xFFD6322B)); drawRoundRect(Color.White, Offset(14f, 31f), Size(72f, 11f), CornerRadius(5.5f))
            drawCircle(Color.White, 6.4f, Offset(72f, 11f))
        }
        Accessory.DEVIL -> {
            tri(Offset(23f, 42f), Offset(18f, 10f), Offset(41f, 32f), Color(0xFFD9322B)); tri(Offset(77f, 42f), Offset(82f, 10f), Offset(59f, 32f), Color(0xFFD9322B))
            tri(Offset(26f, 36f), Offset(22f, 16f), Offset(37f, 31f), Color(0xFFFF7A6B).copy(alpha = 0.7f)); tri(Offset(74f, 36f), Offset(78f, 16f), Offset(63f, 31f), Color(0xFFFF7A6B).copy(alpha = 0.7f))
        }
        Accessory.HALO -> {
            drawOval(Color(0xFFFFE08A).copy(alpha = 0.3f), Offset(22f, 1f), Size(56f, 17f))
            drawOval(Color(0xFFFFD34A), Offset(28f, 5f), Size(44f, 11f), style = Stroke(3.8f)); drawOval(Color.White.copy(alpha = 0.7f), Offset(28f, 5f), Size(44f, 11f), style = Stroke(1f))
        }
        Accessory.NINJA -> {
            val nk = Color(0xFF2B2B3A)
            drawRoundRect(nk, Offset(16f, 39f), Size(68f, 10f), CornerRadius(4f)); drawRect(Color(0xFFD94F4F), Offset(16f, 44.5f), Size(68f, 2.2f))
            drawCircle(nk, 5f, Offset(87f, 45f)); rotate(22f, pivot = Offset(88f, 46f)) { drawRoundRect(nk, Offset(86f, 46f), Size(12f, 5f), CornerRadius(2.5f)) }
            drawRoundRect(nk, Offset(21f, 66f), Size(58f, 22f), CornerRadius(11f)); drawRect(Color.White.copy(alpha = 0.08f), Offset(27f, 69f), Size(20f, 2.4f))
        }
        Accessory.TOPHAT -> {
            drawRoundRect(Color(0xFF22222E), Offset(19f, 28f), Size(62f, 9f), CornerRadius(4.5f))
            drawRoundRect(Color(0xFF2B2B3A), Offset(30f, 3f), Size(40f, 30f), CornerRadius(4f)); drawRect(Color(0xFFB8473A), Offset(30f, 23f), Size(40f, 6.5f))
            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(33f, 6f), Size(5f, 18f), CornerRadius(2.5f))
        }
        Accessory.CAP -> {
            val dome = Path().apply { moveTo(19f, 41f); quadraticTo(21f, 9f, 52f, 9f); quadraticTo(80f, 11f, 82f, 41f); close() }
            drawPath(dome, Color(0xFF3E7CD9)); drawCircle(Color(0xFF2D5CA8), 2.8f, Offset(51f, 9.5f))
            drawOval(Color(0xFF2D5CA8), Offset(44f, 33f), Size(48f, 11f))
            drawArc(Color.White.copy(alpha = 0.3f), 200f, 50f, false, Offset(26f, 14f), Size(36f, 30f), style = Stroke(2.4f, cap = StrokeCap.Round))
        }
        Accessory.SUNGLASSES -> {
            val gk = Color(0xFF1E1E2C)
            drawRoundRect(gk, Offset(24f, 49f), Size(24f, 16f), CornerRadius(7f)); drawRoundRect(gk, Offset(52f, 49f), Size(24f, 16f), CornerRadius(7f))
            drawLine(gk, Offset(47f, 54f), Offset(53f, 54f), strokeWidth = 2.6f); drawLine(gk, Offset(24f, 53f), Offset(16f, 50f), strokeWidth = 2.4f); drawLine(gk, Offset(76f, 53f), Offset(84f, 50f), strokeWidth = 2.4f)
            drawLine(Color.White.copy(alpha = 0.55f), Offset(28f, 52f), Offset(33f, 52f), strokeWidth = 1.8f, cap = StrokeCap.Round); drawLine(Color.White.copy(alpha = 0.55f), Offset(56f, 52f), Offset(61f, 52f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        }
        Accessory.CAPE -> {
            for (sd in listOf(-1f, 1f)) {
                val collar = Path().apply { moveTo(50f + sd * 44f, 52f); lineTo(50f + sd * 18f, 86f); lineTo(50f + sd * 46f, 100f); close() }
                drawPath(collar, Color(0xFF7A1F3D)); drawPath(Path().apply { moveTo(50f + sd * 40f, 60f); lineTo(50f + sd * 22f, 86f); lineTo(50f + sd * 40f, 94f); close() }, Color(0xFFD9322B))
            }
            drawCircle(Color(0xFFFFD34A), 3.4f, Offset(50f, 90f))
        }
        Accessory.PARTY -> {
            val cone = Path().apply { moveTo(33f, 38f); lineTo(50f, 2f); lineTo(67f, 38f); close() }
            drawPath(cone, Color(0xFFB57CF0))
            for (k in 0..2) drawLine(Color(0xFFFFD34A), Offset(36f + k * 3f, 31f - k * 12f), Offset(64f - k * 3f, 31f - k * 12f + 6f), strokeWidth = 2.6f, cap = StrokeCap.Round)
            drawCircle(Color(0xFFFFD34A), 5f, Offset(50f, 3f)); drawCircle(Color.White.copy(alpha = 0.7f), 1.5f, Offset(48.6f, 1.8f))
        }
        Accessory.FLOWERS -> {
            val c = Offset(74f, 30f)
            for (i in 0 until 5) {
                val ang = (i * 72f - 90f) * PI.toFloat() / 180f
                drawCircle(Color(0xFFFF9EC4), 4.4f, Offset(c.x + cos(ang) * 5.5f, c.y + sin(ang) * 5.5f))
            }
            drawCircle(Color(0xFFFFD34A), 3.4f, c)
        }
    }
}


/**
 * Avatar con su marco: simple, plateado, dorado o —los de temporada— un prisma que gira despacio.
 * El marco sale del propio avatar (`AvatarDef.frame`), así que en todas partes se ve igual.
 */
@Composable
fun AvatarFramed(avatarId: Int, modifier: Modifier = Modifier, ring: androidx.compose.ui.unit.Dp = 4.dp, animate: Boolean = true) {
    val def = Avatars.byId(avatarId)
    val clock = LocalCozyClock.current
    val colors: List<Color> = when (def.frame) {
        AvatarFrame.NONE -> listOf(Color.White, Color.White)
        AvatarFrame.SOFT -> listOf(Color(0xFFF2E6D4), Color(0xFFE2CDAE), Color(0xFFF2E6D4))
        AvatarFrame.SILVER -> listOf(Color(0xFFF4F6FA), Color(0xFF9AA6BC), Color(0xFFF4F6FA), Color(0xFFB8C2D6), Color(0xFFF4F6FA))
        AvatarFrame.GOLD -> listOf(Color(0xFFFFF1B5), Color(0xFFE0A93B), Color(0xFFFFF1B5), Color(0xFFC98A1B), Color(0xFFFFF1B5))
        AvatarFrame.PRISM -> listOf(Color(0xFFFF8FA3), Color(0xFFFFD36E), Color(0xFF8EE3B0), Color(0xFF8EC9FF), Color(0xFFC59BFF), Color(0xFFFF8FA3))
        AvatarFrame.MYTHIC -> listOf(Color(0xFFFFF1B5), Color(0xFFFF6F91), Color(0xFFB57CF0), Color(0xFF5CE1E6), Color(0xFFFFD36E), Color(0xFFFFF1B5))
    }
    val angle = if (animate && (def.frame == AvatarFrame.PRISM || def.frame == AvatarFrame.MYTHIC)) clock.value * (if (def.frame == AvatarFrame.MYTHIC) 720f else 360f) else 0f
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .drawBehind {
                rotate(angle) { drawCircle(Brush.sweepGradient(colors), radius = size.minDimension / 2f) }
            }
            .padding(ring)
    ) {
        AvatarImage(avatarId, Modifier.fillMaxSize().clip(CircleShape), animate)
    }
}


// ------------------------------------------------------------------ escenas de fondo

private fun DrawScope.sparkle4(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx, cy - r); quadraticTo(cx, cy, cx + r, cy); quadraticTo(cx, cy, cx, cy + r); quadraticTo(cx, cy, cx - r, cy); quadraticTo(cx, cy, cx, cy - r); close()
    }
    drawPath(p, color)
}

private fun DrawScope.heart(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx, cy + r)
        cubicTo(cx - r * 1.6f, cy - r * 0.2f, cx - r * 0.7f, cy - r * 1.3f, cx, cy - r * 0.4f)
        cubicTo(cx + r * 0.7f, cy - r * 1.3f, cx + r * 1.6f, cy - r * 0.2f, cx, cy + r)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.drawScene(scene: AvatarScene, l: Look, time: Float, seed: Int) {
    val tw = { i: Int -> 0.35f + 0.65f * ((sin((time * 2 * PI + i * 1.7 + seed).toDouble()).toFloat() + 1f) / 2f) }
    when (scene) {
        AvatarScene.AUTO -> Unit
        AvatarScene.SPARKLES -> listOf(Offset(14f, 16f) to 6f, Offset(86f, 24f) to 5f, Offset(80f, 8f) to 3f, Offset(10f, 46f) to 3.4f, Offset(92f, 54f) to 4.4f)
            .forEachIndexed { i, (pt, r) -> sparkle4(pt.x, pt.y, r, Color.White.copy(alpha = tw(i))) }
        AvatarScene.RAYS -> {
            rotate(time * 40f, pivot = Offset(50f, 62f)) {
                for (i in 0 until 12) {
                    val a0 = (i * 30f) * PI.toFloat() / 180f; val a1 = a0 + 15f * PI.toFloat() / 180f
                    drawPath(Path().apply { moveTo(50f, 62f); lineTo(50f + cos(a0) * 90f, 62f + sin(a0) * 90f); lineTo(50f + cos(a1) * 90f, 62f + sin(a1) * 90f); close() }, Color.White.copy(alpha = 0.22f))
                }
            }
            drawCircle(Color.White.copy(alpha = 0.25f), 44f, Offset(50f, 62f))
        }
        AvatarScene.STARS -> {
            listOf(Offset(12f, 14f), Offset(86f, 20f), Offset(76f, 7f), Offset(8f, 42f), Offset(93f, 52f), Offset(24f, 6f), Offset(60f, 5f), Offset(90f, 38f), Offset(6f, 66f))
                .forEachIndexed { i, pt -> drawCircle(Color.White.copy(alpha = tw(i)), 1f + (i % 3) * 0.5f, pt) }
            sparkle4(16f, 28f, 4.2f, Color.White.copy(alpha = tw(3))); sparkle4(84f, 40f, 3.6f, l.inner.copy(alpha = tw(5)))
        }
        AvatarScene.CLOUDS -> {
            val dx = (time * 8f) % 100f
            listOf(Triple(12f, 20f, 1f), Triple(66f, 12f, 0.8f), Triple(78f, 44f, 0.7f)).forEach { (x0, y0, sc) ->
                val x = ((x0 + dx) % 120f) - 10f
                drawCircle(Color.White.copy(alpha = 0.85f), 7f * sc, Offset(x, y0)); drawCircle(Color.White.copy(alpha = 0.85f), 9f * sc, Offset(x + 8f * sc, y0 - 3f * sc))
                drawCircle(Color.White.copy(alpha = 0.85f), 7f * sc, Offset(x + 16f * sc, y0)); drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(x - 3f * sc, y0), Size(24f * sc, 7f * sc), CornerRadius(3.5f * sc))
            }
        }
        AvatarScene.HEARTS -> listOf(Offset(14f, 18f) to 4.6f, Offset(86f, 26f) to 5.4f, Offset(78f, 8f) to 3f, Offset(10f, 52f) to 3.4f, Offset(92f, 60f) to 4f)
            .forEachIndexed { i, (pt, r) -> heart(pt.x, pt.y - ((time * 6f + i * 3f) % 6f), r, Color(0xFFFF6F9B).copy(alpha = 0.55f + 0.35f * tw(i))) }
        AvatarScene.SNOW -> {
            for (i in 0 until 16) {
                val prog = ((i * 0.137f + time) % 1f)
                drawCircle(Color.White.copy(alpha = 0.9f), 1f + (i % 3) * 0.6f, Offset((i * 37 % 100).toFloat() + sin((prog * 6f + i).toDouble()).toFloat() * 3f, prog * 100f))
            }
        }
        AvatarScene.SPOOKY -> {
            drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.22f), 22f, Offset(78f, 22f)); drawCircle(Color(0xFFFFE9A8), 13f, Offset(78f, 22f))
            drawCircle(Color(0xFFE6C97A).copy(alpha = 0.5f), 2.6f, Offset(74f, 20f)); drawCircle(Color(0xFFE6C97A).copy(alpha = 0.5f), 1.8f, Offset(82f, 27f))
            fun bat(cx: Float, cy: Float, sc: Float) {
                drawPath(Path().apply {
                    moveTo(cx, cy); quadraticTo(cx - 5f * sc, cy - 6f * sc, cx - 11f * sc, cy - 2f * sc); quadraticTo(cx - 8f * sc, cy, cx - 6f * sc, cy + 3f * sc)
                    quadraticTo(cx - 3f * sc, cy, cx, cy + 2f * sc); quadraticTo(cx + 3f * sc, cy, cx + 6f * sc, cy + 3f * sc); quadraticTo(cx + 8f * sc, cy, cx + 11f * sc, cy - 2f * sc)
                    quadraticTo(cx + 5f * sc, cy - 6f * sc, cx, cy); close()
                }, Color(0xFF0E0820).copy(alpha = 0.85f))
            }
            bat(20f + sin((time * 2 * PI).toDouble()).toFloat() * 3f, 22f, 1f); bat(46f, 10f + sin((time * 2 * PI + 2).toDouble()).toFloat() * 2f, 0.7f)
            drawCircle(Color.White.copy(alpha = tw(1)), 1f, Offset(10f, 44f)); drawCircle(Color.White.copy(alpha = tw(2)), 1.2f, Offset(92f, 58f))
        }
        AvatarScene.PETALS -> for (i in 0 until 10) {
            val prog = ((i * 0.173f + time) % 1f)
            val x = (i * 41 % 100).toFloat() + sin((prog * 6f + i).toDouble()).toFloat() * 5f; val y = prog * 100f
            rotate(prog * 260f + i * 40f, pivot = Offset(x, y)) { drawOval(Color(0xFFFF9EBB).copy(alpha = 0.8f), Offset(x - 3.6f, y - 2.2f), Size(7.2f, 4.4f)) }
        }
        AvatarScene.BUBBLES -> for (i in 0 until 9) {
            val prog = ((i * 0.19f + time) % 1f)
            val x = (i * 29 % 90).toFloat() + 6f + sin((prog * 6f + i).toDouble()).toFloat() * 3f; val y = 100f - prog * 110f; val r = 3f + (i % 4) * 1.6f
            drawCircle(Color.White.copy(alpha = 0.22f), r, Offset(x, y)); drawCircle(Color.White.copy(alpha = 0.65f), r, Offset(x, y), style = Stroke(0.9f))
            drawCircle(Color.White.copy(alpha = 0.8f), r * 0.22f, Offset(x - r * 0.3f, y - r * 0.3f))
        }
        AvatarScene.CONFETTI -> {
            val cols = listOf(Color(0xFFFF6F9B), Color(0xFFFFD34A), Color(0xFF5CE1E6), Color(0xFFB57CF0), Color(0xFF8EE3B0))
            for (i in 0 until 16) {
                val prog = ((i * 0.113f + time) % 1f)
                val x = (i * 53 % 100).toFloat(); val y = prog * 100f
                rotate(prog * 540f + i * 31f, pivot = Offset(x, y)) { drawRect(cols[i % cols.size].copy(alpha = 0.9f), Offset(x - 2f, y - 1.2f), Size(4f, 2.4f)) }
            }
        }
        AvatarScene.AURORA -> {
            for (k in 0..2) {
                val col = listOf(Color(0xFF78E6C6), Color(0xFF8EC5FF), Color(0xFFB69CFF))[k]
                val path = Path().apply {
                    moveTo(0f, 0f)
                    var x = 0f
                    while (x <= 104f) { lineTo(x, 18f + k * 11f + sin((x / 22f + time * 2 * PI * (k + 1)).toDouble()).toFloat() * 7f); x += 6f }
                    lineTo(100f, 0f); close()
                }
                drawPath(path, Brush.verticalGradient(listOf(col.copy(alpha = 0f), col.copy(alpha = 0.55f)), 0f, 50f))
            }
            listOf(Offset(14f, 54f), Offset(88f, 60f), Offset(8f, 30f)).forEachIndexed { i, pt -> drawCircle(Color.White.copy(alpha = tw(i)), 1.1f, pt) }
        }
        AvatarScene.FLAMES -> {
            for (sd in listOf(0f, 100f)) for (k in 0..3) {
                val h = 26f + k * 6f + sin((time * 2 * PI * 2 + k + sd).toDouble()).toFloat() * 4f
                val x = if (sd == 0f) 4f + k * 7f else 96f - k * 7f
                val flame = Path().apply { moveTo(x - 4f, 100f); quadraticTo(x - 5f, 100f - h * 0.5f, x, 100f - h); quadraticTo(x + 5f, 100f - h * 0.5f, x + 4f, 100f) ; close() }
                drawPath(flame, Color(0xFFFF8A1F).copy(alpha = 0.7f)); drawPath(Path().apply { moveTo(x - 2f, 100f); quadraticTo(x - 2.5f, 100f - h * 0.35f, x, 100f - h * 0.6f); quadraticTo(x + 2.5f, 100f - h * 0.35f, x + 2f, 100f); close() }, Color(0xFFFFD34A).copy(alpha = 0.85f))
            }
            for (i in 0 until 6) drawCircle(Color(0xFFFFB347).copy(alpha = 1f - ((i * 0.17f + time) % 1f)), 1f + (i % 2), Offset((i * 31 % 90).toFloat() + 5f, 90f - ((i * 0.17f + time) % 1f) * 80f))
        }
    }
}
