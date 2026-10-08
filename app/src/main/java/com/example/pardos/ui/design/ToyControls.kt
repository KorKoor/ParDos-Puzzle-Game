package com.korkoor.pardos.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================================
//  CONTROLES "DE JUGUETE": botones, botones de texto y campos de texto con el mismo lenguaje que JellyCard.
//  Aceptan los mismos parámetros que los de Material para poder intercambiarlos sin reescribir las pantallas.
// =====================================================================================

/** Sustituto de `Button` de Material: pieza 3D con labio que se hunde al pulsar. El color sale de [colors]. */
@Composable
fun ToyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(Radius.Medium),
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = Sage),
    @Suppress("UNUSED_PARAMETER") elevation: androidx.compose.material3.ButtonElevation? = null,
    @Suppress("UNUSED_PARAMETER") border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val container = if (enabled) colors.containerColor else Navy.copy(alpha = 0.12f).compositeOver(Color.White)
    val ink = if (enabled) colors.contentColor.takeIf { it != Color.Unspecified } ?: Color.White else Navy.copy(alpha = 0.4f)
    JellyCard(
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        fill = container,
        lip = if (enabled) container.deepen(0.38f) else Navy.copy(alpha = 0.10f),
        lipHeight = 5.dp,
        brush = if (enabled) container.toyGradient() else null
    ) {
        if (enabled) {
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 4.dp, start = 14.dp, end = 14.dp)
                    .fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            )
        }
        CompositionLocalProvider(LocalContentColor provides ink) {
            Row(
                modifier = Modifier.align(Alignment.Center).padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

/** Sustituto de `TextButton`: una píldora suave que se tiñe al pulsar (para acciones secundarias). */
@Composable
fun ToyTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    @Suppress("UNUSED_PARAMETER") contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val ink = if (enabled) colors.contentColor.takeIf { it != Color.Unspecified } ?: Sage else Navy.copy(alpha = 0.35f)
    CompositionLocalProvider(LocalContentColor provides ink) {
        Row(
            modifier = modifier
                .clip(shape)
                .background(ink.copy(alpha = 0.08f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Campo de texto "hundido": canal en el fondo del papel, borde que se enciende con el foco y texto grande.
 * Sustituye a `OutlinedTextField` en los diálogos y formularios del juego.
 */
@Composable
fun ToyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    enabled: Boolean = true,
    accent: Color = Sage,
    textStyle: TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Navy),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        cursorBrush = SolidColor(accent),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(Navy.copy(alpha = 0.09f), Navy.copy(alpha = 0.04f))))
                    .border(if (focused) 2.dp else 1.dp, if (focused) accent else Navy.copy(alpha = 0.10f), shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) { leading(); androidx.compose.foundation.layout.Spacer(Modifier.padding(end = 10.dp)) }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = textStyle.copy(color = Navy.copy(alpha = 0.3f)))
                    }
                    inner()
                }
                if (trailing != null) trailing()
            }
        }
    )
}

