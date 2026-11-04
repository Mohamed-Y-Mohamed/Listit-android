package com.list_it.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.appBackground
import kotlinx.coroutines.launch

// ---------- Navigation bar ----------

/** Inline navigation bar (iOS `.navigationBarTitleDisplayMode(.inline)`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosTopBar(
    title: String,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = IosFont.headline, maxLines = 1) },
        navigationIcon = { Row(verticalAlignment = Alignment.CenterVertically, content = leading) },
        actions = trailing,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
    )
}

/** Plain text toolbar button such as "Dismiss", "Save" or "Create". */
@Composable
fun BarButton(text: String, bold: Boolean = false, enabled: Boolean = true, color: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text,
            style = IosFont.body,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) color else LocalPalette.current.tertiary,
        )
    }
}

// ---------- Sheets ----------

/**
 * A sheet (iOS `.sheet`). [medium] opens at half height first (iOS `.medium` detent).
 * [dismissible] = false matches `.interactiveDismissDisabled()`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosSheet(
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    medium: Boolean = false,
    background: Color? = null,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    var allowHide by remember { mutableStateOf(false) }
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = !medium,
        confirmValueChange = { it != SheetValue.Hidden || dismissible || allowHide },
    )
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        allowHide = true
        scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = { if (dismissible || allowHide) onDismiss() },
        sheetState = state,
        containerColor = background ?: appBackground,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = dismissible),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight()) { content(dismiss) }
    }
}

// ---------- Alerts ----------

data class AlertButton(val text: String, val destructive: Boolean = false, val cancel: Boolean = false, val action: () -> Unit = {})

/** iOS `.alert`. Buttons appear right to left like the system dialog. */
@Composable
fun IosAlert(title: String, message: String, onDismiss: () -> Unit, buttons: List<AlertButton> = listOf(AlertButton("OK", cancel = true))) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = IosFont.headline) },
        text = { if (message.isNotEmpty()) Text(message, style = IosFont.footnote) },
        confirmButton = {
            Row {
                buttons.forEach { b ->
                    TextButton(onClick = { onDismiss(); b.action() }) {
                        Text(
                            b.text,
                            color = if (b.destructive) IosColor.red else MaterialTheme.colorScheme.primary,
                            fontWeight = if (b.cancel) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
    )
}

// ---------- Menus ----------

/** Content that opens a menu on long press (iOS `.contextMenu`) and reacts to taps. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContextMenuBox(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)?,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.combinedClickable(onClick = onClick, onLongClick = { if (menu != null) open = true })) {
        content()
        if (menu != null) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) { menu { open = false } }
        }
    }
}

/** A tap-to-open menu (iOS `Menu`). */
@Composable
fun IosMenu(label: @Composable () -> Unit, items: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).clickable { open = true }) { label() }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) { items { open = false } }
    }
}

@Composable
fun MenuItem(
    text: String,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    checked: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) IosColor.red else LocalPalette.current.primary
    DropdownMenuItem(
        text = { Text(text, color = if (enabled) color else LocalPalette.current.tertiary) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = if (checked) ({ Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }) else null,
        trailingIcon = icon?.let { { Icon(it, null, tint = if (enabled) color else LocalPalette.current.tertiary, modifier = Modifier.size(20.dp)) } },
    )
}

@Composable
fun MenuDivider() = HorizontalDivider(Modifier.padding(vertical = 4.dp), color = LocalPalette.current.separator)

// ---------- Content ----------

/** iOS `ContentUnavailableView`. */
@Composable
fun ContentUnavailable(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = palette.secondary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, style = IosFont.title3, fontWeight = FontWeight.Bold, color = palette.primary)
        description?.let { Text(it, style = IosFont.subheadline, color = palette.secondary, textAlign = TextAlign.Center) }
        actions?.let { Spacer(Modifier.height(8.dp)); it() }
    }
}

/** iOS `.searchable` search field. */
@Composable
fun SearchField(text: String, onTextChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (palette.isDark) palette.fill else palette.ramp.card)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = palette.secondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Text(placeholder, style = IosFont.body, color = palette.secondary, modifier = Modifier.clearAndSetSemantics {})
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = IosFont.body.copy(color = palette.primary),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
        if (text.isNotEmpty()) {
            Icon(
                Icons.Filled.Cancel, "Clear", tint = palette.secondary,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onTextChange("") },
            )
        }
    }
}

/** Single-line text field without decoration (iOS plain `TextField`). */
@Composable
fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholderColor: Color = LocalPalette.current.tertiary,
    textColor: Color = LocalPalette.current.primary,
) {
    Box(modifier) {
        if (value.isEmpty()) Text(placeholder, style = style, color = placeholderColor, modifier = Modifier.clearAndSetSemantics {})
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = style.copy(color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        )
    }
}

@Composable
fun IosDivider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 0.5.dp, color = LocalPalette.current.separator)

// ---------- Buttons ----------

/** iOS `.buttonStyle(.bordered)` / `.borderedProminent` with a tint. */
@Composable
fun TintedButton(
    text: String,
    icon: ImageVector?,
    tint: Color,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true,
    large: Boolean = false,
    capsule: Boolean = false,
    small: Boolean = false,
    textStyle: TextStyle = if (small) IosFont.caption.copy(fontWeight = FontWeight.SemiBold) else IosFont.subheadline,
    minHeight: Dp = if (large) 50.dp else if (small) 28.dp else 36.dp,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val bg = if (prominent) tint else tint.copy(alpha = 0.15f)
    val fg = if (prominent) Color.White else tint
    val shape = if (capsule) CircleShape else RoundedCornerShape(if (large) 14.dp else 10.dp)
    Row(
        modifier
            .clip(shape)
            .background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = minHeight)
            .padding(horizontal = if (small) 10.dp else 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading(); Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, null, tint = if (enabled) fg else fg.copy(alpha = 0.5f), modifier = Modifier.size(if (small) 14.dp else 18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = textStyle, color = if (enabled) fg else fg.copy(alpha = 0.5f), maxLines = 1)
    }
}

/** iOS `Toggle` row with a label and green switch. */
@Composable
fun ToggleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, icon: ImageVector, tint: Color = IosColor.green) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = IosFont.body, color = LocalPalette.current.primary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = tint, checkedThumbColor = Color.White, checkedBorderColor = tint),
        )
    }
}

/** Small count badge in a capsule. */
@Composable
fun CountBadge(text: String, background: Color, color: Color = Color.White, style: TextStyle = IosFont.caption2.copy(fontWeight = FontWeight.Bold), padding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
    Text(text, style = style, color = color, modifier = Modifier.clip(CircleShape).background(background).padding(padding))
}
