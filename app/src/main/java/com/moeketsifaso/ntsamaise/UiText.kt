package com.moeketsifaso.ntsamaise

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

/** Localizes app-owned copy; recognized passages use Text directly. */
@Composable
internal fun UiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current
) {
    Text(LocalAppLanguage.current.ui(text), modifier = modifier, color = color,
        fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, style = style)
}
