package com.list_it.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/** Renders markdown (iOS MarkdownUI `Markdown`). */
@Composable
fun MarkdownText(content: String, modifier: Modifier = Modifier, fontSize: Int = 17) {
    val palette = LocalPalette.current
    val body = IosFont.body.copy(fontSize = fontSize.sp, lineHeight = (fontSize + 6).sp, color = palette.primary)
    Markdown(
        content = content,
        modifier = modifier,
        colors = markdownColor(text = palette.primary),
        typography = markdownTypography(
            h1 = body.copy(fontSize = (fontSize * 2).sp, lineHeight = (fontSize * 2.4).sp, fontWeight = FontWeight.SemiBold),
            h2 = body.copy(fontSize = (fontSize * 1.5).sp, lineHeight = (fontSize * 1.9).sp, fontWeight = FontWeight.SemiBold),
            h3 = body.copy(fontSize = (fontSize * 1.25).sp, lineHeight = (fontSize * 1.6).sp, fontWeight = FontWeight.SemiBold),
            h4 = body.copy(fontWeight = FontWeight.SemiBold),
            h5 = body.copy(fontWeight = FontWeight.SemiBold),
            h6 = body.copy(fontWeight = FontWeight.SemiBold, color = palette.secondary),
            text = body,
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
        ),
    )
}
