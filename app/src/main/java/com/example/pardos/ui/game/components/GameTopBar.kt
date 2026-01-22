package com.example.pardos.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.pardos.R
@Composable
fun GameTopBar(
    selectedShapeType: String,
    onShapeSelected: (String) -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ShapeSelector(selectedShapeType, onShapeSelected)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
            color = Color(0xFFE07A5F).copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .zIndex(10f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE07A5F).copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onBackToMenu
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    // ✅ Traducción para accesibilidad
                    contentDescription = stringResource(R.string.close_game),
                    tint = Color(0xFFE07A5F),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ShapeSelector(current: String, onShapeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    // ✅ Lista de IDs internos (no se traducen, se usan para la lógica)
    val shapes = listOf("Square", "Rhombus", "Triangle", "Hexagon")

    Box {
        Surface(
            onClick = { expanded = true },
            color = Color.White.copy(alpha = 0.85f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3D405B).copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF81B29A)
                )
                Spacer(Modifier.width(10.dp))

                // ✅ Traducimos el nombre de la forma actual
                Text(
                    text = getShapeTranslation(current).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF3D405B),
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF3D405B).copy(alpha = 0.4f)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(4.dp)
        ) {
            shapes.forEach { shape ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = getShapeTranslation(shape), // ✅ Traducido en la lista
                            color = if (current == shape) Color(0xFF81B29A) else Color(0xFF3D405B),
                            fontWeight = if (current == shape) FontWeight.Black else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        if (current == shape) {
                            Box(Modifier.size(6.dp).background(Color(0xFF81B29A), CircleShape))
                        }
                    },
                    onClick = {
                        onShapeSelected(shape)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ✅ Función auxiliar para mapear el ID de la forma a su StringResource
@Composable
private fun getShapeTranslation(shape: String): String {
    return when (shape) {
        "Square" -> stringResource(R.string.shape_square)
        "Rhombus" -> stringResource(R.string.shape_rhombus)
        "Triangle" -> stringResource(R.string.shape_triangle)
        "Hexagon" -> stringResource(R.string.shape_hexagon)
        else -> shape
    }
}