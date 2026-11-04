package com.list_it.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.ui.components.ButtonView
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Four-step onboarding (iOS `IntroView`). */
@Composable
fun IntroScreen(onGetStarted: () -> Unit) {
    val palette = LocalPalette.current
    val pager = rememberPagerState { 4 }
    val lastStep = 3
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(100)
        appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
    }

    Box(Modifier.fillMaxSize().background(palette.secondaryBackground).windowInsetsPadding(WindowInsets.safeDrawing)) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                val s = 0.88f + 0.12f * appear.value
                scaleX = s; scaleY = s
                translationY = (1f - appear.value) * 350.dp.toPx()
            },
        ) { page ->
            when (page) {
                0 -> OnboardingPage(Icons.Filled.Inventory2, "Start with a List", IosColor.blue) {
                    BodyText("Lists are your top-level spaces - the big areas of your life you want to keep organised.")
                    ExampleBox(IosColor.blue) {
                        listOf("School", "Work", "Home", "Entertainment").forEach { LabelRow(it, Icons.Filled.Inbox, IosFont.subheadline) }
                    }
                }
                1 -> OnboardingPage(Icons.Outlined.FolderSpecial, "Break it down with Collections", IosColor.purple) {
                    BodyText("Inside each List, Collections group the Tasks and Notes that belong together.")
                    ExampleBox(IosColor.purple, spacing = 12) {
                        LabelRow("School", Icons.Filled.Inbox, IosFont.subheadline.copy(fontWeight = FontWeight.SemiBold))
                        Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("General", "Maths", "Science").forEach {
                                LabelRow(it, Icons.Filled.Folder, IosFont.footnote, palette.secondary)
                            }
                        }
                    }
                    Text(
                        "Every new List starts with a General Collection, ready to use.",
                        style = IosFont.caption, color = palette.secondary, textAlign = TextAlign.Center,
                    )
                }
                2 -> OnboardingPage(Icons.Filled.Checklist, "Tasks to do, notes to remember", IosColor.green) {
                    BodyText("Add a Task as a reminder, or a Note to hold on to something - both live right in the Collection they belong to.")
                    ExampleBox(IosColor.green, spacing = 12) {
                        LabelRow("Complete Maths homework", Icons.Outlined.CheckCircle, IosFont.subheadline)
                        LabelRow("Quadratic formula: x = (-b ± √(b²-4ac)) / 2a", Icons.AutoMirrored.Filled.Notes, IosFont.subheadline)
                    }
                }
                else -> OnboardingPage(Icons.Filled.AutoAwesome, "You're ready to get organised", IosColor.indigo, spacing = 24) {
                    BodyText("Create your first List and start grouping your Tasks and Notes into Collections.")
                    ButtonView("Get Started", showArrow = true, onClick = onGetStarted)
                }
            }
        }

        // Page indicator with a background capsule (iOS `.indexViewStyle(.page(backgroundDisplayMode: .always))`).
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clip(CircleShape)
                .background(palette.primary.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) { i ->
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (pager.currentPage == i) palette.primary else palette.primary.copy(alpha = 0.3f)))
            }
        }

        if (pager.currentPage != lastStep) {
            Text(
                "Skip",
                style = IosFont.subheadline, fontWeight = FontWeight.Medium,
                color = IosColor.blue,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onGetStarted).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    title: String,
    accent: Color,
    spacing: Int = 20,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    val reveal = remember { Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) {
        delay(350)
        scope.launch { reveal.animateTo(1f, spring(dampingRatio = 0.66f, stiffness = 70f)) }
    }
    val r = reveal.value
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "LIST IT",
            style = IosFont.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = palette.secondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 40.dp).graphicsLayer { alpha = r.coerceIn(0f, 1f); translationY = (1 - r) * -30.dp.toPx() },
        )
        Spacer(Modifier.height(40.dp))
        Icon(
            icon, null, tint = accent,
            modifier = Modifier.padding(bottom = 24.dp).size(76.dp).graphicsLayer {
                val s = 0.2f + 0.8f * r
                scaleX = s; scaleY = s
                rotationZ = (1 - r) * -45f
                alpha = r.coerceIn(0f, 1f)
            },
        )
        Text(
            title,
            style = IosFont.largeTitle, color = palette.primary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)
                .graphicsLayer { alpha = r.coerceIn(0f, 1f); translationY = (1 - r) * 30.dp.toPx() },
        )
        Column(
            Modifier.padding(horizontal = 24.dp).graphicsLayer { alpha = r.coerceIn(0f, 1f); translationY = (1 - r) * 40.dp.toPx() },
            verticalArrangement = Arrangement.spacedBy(spacing.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = IosFont.body.copy(lineHeight = 26.sp), color = LocalPalette.current.secondary, textAlign = TextAlign.Center)
}

@Composable
private fun ExampleBox(accent: Color, spacing: Int = 8, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.08f)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

@Composable
private fun LabelRow(text: String, icon: ImageVector, style: TextStyle, color: Color = LocalPalette.current.primary.copy(alpha = 0.85f)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = IosColor.blue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = style, color = color)
    }
}
