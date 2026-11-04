package com.list_it.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** iOS Dynamic Type sizes (Large) used by SwiftUI's text styles. */
object IosFont {
    val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 41.sp)
    val title2 = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
    val title3 = TextStyle(fontSize = 20.sp, lineHeight = 25.sp)
    val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 22.sp)
    val subheadline = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
    val footnote = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val caption2 = TextStyle(fontSize = 11.sp, lineHeight = 13.sp)
}

val IosTypography = Typography(
    bodyLarge = IosFont.body,
    bodyMedium = IosFont.subheadline,
    bodySmall = IosFont.footnote,
    titleLarge = IosFont.title3.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = IosFont.headline,
    labelLarge = IosFont.subheadline.copy(fontWeight = FontWeight.Medium),
)
