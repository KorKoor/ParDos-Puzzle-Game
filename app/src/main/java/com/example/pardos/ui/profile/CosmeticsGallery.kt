package com.korkoor.pardos.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korkoor.pardos.domain.shop.Avatars
import com.korkoor.pardos.domain.shop.Banners

/**
 * Galería para revisar todo el catálogo de cosméticos de golpe. Solo se abre en builds de depuración
 * (`--ei debug_gallery 1|2 --ei debug_page N`): 1 = avatares (20 por página), 2 = banners (7 por página).
 */
@Composable
fun CosmeticsGallery(kind: Int, page: Int) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF4EFE6)).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (kind == 3) {
            val items = com.korkoor.pardos.domain.collection.CollectibleCatalog.all.drop(page * 12).take(12)
            Text("Cartas ${page * 12 + 1}–${page * 12 + items.size} de 320", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B2B3A))
            Spacer(Modifier.height(6.dp))
            items.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { c ->
                        Box(Modifier.weight(1f)) {
                            com.korkoor.pardos.ui.collection.CollectibleCard(c, Modifier.fillMaxWidth(), owned = c.number % 5 != 0, foil = c.number == 8, copies = if (c.number == 2) 3 else 1, animate = false)
                        }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        } else if (kind == 1) {
            val items = Avatars.all.drop(page * 20).take(20)
            Text("Avatares ${page * 20 + 1}–${page * 20 + items.size} de ${Avatars.all.size}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B2B3A))
            Spacer(Modifier.height(6.dp))
            items.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { a ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            AvatarFramed(a.id, Modifier.fillMaxWidth().aspectRatio(1f), ring = 3.dp, animate = false)
                            Text(a.name, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2B3A), maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                    repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        } else {
            val items = Banners.all.drop(page * 7).take(7)
            Text("Banners ${page * 7 + 1}–${page * 7 + items.size} de ${Banners.all.size}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B2B3A))
            Spacer(Modifier.height(6.dp))
            items.forEach { b ->
                Box(Modifier.fillMaxWidth().height(92.dp).padding(vertical = 3.dp).clip(RoundedCornerShape(16.dp))) {
                    ProfileBanner(b, Modifier.fillMaxSize(), animate = false)
                    Text(b.name, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(b.ink), modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                }
            }
        }
    }
}
