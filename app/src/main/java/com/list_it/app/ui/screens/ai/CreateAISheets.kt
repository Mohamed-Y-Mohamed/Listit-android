package com.list_it.app.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.list_it.app.data.FoundationModelManager
import com.list_it.app.model.AIFeatureMode
import com.list_it.app.ui.components.AIOutputSection
import com.list_it.app.ui.components.AIPromptInput
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.SegmentedControl
import kotlinx.coroutines.launch

/** iOS `CreateTaskAIView`: breaks a task down or builds a deep plan. */
@Composable
fun ColumnScope.CreateTaskAISheet(
    dismiss: () -> Unit,
    manager: FoundationModelManager = remember { FoundationModelManager() },
    onInsert: ((String) -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AIFeatureMode.BREAKDOWN) }
    val trigger: () -> Unit = { scope.launch { manager.generateSmartContent(input, mode) } }

    AISheetLayout(
        dismiss = dismiss,
        modes = listOf(AIFeatureMode.BREAKDOWN, AIFeatureMode.PLANNER),
        mode = mode,
        onMode = { mode = it },
        heading = if (mode == AIFeatureMode.BREAKDOWN) "What do you need broken down?" else "Describe the deep plan/routine you want built",
        placeholder = if (mode == AIFeatureMode.BREAKDOWN) "e.g., Clean the messy kitchen" else "e.g., Full body workout focusing on belly fat loss",
        buttonTitle = if (mode == AIFeatureMode.BREAKDOWN) "Generate Subtasks" else "Generate Deep Plan",
        busyTitle = "Analysing & structuring…",
        applyTitle = onInsert?.let { "Insert into Description" },
        manager = manager,
        input = input,
        onInput = { input = it },
        trigger = trigger,
        onApply = { onInsert?.invoke(manager.aiResponse); dismiss() },
    )
}

/** iOS `CreateNoteAIView`: outline, template or brainstorm for a note. */
@Composable
fun ColumnScope.CreateNoteAISheet(
    dismiss: () -> Unit,
    manager: FoundationModelManager = remember { FoundationModelManager() },
    onInsert: ((String) -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AIFeatureMode.NOTE_OUTLINE) }
    val trigger: () -> Unit = {
        scope.launch {
            when (mode) {
                AIFeatureMode.NOTE_OUTLINE -> manager.generateNoteOutline(input)
                AIFeatureMode.NOTE_TEMPLATE -> manager.generateSmartTemplate(input)
                AIFeatureMode.NOTE_BRAINSTORM -> manager.brainstormAndIdeate(input)
                else -> Unit
            }
        }
    }

    AISheetLayout(
        dismiss = dismiss,
        modes = listOf(AIFeatureMode.NOTE_OUTLINE, AIFeatureMode.NOTE_TEMPLATE, AIFeatureMode.NOTE_BRAINSTORM),
        mode = mode,
        onMode = { mode = it },
        heading = when (mode) {
            AIFeatureMode.NOTE_OUTLINE -> "What topic do you want an outline for?"
            AIFeatureMode.NOTE_TEMPLATE -> "What kind of template do you need?"
            else -> "What is your creative spark or topic?"
        },
        placeholder = when (mode) {
            AIFeatureMode.NOTE_OUTLINE -> "e.g., History of the UK"
            AIFeatureMode.NOTE_TEMPLATE -> "e.g., 1-on-1 meeting with manager"
            else -> "e.g., Feature ideas for a habit tracker app"
        },
        buttonTitle = when (mode) {
            AIFeatureMode.NOTE_OUTLINE -> "Generate Outline"
            AIFeatureMode.NOTE_TEMPLATE -> "Create Template"
            else -> "Brainstorm Ideas"
        },
        busyTitle = "Drafting note…",
        applyTitle = onInsert?.let { "Insert into Note" },
        manager = manager,
        input = input,
        onInput = { input = it },
        trigger = trigger,
        onApply = { onInsert?.invoke(manager.aiResponse); dismiss() },
    )
}

@Composable
private fun AISheetLayout(
    dismiss: () -> Unit,
    modes: List<AIFeatureMode>,
    mode: AIFeatureMode,
    onMode: (AIFeatureMode) -> Unit,
    heading: String,
    placeholder: String,
    buttonTitle: String,
    busyTitle: String,
    applyTitle: String?,
    manager: FoundationModelManager,
    input: String,
    onInput: (String) -> Unit,
    trigger: () -> Unit,
    onApply: () -> Unit,
) {
    // Shown while regenerating or downloading the model too, so the result area never goes blank.
    val hasOutput = manager.aiResponse.isNotEmpty() || manager.errorMessage != null || manager.isPreparingModel
    IosTopBar("AI Assistant", leading = { BarButton("Dismiss", onClick = dismiss) })
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // A result for one mode is meaningless under another, so switching clears it.
        SegmentedControl(modes, mode, { it.title }) { if (it != mode) { manager.reset(); onMode(it) } }
        IosDivider()
        AIPromptInput(heading, placeholder, input, onInput, buttonTitle, busyTitle, manager.isGenerating, trigger)
        if (hasOutput) {
            IosDivider()
            AIOutputSection(manager, applyTitle = applyTitle, onApply = if (applyTitle != null) onApply else null, onRegenerate = trigger)
        }
    }
}
