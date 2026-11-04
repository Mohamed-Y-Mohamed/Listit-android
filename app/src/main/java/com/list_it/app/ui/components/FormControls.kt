package com.list_it.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex

/**
 * Primary action for the onboarding and auth screens, in the web app's Capacitor style:
 * solid orange, 50dp, 14dp corners, a slight press-in. Shows a spinner while [loading].
 */
@Composable
fun ButtonView(
    buttonTxt: String,
    showArrow: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (pressed) AuthColor.orange600 else AuthColor.orange500)
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(buttonTxt, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        if (showArrow && !loading) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Colours from the web app's native auth styles (`authStyles.ts`). */
object AuthColor {
    val orange500 = Color(0xFFF97316)
    val orange600 = Color(0xFFEA580C)
    val orange400 = Color(0xFFFB923C)
}

/** Auth text field in the web app's Capacitor style: 50dp, 14dp corners, icon inside, orange focus border. */
@Composable
fun CustomTextField(
    icon: ImageVector,
    placeholder: String,
    text: String,
    onTextChange: (String) -> Unit,
    isPassword: Boolean,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val palette = LocalPalette.current
    var showPassword by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val fill = if (palette.isDark) Color(0xFF1F2937) else Color.White
    val border = when {
        isEditing -> AuthColor.orange500
        palette.isDark -> Color(0xFF374151)
        else -> Color(0xFFE5E7EB)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (isEditing) AuthColor.orange500 else Color(0xFF6B7280), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Text(placeholder, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280), modifier = Modifier.clearAndSetSemantics {})
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                singleLine = true,
                textStyle = IosFont.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (palette.isDark) Color.White else Color(0xFF111827)),
                cursorBrush = SolidColor(AuthColor.orange500),
                visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                ),
                modifier = Modifier.fillMaxWidth().onFocusChanged { isEditing = it.isFocused }.semantics { contentDescription = placeholder },
            )
        }
        if (isPassword) {
            IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(32.dp)) {
                Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (showPassword) "Hide password" else "Show password", tint = Color(0xFF6B7280), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Icon + text field row (iOS `InputField`). */
@Composable
fun InputField(icon: ImageVector, text: String, onTextChange: (String) -> Unit, placeholder: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = LocalPalette.current.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        PlainTextField(text, onTextChange, placeholder, IosFont.body.copy(fontWeight = FontWeight.Medium), Modifier.weight(1f))
    }
}

/** Multi-line description editor with placeholder (iOS `TextEditor` + overlay text). */
@Composable
fun DescriptionEditor(text: String, onTextChange: (String) -> Unit, placeholder: String, minHeight: Int, fontSize: Int = 13, dimmed: Boolean = false) {
    val palette = LocalPalette.current
    Box(Modifier.fillMaxWidth().heightIn(min = minHeight.dp).padding(top = 8.dp, start = 5.dp)) {
        if (text.isEmpty()) Text(placeholder, fontSize = fontSize.sp, color = palette.secondary, modifier = Modifier.clearAndSetSemantics {})
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = IosFont.body.copy(fontSize = fontSize.sp, lineHeight = (fontSize + 5).sp, color = if (dimmed) palette.secondary else palette.primary),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp).semantics { contentDescription = placeholder },
        )
    }
}

/** "Edit" / "Markdown Preview" tabs (iOS `DescriptionModeSwitcher`). */
@Composable
fun DescriptionModeSwitcher(isPreview: Boolean, onChange: (Boolean) -> Unit, accent: Color = IosColor.orange) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        UnderlineTab("Edit", !isPreview, accent) { onChange(false) }
        UnderlineTab("Markdown Preview", isPreview, accent) { onChange(true) }
    }
}

@Composable
fun UnderlineTab(title: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.width(IntrinsicSize.Max).clickable(interactionSource = null, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title, style = IosFont.subheadline,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) palette.primary else palette.secondary,
        )
        Box(Modifier.height(2.dp).fillMaxWidth().background(if (selected) accent else Color.Transparent))
    }
}
