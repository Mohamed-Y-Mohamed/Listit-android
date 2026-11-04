package com.list_it.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.R
import com.list_it.app.ui.components.AuthColor
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.appBackground

// Shared layout of the login, sign-up and reset screens, from the web app's
// Capacitor auth screens: app mark, heading, "… with email" divider, form, footer link.

/** Full-height scrolling column, centred and capped at 26rem like the web card. */
@Composable
internal fun AuthLayout(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(appBackground).windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 416.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight).padding(horizontal = 24.dp, vertical = 8.dp),
            content = content,
        )
    }
}

/** The app mark with the LIST IT wordmark, then the screen's heading. */
@Composable
internal fun AuthHeader(title: String) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.app_logo), contentDescription = null,
                modifier = Modifier.size(48.dp).shadow(6.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.size(12.dp))
            Text("LIST IT", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (palette.isDark) AuthColor.orange400 else AuthColor.orange500)
        }
        Spacer(Modifier.height(32.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (palette.isDark) Color.White else Color(0xFF111827))
    }
}

/** The thin rule with a pill in the middle, e.g. "log in with email". */
@Composable
internal fun AuthDivider(text: String) {
    val palette = LocalPalette.current
    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        HorizontalDivider(color = if (palette.isDark) Color(0xFF374151).copy(alpha = 0.5f) else Color(0xFFD1D5DB).copy(alpha = 0.5f))
        Text(
            text, fontSize = 14.sp, color = if (palette.isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563),
            modifier = Modifier.clip(CircleShape).background(palette.ramp.card).padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

/** "Don't have an account? Sign up" — a secondary route out of the screen. */
@Composable
internal fun AuthFooter(question: String, action: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(question, fontSize = 15.sp, color = if (palette.isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563))
        Spacer(Modifier.size(6.dp))
        Text(
            action, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AuthColor.orange500,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(4.dp),
        )
    }
}
