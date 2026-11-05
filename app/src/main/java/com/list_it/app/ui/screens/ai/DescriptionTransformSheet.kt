package com.list_it.app.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.list_it.app.data.FoundationModelManager
import com.list_it.app.model.AIFeatureMode
import com.list_it.app.ui.components.AIOutputSection
import com.list_it.app.ui.components.AIPromptInput
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch

/** iOS `DescriptionTransformView`: rewrites, refines, summarises or custom-prompts a description. */
@Composable
fun ColumnScope.DescriptionTransformSheet(
    initialDescription: String,
    mode: AIFeatureMode,
    dismiss: () -> Unit,
    manager: FoundationModelManager = remember { FoundationModelManager() },
    onApply: ((String) -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    var customPromptInput by remember { mutableStateOf("") }

    val showsCustomInput = mode == AIFeatureMode.CUSTOM && manager.aiResponse.isEmpty() && !manager.isGenerating && manager.errorMessage == null
    val showsOutput = mode != AIFeatureMode.CUSTOM || manager.aiResponse.isNotEmpty() || manager.isGenerating || manager.errorMessage != null

    val runTransformation: () -> Unit = {
        scope.launch {
            when (mode) {
                AIFeatureMode.REWRITE_LIST -> manager.rewriteDescriptionAsList(initialDescription)
                AIFeatureMode.REFINE -> manager.refineDescription(initialDescription)
                AIFeatureMode.SUMMARISE -> manager.summarizeDescription(initialDescription)
                AIFeatureMode.CUSTOM -> if (customPromptInput.isNotEmpty()) manager.processCustomPrompt(initialDescription, customPromptInput)
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) { if (mode != AIFeatureMode.CUSTOM) runTransformation() }

    val headerTitle = when (mode) {
        AIFeatureMode.REWRITE_LIST -> "Formatted List Output"
        AIFeatureMode.REFINE -> "Refined Text Output"
        AIFeatureMode.SUMMARISE -> "Summary Output"
        AIFeatureMode.CUSTOM -> "Custom AI Output"
        else -> "AI Output"
    }

    IosTopBar(mode.title, leading = { BarButton("Dismiss", onClick = dismiss) })
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (showsCustomInput) {
            AIPromptInput(
                "Custom instructions", "e.g., Rewrite to be simpler", customPromptInput, { customPromptInput = it },
                "Run Custom Prompt", "Processing…", manager.isGenerating, runTransformation,
            )
        }
        if (showsOutput) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(headerTitle, style = IosFont.subheadline, fontWeight = FontWeight.SemiBold, color = LocalPalette.current.primary)
                AIOutputSection(
                    manager,
                    loadingText = "Processing text…",
                    applyTitle = onApply?.let { "Replace Description" },
                    onApply = onApply?.let { apply -> { apply(manager.aiResponse); dismiss() } },
                    onRegenerate = runTransformation,
                )
            }
        }
    }
}
