package com.list_it.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.WebColor

/** A top-level destination in the bottom bar. */
data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Lists, Progress and Settings, as in the web app's Capacitor build (`navTabs.ts`). */
val TABS = listOf(
    Tab("lists", "Lists", Icons.Outlined.GridView),
    Tab("progress", "Progress", Icons.Outlined.BarChart),
    Tab("settings", "Settings", Icons.Outlined.Settings),
)

/** Bottom tab bar with a sliding highlight behind the selected tab (web `NativeTabBar`). */
@Composable
fun TabBar(currentRoute: String, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    val selectedIndex = TABS.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().background(palette.ramp.deep.copy(alpha = 0.95f)).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color(16, 24, 40).copy(alpha = 0.07f))
        BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp)) {
            val tabWidth = maxWidth / TABS.size
            val indicatorX by animateDpAsState(
                tabWidth * selectedIndex,
                spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
                label = "tab indicator",
            )
            Box(
                Modifier.offset { IntOffset(indicatorX.roundToPx(), 0) }.width(tabWidth).fillMaxHeight().padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(WebColor.blue500.copy(alpha = if (palette.isDark) 0.15f else 0.10f), RoundedCornerShape(14.dp)),
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                TABS.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    val color = when {
                        isSelected -> WebColor.blue500
                        palette.isDark -> WebColor.gray500
                        else -> WebColor.gray400
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .semantics { selected = isSelected }
                            .clickable(role = Role.Tab) { if (!isSelected) onSelect(tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    ) {
                        Icon(tab.icon, null, tint = color, modifier = Modifier.size(22.dp))
                        Text(
                            tab.label, fontSize = 11.sp, color = color,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
