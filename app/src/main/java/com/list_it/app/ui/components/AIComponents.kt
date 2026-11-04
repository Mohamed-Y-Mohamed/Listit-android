package com.list_it.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.list_it.app.data.FoundationModelManager
import com.list_it.app.data.OnDeviceModel
import com.list_it.app.data.strippingMarkdown
import com.list_it.app.model.AIFeatureMode
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.delay

/** Whether on-device AI can run on this device (iOS `#available(iOS 26)` + model availability). */
@Composable
fun rememberAiAvailable(): Boolean = produceState(false) { value = OnDeviceModel.isAvailable() }.value

@Composable
fun AICard(content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .background(if (palette.isDark) palette.secondaryGroupedBackground else Color.White, shape)
            .border(0.8.dp, palette.primary.copy(alpha = if (palette.isDark) 0.12f else 0.08f), shape)
            .padding(14.dp),
    ) { content() }
}

@Composable
fun AIPromptInput(
    heading: String,
    placeholder: String,
    text: String,
    onTextChange: (String) -> Unit,
    buttonTitle: String,
    busyTitle: String,
    isBusy: Boolean,
    action: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(heading, style = IosFont.subheadline, fontWeight = FontWeight.SemiBold, color = LocalPalette.current.primary)
        AICard {
            PlainTextField(text, onTextChange, placeholder, IosFont.body, Modifier.fillMaxWidth().heightIn(min = 66.dp), singleLine = false)
        }
        TintedButton(
            text = if (isBusy) busyTitle else buttonTitle,
            icon = if (isBusy) null else Icons.Filled.AutoAwesome,
            tint = IosColor.orange,
            prominent = true,
            large = true,
            enabled = text.isNotBlank() && !isBusy,
            modifier = Modifier.fillMaxWidth(),
            leading = if (isBusy) ({ CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) }) else null,
            onClick = action,
        )
    }
}

@Composable
fun AIOutputSection(
    manager: FoundationModelManager,
    loadingText: String? = null,
    applyTitle: String? = null,
    onApply: (() -> Unit)? = null,
    onRegenerate: () -> Unit,
) {
    val palette = LocalPalette.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var didCopy by remember { mutableStateOf(false) }
    LaunchedEffect(didCopy) {
        if (didCopy) { delay(2500); didCopy = false }
    }
    val error = manager.errorMessage
    when {
        manager.isGenerating -> if (loadingText != null) {
            Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(loadingText, style = IosFont.subheadline, color = palette.secondary)
            }
        }
        error != null -> Text(error, style = IosFont.subheadline, color = IosColor.red)
        manager.aiResponse.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AICard { SelectionContainer { MarkdownText(manager.aiResponse) } }
            if (applyTitle != null && onApply != null) {
                TintedButton(applyTitle, Icons.Filled.Download, IosColor.orange, Modifier.fillMaxWidth(), prominent = true, large = true, onClick = onApply)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TintedButton(
                    if (didCopy) "Copied" else "Copy",
                    if (didCopy) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    if (didCopy) IosColor.green else IosColor.orange,
                    Modifier.weight(1f),
                ) {
                    val text = manager.aiResponse.strippingMarkdown()
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("AI response", text))) }
                    didCopy = true
                }
                TintedButton("Regenerate", Icons.Filled.Refresh, IosColor.orange, Modifier.weight(1f)) {
                    didCopy = false
                    onRegenerate()
                }
            }
        }
    }
}

/** "Write with AI" capsule, shown only when on-device AI is available (iOS `AIGenerateButton`). */
@Composable
fun AIGenerateButton(title: String = "Write with AI", action: () -> Unit) {
    if (rememberAiAvailable()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TintedButton(title, Icons.Filled.AutoAwesome, IosColor.orange, capsule = true, small = true, onClick = action)
        }
    }
}

/** "AI Features" menu above a description (iOS `AIMenuView`). */
@Composable
fun AIMenuView(editedDescription: String, onSelect: (AIFeatureMode) -> Unit) {
    if (!rememberAiAvailable()) return
    val enabled = editedDescription.isNotBlank()
    IosMenu(
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.Filled.AutoAwesome, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("AI Features", style = IosFont.subheadline, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
        },
    ) { close ->
        MenuItem("Rewrite as List", Icons.AutoMirrored.Filled.FormatListBulleted, enabled = enabled) { close(); onSelect(AIFeatureMode.REWRITE_LIST) }
        MenuItem("Refine Description", Icons.Filled.AutoAwesome, enabled = enabled) { close(); onSelect(AIFeatureMode.REFINE) }
        MenuItem("Summarise description", Icons.Filled.Summarize, enabled = enabled) { close(); onSelect(AIFeatureMode.SUMMARISE) }
        MenuItem("Custom Prompt", Icons.Outlined.ChatBubbleOutline, enabled = enabled) { close(); onSelect(AIFeatureMode.CUSTOM) }
    }
}
