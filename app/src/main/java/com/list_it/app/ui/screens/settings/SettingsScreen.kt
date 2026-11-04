package com.list_it.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.data.PasswordRules
import com.list_it.app.model.ListDisplayStyle
import com.list_it.app.model.SurfaceChoice
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.LocalPrefs
import com.list_it.app.ui.components.RequestNotificationPermission
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.SURFACE_RAMPS
import com.list_it.app.ui.theme.WebColor
import com.list_it.app.ui.theme.appBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Settings, as designed for the web app's Capacitor (Android) build: collapsible
// sections, one open at a time, then Privacy Policy, Feedback and Sign Out rows.

private data class Section(val id: String, val title: String, val icon: ImageVector, val description: String)

private val SECTIONS = listOf(
    Section("profile", "Profile", Icons.Outlined.Person, "Manage your personal information"),
    Section("appearance", "Appearance", Icons.Outlined.LightMode, "Theme, background and layout"),
    Section("notifications", "Notifications", Icons.Outlined.Notifications, "Reminders from your tasks"),
    Section("security", "Security", Icons.Outlined.Lock, "Password and security settings"),
    Section("account", "Account", Icons.Outlined.Delete, "Account deletion and data management"),
)

private const val PRIVACY_URL = "https://list-it-dom.netlify.app/privacy"
private const val FEEDBACK_URL = "https://forms.gle/dE21epzC4L8Tx1Kd9"

private data class Toast(val success: Boolean, val message: String)

@Composable
fun SettingsScreen() {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var openSection by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<Toast?>(null) }
    val show: (Boolean, String) -> Unit = { ok, msg -> toast = Toast(ok, msg) }

    LaunchedEffect(toast) { if (toast != null) { delay(5_000); toast = null } }

    Box(Modifier.fillMaxSize().background(appBackground)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Settings, null, tint = palette.accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = strong())
            }
            Text("Manage your account, security, and preferences", fontSize = 16.sp, color = muted(), modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SECTIONS.forEach { section ->
                    SectionCard(section, isOpen = openSection == section.id, onToggle = {
                        openSection = if (openSection == section.id) null else section.id
                    }) {
                        when (section.id) {
                            "profile" -> ProfileSection(show)
                            "appearance" -> AppearanceSection()
                            "notifications" -> NotificationsSection()
                            "security" -> SecuritySection(show)
                            else -> AccountSection(show)
                        }
                    }
                }
                LinkRow(Icons.Outlined.VerifiedUser, palette.accent, "Privacy Policy", "What we collect, and how to delete it") {
                    uriHandler.openUri(PRIVACY_URL)
                }
                LinkRow(Icons.Outlined.RateReview, palette.accent, "Feedback and feature requests", "Tell us what to build next", external = true) {
                    uriHandler.openUri(FEEDBACK_URL)
                }
                LinkRow(
                    Icons.AutoMirrored.Filled.Logout, if (palette.isDark) Color(0xFFFDBA74) else Color(0xFFEA580C),
                    "Sign Out", "Leave this account on this device", titleColor = if (palette.isDark) Color(0xFFFDBA74) else Color(0xFFEA580C),
                ) { scope.launch { db.signOutUser() } }
            }
        }

        toast?.let { ToastView(it) { toast = null } }
    }
}

// ---------- Building blocks ----------

@Composable private fun strong() = if (LocalPalette.current.isDark) Color.White else WebColor.gray900
@Composable private fun muted() = if (LocalPalette.current.isDark) WebColor.gray400 else WebColor.gray500
@Composable private fun label() = if (LocalPalette.current.isDark) WebColor.gray300 else Color(0xFF374151)

@Composable
private fun SectionCard(section: Section, isOpen: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    val chevron by animateFloatAsState(if (isOpen) 180f else 0f, tween(200), label = "chevron")
    Column(Modifier.fillMaxWidth().clip(shape).background(palette.ramp.card).border(1.dp, palette.ramp.border, shape)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics { stateDescription = if (isOpen) "Expanded" else "Collapsed" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(section.icon, null, tint = if (isOpen) WebColor.indigo else muted(), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(section.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = strong(), maxLines = 1)
                Text(section.description, fontSize = 12.sp, color = WebColor.gray500, maxLines = 1)
            }
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = if (palette.isDark) WebColor.gray500 else WebColor.gray400, modifier = Modifier.size(18.dp).rotate(chevron))
        }
        AnimatedVisibility(isOpen, enter = expandVertically(tween(200)) + fadeIn(tween(200)), exit = shrinkVertically(tween(200)) + fadeOut(tween(200))) {
            Column {
                Box(Modifier.fillMaxWidth().height(1.dp).background(if (palette.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) { content() }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = strong())
        Text(subtitle, fontSize = 14.sp, color = if (LocalPalette.current.isDark) WebColor.gray400 else Color(0xFF4B5563), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector, tint: Color, title: String, subtitle: String,
    external: Boolean = false, titleColor: Color = strong(), onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(palette.ramp.card)
            .border(1.dp, if (palette.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f), shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor)
            Text(subtitle, fontSize = 12.sp, color = WebColor.gray500)
        }
        if (external) Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = if (palette.isDark) WebColor.gray500 else WebColor.gray400, modifier = Modifier.size(16.dp))
    }
}

/** Rounded field box on the raised surface, as the web inputs. */
@Composable
private fun Modifier.fieldBox(focused: Boolean = false, error: Boolean = false): Modifier {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(8.dp)
    val border = when {
        error -> if (palette.isDark) Color(0xFFDC2626) else Color(0xFFFCA5A5)
        focused -> palette.accent
        palette.isDark -> palette.ramp.border
        else -> WebColor.gray300
    }
    val bg = when {
        error -> if (palette.isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color.White
        palette.isDark -> palette.ramp.raised
        else -> Color.White
    }
    return this.fillMaxWidth().clip(shape).background(bg).border(if (focused) 2.dp else 1.dp, border, shape).padding(horizontal = 12.dp, vertical = 10.dp)
}

@Composable
private fun FieldLabel(text: String) = Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = label(), modifier = Modifier.padding(bottom = 4.dp))

@Composable
private fun InputField(
    value: String, onChange: (String) -> Unit, placeholder: String, description: String,
    password: Boolean = false, enabled: Boolean = true, error: Boolean = false,
) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    Row(Modifier.fieldBox(focused, error), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, color = WebColor.gray400)
            BasicTextField(
                value, onChange, enabled = enabled, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = strong()),
                cursorBrush = SolidColor(palette.accent),
                visualTransformation = if (password && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.semantics { contentDescription = description },
            )
        }
        if (password) {
            Icon(
                if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (reveal) "Hide password" else "Show password",
                tint = WebColor.gray400, modifier = Modifier.padding(start = 8.dp).size(20.dp).clickable { reveal = !reveal },
            )
        }
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, color: Color, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (enabled) color else if (palette.isDark) palette.ramp.raised else WebColor.gray300)
            .clickable(enabled = enabled && !busy, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) Color.White else if (palette.isDark) WebColor.gray400 else WebColor.gray500
        if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = fg, strokeWidth = 2.dp)
        else Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

/** The web's pill switch: 48x28, orange when on in dark, sky in light. */
@Composable
private fun PillSwitch(checked: Boolean, description: String, onColor: Color, onToggle: () -> Unit) {
    val knob by animateFloatAsState(if (checked) 24f else 4f, tween(200), label = "knob")
    Box(
        Modifier.size(width = 48.dp, height = 44.dp)
            .semantics { role = Role.Switch; contentDescription = description; stateDescription = if (checked) "On" else "Off" }
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 48.dp, height = 28.dp).clip(CircleShape).background(if (checked) onColor else WebColor.gray300)) {
            Box(Modifier.offset { IntOffset(knob.dp.roundToPx(), 4.dp.roundToPx()) }.size(20.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, iconTint: Color, title: String, checked: Boolean, onColor: Color, onToggle: () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(palette.ramp.raised)
            .border(1.dp, if (palette.isDark) palette.ramp.border else Color(0xFFE5E7EB), shape).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = strong())
            Text(if (checked) "On" else "Off", fontSize = 14.sp, color = muted())
        }
        PillSwitch(checked, title, onColor, onToggle)
    }
}

@Composable
private fun ToastView(toast: Toast, onClose: () -> Unit) {
    val (bg, border, fg) = if (toast.success) Triple(Color(0xFFDCFCE7), Color(0xFFBBF7D0), Color(0xFF16A34A)) else Triple(Color(0xFFFEE2E2), Color(0xFFFECACA), Color(0xFFDC2626))
    Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(16.dp), contentAlignment = Alignment.TopEnd) {
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(bg).border(1.dp, border, RoundedCornerShape(8.dp)).clickable(onClick = onClose).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (toast.success) Icons.Filled.CheckCircle else Icons.Outlined.WarningAmber, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(toast.message, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fg)
        }
    }
}

// ---------- Sections ----------

@Composable
private fun ProfileSection(show: (Boolean, String) -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val user = db.currentUser
    var fullName by remember { mutableStateOf(user?.name.orEmpty()) }
    var saving by remember { mutableStateOf(false) }

    SectionHeading("Profile Settings", "Update your personal information and profile details")
    Column {
        FieldLabel("Email Address")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).fieldBox(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Email, null, tint = label(), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(user?.email.orEmpty(), fontSize = 16.sp, color = label(), maxLines = 1)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Read-only", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (palette.isDark) WebColor.gray300 else Color(0xFF4B5563),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (palette.isDark) palette.ramp.raised else Color(0xFFF3F4F6)).padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Text("Email address cannot be changed", fontSize = 12.sp, color = WebColor.gray500, modifier = Modifier.padding(top = 4.dp))
    }
    Column {
        FieldLabel("Full Name")
        InputField(fullName, { fullName = it }, "Enter your full name", "Full Name", enabled = !saving)
    }
    Column {
        FieldLabel("Account Created")
        Text(
            user?.createdAt?.let { DateTimeFormatter.ofPattern("MMMM d, yyyy").format(it.atZone(ZoneId.systemDefault())) } ?: "Loading...",
            fontSize = 16.sp, color = label(), modifier = Modifier.fieldBox(),
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ActionButton("Save Changes", Icons.Outlined.Save, palette.accent, enabled = fullName.isNotBlank(), busy = saving) {
            if (fullName.isBlank()) { show(false, "Full name is required"); return@ActionButton }
            saving = true
            scope.launch {
                if (db.updateName(fullName.trim())) show(true, "Profile updated successfully") else show(false, "Failed to update profile")
                saving = false
            }
        }
    }
}

@Composable
private fun AppearanceSection() {
    val prefs = LocalPrefs.current
    val palette = LocalPalette.current
    val isDark = palette.isDark

    SectionHeading("Appearance", "Choose how List It looks on this device.")
    SwitchRow(
        if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode, if (isDark) WebColor.orange400 else WebColor.sky500,
        "Dark mode", isDark, WebColor.orange500,
    ) { prefs.darkMode = !isDark }

    // Background: four ramps for the current theme, each choice stored per theme.
    Column {
        Text("Background", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = strong(), modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SurfaceChoice.entries.forEach { choice ->
                val ramp = SURFACE_RAMPS.getValue(isDark).getValue(choice)
                val active = prefs.surface(isDark) == choice
                val shape = RoundedCornerShape(12.dp)
                Column(
                    Modifier.weight(1f).clip(shape)
                        .border(1.dp, if (active) palette.accent else if (isDark) palette.ramp.border else Color(0xFFE5E7EB), shape)
                        .semantics { role = Role.RadioButton; stateDescription = if (active) "Selected" else "Not selected" }
                        .clickable { prefs.setSurface(isDark, choice) }.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(8.dp)).background(ramp.field).padding(6.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(6.dp)).background(ramp.card).border(1.dp, ramp.border, RoundedCornerShape(6.dp)))
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text(choice.label(isDark), fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 13.sp, color = strong(), modifier = Modifier.weight(1f))
                        if (active) Icon(Icons.Filled.CheckCircle, null, tint = palette.accent, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Text(
            if (isDark) "Applies to dark mode. Light mode keeps its own choice." else "Applies to light mode. Dark mode keeps its own choice.",
            fontSize = 12.sp, color = WebColor.gray500, modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )
    }

    // List layout (Android only in the web app).
    Column {
        Text("List layout", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = strong(), modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        val shape = RoundedCornerShape(8.dp)
        Column(Modifier.fillMaxWidth().clip(shape).background(palette.ramp.raised).border(1.dp, if (isDark) palette.ramp.border else Color(0xFFE5E7EB), shape)) {
            listOf(
                Triple(ListDisplayStyle.CARD, "Cards", "A two-column grid") to Icons.Outlined.GridView,
                Triple(ListDisplayStyle.LIST, "List", "One full-width row each") to Icons.Outlined.TableRows,
            ).forEachIndexed { index, (option, icon) ->
                val (style, title, hint) = option
                val selected = prefs.displayStyle == style
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(if (isDark) palette.ramp.border else Color(0xFFE5E7EB)))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp)
                        .semantics { role = Role.RadioButton; stateDescription = if (selected) "Selected" else "Not selected" }
                        .clickable { prefs.displayStyle = style }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = if (selected) palette.accent else muted(), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = strong())
                        Text(hint, fontSize = 14.sp, color = muted())
                    }
                    if (selected) Icon(Icons.Filled.CheckCircle, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationsSection() {
    val prefs = LocalPrefs.current
    val palette = LocalPalette.current
    var askPermission by remember { mutableStateOf(false) }
    if (askPermission) RequestNotificationPermission()

    SectionHeading("Notifications", "Choose what List It interrupts you for on this device.")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SwitchRow(Icons.Outlined.Notifications, palette.accent, "Task reminders", prefs.remindersEnabled, palette.accent) {
            prefs.remindersEnabled = !prefs.remindersEnabled
            if (prefs.remindersEnabled) askPermission = true
        }
        Text(
            "Turning this off keeps every reminder you have set, it just stops them being delivered. Reminders arrive through the mobile app, so this covers this device only.",
            fontSize = 14.sp, color = muted(),
        )
    }
}

@Composable
private fun SecuritySection(show: (Boolean, String) -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    SectionHeading("Security Settings", "Manage your password and security preferences")
    Column {
        FieldLabel("Current Password")
        InputField(current, { current = it }, "Enter current password", "Current Password", password = true, enabled = !saving)
    }
    Column {
        FieldLabel("New Password")
        InputField(new, { new = it }, "Enter new password", "New Password", password = true, enabled = !saving)
        Text(PasswordRules.RULE, fontSize = 12.sp, color = WebColor.gray500, modifier = Modifier.padding(top = 4.dp))
    }
    Column {
        FieldLabel("Confirm New Password")
        InputField(confirm, { confirm = it }, "Confirm new password", "Confirm New Password", password = true, enabled = !saving)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ActionButton("Update Password", Icons.Outlined.Lock, palette.accent, enabled = current.isNotEmpty() && new.isNotEmpty() && confirm.isNotEmpty(), busy = saving) {
            when {
                current.isEmpty() || new.isEmpty() || confirm.isEmpty() -> show(false, "All password fields are required")
                new != confirm -> show(false, "New passwords do not match")
                PasswordRules.error(new) != null -> show(false, PasswordRules.error(new)!!)
                else -> {
                    saving = true
                    scope.launch {
                        val error = db.changePassword(current, new)
                        saving = false
                        if (error != null) show(false, error) else {
                            show(true, "Password changed. Sign in again with your new password.")
                            delay(1_500)
                            db.signOutUser()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSection(show: (Boolean, String) -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var confirmation by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    val isDark = palette.isDark
    val title = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
    val body = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)

    SectionHeading("Account Management", "Manage your account and data deletion")
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.2f) else Color(0xFFFEF2F2))
            .border(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA), shape).padding(24.dp),
    ) {
        Icon(Icons.Outlined.WarningAmber, null, tint = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444), modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Delete Account", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = title)
            Text("Once you delete your account, there is no going back. Please be certain. This action will:", fontSize = 14.sp, color = body)
            listOf(
                "Permanently delete all your lists and collections",
                "Remove all your tasks and notes",
                "Delete your account and profile information",
            ).forEach { Text("•  $it", fontSize = 14.sp, color = body) }
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    append("Type "); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("DELETE") }; append(" to confirm")
                },
                fontSize = 14.sp, fontWeight = FontWeight.Medium, color = title,
            )
            InputField(confirmation, { confirmation = it }, "Type DELETE to confirm", "Type DELETE to confirm", enabled = !deleting, error = true)
            Box(Modifier.alpha(1f)) {
                ActionButton("Delete Account Permanently", Icons.Outlined.Delete, WebColor.red600, enabled = confirmation == "DELETE", busy = deleting) {
                    if (confirmation != "DELETE") { show(false, "Please type \"DELETE\" to confirm"); return@ActionButton }
                    deleting = true
                    scope.launch {
                        if (db.deleteAccount()) show(true, "Account deleted successfully. Signing you out...")
                        else show(false, db.alertMessage)
                        deleting = false
                    }
                }
            }
        }
    }
}
