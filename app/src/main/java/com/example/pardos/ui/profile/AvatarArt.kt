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
        scale(u, u, pivot = Offset.Zero) { drawAvatar(def, blink) }
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

private fun DrawScope.drawAvatar(def: AvatarDef, blink: Boolean) {
    val a = def.animal ?: return
    val l = look(def)
    val night = def.variant == AvatarVariant.MIDNIGHT

    // ---- fondo
    drawRect(Brush.verticalGradient(listOf(l.bg1, l.bg2)), size = Size(100f, 100f))
    if (night) {
        listOf(Offset(14f, 14f) to 1.8f, Offset(86f, 22f) to 1.4f, Offset(78f, 8f) to 1.1f, Offset(10f, 44f) to 1.2f, Offset(92f, 54f) to 1.5f)
            .forEach { (p, r) -> drawCircle(Color.White.copy(alpha = 0.85f), r, p) }
    }
    if (def.variant == AvatarVariant.GOLD) {
        listOf(Offset(14f, 18f), Offset(88f, 30f), Offset(84f, 8f)).forEach { drawCircle(Color.White.copy(alpha = 0.8f), 2f, it) }
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
        AnimalKind.FROG, AnimalKind.PENGUIN, AnimalKind.CHICK -> Unit
    }
    if (a == AnimalKind.FROG) {
        // ojos saltones por encima de la cabeza
        drawCircle(l.head, 13f, Offset(33f, 35f)); drawCircle(l.head, 13f, Offset(67f, 35f))
    }

    // ---- cabeza
    val headRect = Offset(17f, 29f)
    val headSize = Size(66f, 58f)
    drawOval(Brush.verticalGradient(listOf(lerp(l.head, Color.White, 0.18f), l.head, lerp(l.head, l.dark, 0.12f)), 29f, 87f), headRect, headSize)
    drawOval(l.dark.copy(alpha = 0.16f), headRect, headSize, style = Stroke(1.4f))

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
    }

    // ---- mejillas
    drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.5f), 5.6f, Offset(27f, 68f)); drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.5f), 5.6f, Offset(73f, 68f))

    // ---- ojos
    val ink = Color(0xFF2B2B3A)
    fun eye(cx: Float, cy: Float, r: Float, white: Boolean = false) {
        if (white) { drawCircle(Color.White, r * 1.7f, Offset(cx, cy)); drawCircle(ink.copy(alpha = 0.25f), r * 1.7f, Offset(cx, cy), style = Stroke(1.2f)) }
        val h = if (blink) r * 0.18f else r
        drawOval(ink, Offset(cx - r, cy - h), Size(r * 2, h * 2))
        if (!blink) drawCircle(Color.White, r * 0.38f, Offset(cx - r * 0.3f, cy - r * 0.35f))
    }
    when (a) {
        AnimalKind.FROG -> { eye(33f, 35f, 4.6f, white = true); eye(67f, 35f, 4.6f, white = true) }
        AnimalKind.OWL -> { eye(36f, 56f, 6.5f, white = true); eye(64f, 56f, 6.5f, white = true) }
        AnimalKind.PANDA -> { eye(36f, 57f, 3.6f, white = true); eye(64f, 57f, 3.6f, white = true) }
        AnimalKind.RACCOON -> { eye(35f, 58f, 3.8f, white = true); eye(65f, 58f, 3.8f, white = true) }
        AnimalKind.PENGUIN -> { eye(38f, 58f, 4f); eye(62f, 58f, 4f) }
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
    }
    val angle = if (animate && def.frame == AvatarFrame.PRISM) clock.value * 360f else 0f
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
