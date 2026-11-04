package com.list_it.app.ui.screens.creating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.list_it.app.model.Note
import com.list_it.app.model.TodoList
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.AIGenerateButton
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.ColorInputView
import com.list_it.app.ui.components.DescriptionEditor
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.PlainTextField
import com.list_it.app.ui.components.ToggleRow
import com.list_it.app.ui.screens.ai.CreateNoteAISheet
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/** iOS `AddNoteView`. */
@Composable
fun ColumnScope.AddNoteSheet(list: TodoList, dismiss: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var noteTitle by remember { mutableStateOf("") }
    var noteDescription by remember { mutableStateOf("") }
    var selectedCollectionID by remember { mutableStateOf<String?>(null) }
    var isPinned by remember { mutableStateOf(false) }
    var selectedColorHex by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }
    var showAIView by remember { mutableStateOf(false) }

    fun createNote() {
        if (noteTitle.isBlank()) {
            db.setError("Required", "Please enter a title for your note.")
            showAlert = true
            return
        }
        val target = db.collections.firstOrNull { it.id == selectedCollectionID }
            ?: db.collections.firstOrNull { it.listID == list.id && it.collectionName == "General" }
        val finalHex = selectedColorHex.ifEmpty { target?.bgColorHex ?: "#808080" }
        val newNote = Note(
            id = UUID.randomUUID().toString().lowercase(), createdAt = Instant.now(), title = noteTitle,
            description = noteDescription.ifEmpty { null }, isDeleted = false, bgColorHex = finalHex, isPinned = isPinned,
            collectionID = target?.id.orEmpty(), userID = "", listID = list.id,
        )
        scope.launch {
            if (db.saveNote(newNote)) {
                db.notes = db.notes + newNote
                dismiss()
            } else showAlert = true
        }
    }

    IosTopBar(
        "New Note",
        leading = { BarButton("Dismiss", onClick = dismiss) },
        trailing = { BarButton("Create", bold = true) { createNote() } },
    )
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SheetHeader(Icons.Filled.NoteAdd, IosColor.yellow, null)
        IosDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlainTextField(noteTitle, { noteTitle = it }, "Note title", IosFont.title3.copy(fontWeight = FontWeight.SemiBold), Modifier.fillMaxWidth())
            DescriptionEditor(noteDescription, { noteDescription = it }, "Description (optional)", minHeight = 100, fontSize = 17, dimmed = true)
            AIGenerateButton { showAIView = true }
        }
        IosDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorInputView(selectedColorHex) { selectedColorHex = it }
            Text("If no color is selected, the Note will use its Collection's colour by default.", style = IosFont.caption, color = LocalPalette.current.secondary)
        }
        IosDivider()
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TaskOrganisationRow(list, selectedCollectionID) { selectedCollectionID = it }
            ToggleRow(isPinned, { isPinned = it }, "Pin Note", if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, tint = IosColor.orange)
        }
    }

    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    if (showAIView) {
        IosSheet(onDismiss = { showAIView = false }) { close ->
            CreateNoteAISheet(close) { generated ->
                noteDescription += (if (noteDescription.isEmpty()) "" else "\n\n") + generated
            }
        }
    }
}
