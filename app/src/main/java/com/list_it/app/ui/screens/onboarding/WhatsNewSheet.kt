package com.list_it.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.list_it.app.model.WhatsNewFeature
import com.list_it.app.ui.LocalPrefs
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette

private val features = listOf(
    WhatsNewFeature("calendar.badge.clock", "Scheduled Tasks", "See every scheduled Task in one place. Browse by month, pick a day, and jump back to today anytime. Find it in the menu on the home screen."),
    WhatsNewFeature("bell.badge", "Separate Reminders", "Due dates and alerts are now independent. Set a final deadline for your task while picking a completely different time to receive your alert notification."),
    WhatsNewFeature("arrow.uturn.backward", "Completion Undo", "Accidentally checked off a task? You can now reverse and restore any tasks completed today directly from the completed list."),
    WhatsNewFeature("rectangle.grid.1x2", "Redesigned Completed View", "Enjoy a beautiful, minimal, and architectural archive feed."),
    WhatsNewFeature("list.bullet", "Interactive Default Lists", "All tasks appearing in your smart default lists are now fully interactive. Simply tap on any task to instantly open its details and update its properties."),
)

/** Picks the icon to show next to each new feature. */
private fun featureIcon(name: String): ImageVector = when (name) {
    "calendar.badge.clock" -> Icons.Outlined.EventAvailable
    "bell.badge" -> Icons.Filled.NotificationsActive
    "arrow.uturn.backward" -> Icons.AutoMirrored.Filled.Undo
    "rectangle.grid.1x2" -> Icons.Filled.ViewAgenda
    else -> Icons.AutoMirrored.Filled.FormatListBulleted
}

/** Shows what's new in this version of the app. Tapping Done stops it showing again. */
@Composable
fun ColumnScope.WhatsNewSheet(currentVersion: Int, dismiss: () -> Unit) {
    val prefs = LocalPrefs.current
    val palette = LocalPalette.current
    IosTopBar(
        "What's new",
        leading = { BarButton("Dismiss", bold = true, onClick = dismiss) },
        trailing = { BarButton("Done", bold = true) { prefs.lastSeenWhatsNewVersion = currentVersion; dismiss() } },
    )
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Here's what's changed in this update", color = palette.secondary, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
        IosDivider(Modifier.padding(16.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                        Icon(featureIcon(feature.icon), null, tint = IosColor.orange, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(feature.title, style = IosFont.headline, color = palette.primary)
                        Text(feature.description, style = IosFont.subheadline, color = palette.secondary)
                    }
                }
                IosDivider()
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
