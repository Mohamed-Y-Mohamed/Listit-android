package com.list_it.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.list_it.app.ui.Dates
import com.list_it.app.ui.components.InfoRow
import com.list_it.app.ui.components.SectionLabel
import com.list_it.app.ui.components.ViewColor
import com.list_it.app.ui.components.ViewFooterButton
import com.list_it.app.ui.theme.WebColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.AIFeatureMode
import com.list_it.app.model.AlertMode
import com.list_it.app.model.Note
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.AIMenuView
import com.list_it.app.ui.components.AlertButton
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.ColorInputView
import com.list_it.app.ui.components.DescriptionEditor
import com.list_it.app.ui.components.DescriptionModeSwitcher
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.MarkdownText
import com.list_it.app.ui.components.PlainTextField
import com.list_it.app.ui.components.TintedButton
import com.list_it.app.ui.components.rememberShareAction
import com.list_it.app.ui.screens.ai.DescriptionTransformSheet
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.colorFromHex
import kotlinx.coroutines.launch

/** iOS `NoteDetailView`: edit, recolour, pin, share or delete a note. */
@Composable
fun ColumnScope.NoteDetailSheet(note: Note, dismiss: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val share = rememberShareAction()

    var editedTitle by remember { mutableStateOf(note.title) }
    var editedDescription by remember { mutableStateOf(note.description.orEmpty()) }
    var editedHex by remember { mutableStateOf(note.bgColorHex) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var showAlert by remember { mutableStateOf(false) }
    var alertMode by remember { mutableStateOf(AlertMode.ERROR) }
    var isMarkdownPreview by remember { mutableStateOf(false) }
    var activeAIMode by remember { mutableStateOf<AIFeatureMode?>(null) }
    // Opens to read the note; Edit swaps in the form (web notedetail).
    var isEditing by remember { mutableStateOf(false) }
    val live = db.notes.firstOrNull { it.id == note.id } ?: note

    val collection = db.collections.firstOrNull { it.id == note.collectionID }
    val accent = collection?.let { colorFromHex(it.bgColorHex) } ?: IosColor.orange
    val shareText = "${note.title}\n\n${note.description.orEmpty()}"

    fun saveChanges() {
        if (editedTitle.isBlank()) {
            db.setError("Required", "Your note must have a title.")
            alertMode = AlertMode.ERROR; showAlert = true
            return
        }
        scope.launch {
            if (db.updateNote(note, editedTitle, editedDescription, editedHex, isPinned)) isEditing = false
            else { alertMode = AlertMode.ERROR; showAlert = true }
        }
    }

    fun cancelEdit() {
        editedTitle = live.title
        editedDescription = live.description.orEmpty()
        editedHex = live.bgColorHex
        isPinned = live.isPinned
        isEditing = false
    }

    fun togglePin() = scope.launch {
        if (db.updatePinForNote(live, !live.isPinned)) isPinned = !live.isPinned else { alertMode = AlertMode.ERROR; showAlert = true }
    }

    fun performDelete() = scope.launch {
        if (db.deleteNote(note)) dismiss() else { alertMode = AlertMode.ERROR; showAlert = true }
    }

    if (!isEditing) {
        NoteView(
            note = live, onDismiss = dismiss, onEdit = { isEditing = true }, onTogglePin = { togglePin() },
            onDelete = { alertMode = AlertMode.DELETE; showAlert = true },
            onShare = { share(live.title, "${live.title}\n\n${live.description.orEmpty()}") },
        )
    } else Column(Modifier.fillMaxSize().background(colorFromHex(editedHex).copy(alpha = 0.08f))) {
        IosTopBar(
            "Edit Note",
            leading = { BarButton("Cancel") { cancelEdit() } },
            trailing = { BarButton("Save", bold = true) { saveChanges() } },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PlainTextField(editedTitle, { editedTitle = it }, "Note title", IosFont.title3.copy(fontWeight = FontWeight.SemiBold), Modifier.fillMaxWidth())
            IosDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DescriptionModeSwitcher(isMarkdownPreview, { isMarkdownPreview = it }, accent)
                    Spacer(Modifier.weight(1f))
                    AIMenuView(editedDescription) { activeAIMode = it }
                }
                if (isMarkdownPreview) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 100.dp).padding(top = 8.dp, start = 5.dp)) {
                        if (editedDescription.isEmpty()) Text("Nothing to preview", fontSize = 13.sp, color = palette.secondary)
                        else MarkdownText(editedDescription, fontSize = 13)
                    }
                } else {
                    DescriptionEditor(editedDescription, { editedDescription = it }, "Add a description", minHeight = 100)
                }
            }
            IosDivider()
            ColorInputView(editedHex) { editedHex = it }
            IosDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TintedButton(if (isPinned) "Unpin" else "Pin", if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, IosColor.orange, Modifier.weight(1f)) {
                    isPinned = !isPinned
                }
                TintedButton("Delete", Icons.Filled.Delete, IosColor.red, Modifier.weight(1f)) {
                    alertMode = AlertMode.DELETE; showAlert = true
                }
                TintedButton("Share", Icons.Filled.Share, IosColor.blue, Modifier.weight(1f)) {
                    share(editedTitle, shareText)
                }
            }
        }
    }

    if (showAlert) {
        if (alertMode == AlertMode.DELETE) {
            IosAlert(
                "Delete Note?", "This action cannot be undone.", onDismiss = { showAlert = false },
                buttons = listOf(AlertButton("Cancel", cancel = true), AlertButton("Delete", destructive = true) { performDelete() }),
            )
        } else IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    }
    activeAIMode?.let { mode ->
        IosSheet(onDismiss = { activeAIMode = null }) { close ->
            DescriptionTransformSheet(editedDescription, mode, close) { editedDescription = it }
        }
    }
}

/** Read-only note view (web notedetail view mode): Dismiss | Edit, the note, then Pin, Delete, Share. */
@Composable
private fun ColumnScope.NoteView(note: Note, onDismiss: () -> Unit, onEdit: () -> Unit, onTogglePin: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val muted = if (palette.isDark) WebColor.gray400 else WebColor.gray500
    val color = colorFromHex(note.bgColorHex)
    Column(Modifier.fillMaxSize().background(color.copy(alpha = 0.08f))) {
        IosTopBar(
            "Note Details",
            leading = { BarButton("Dismiss", onClick = onDismiss) },
            trailing = { BarButton("Edit", bold = true, onClick = onEdit) },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(width = 4.dp, height = 16.dp).clip(CircleShape).background(if (note.isPinned) ViewColor.warning else color))
                    Spacer(Modifier.width(8.dp))
                    Text(note.title.ifEmpty { "Untitled note" }, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, color = palette.primary)
                }
                Text("Created ${Dates.dayMonthYear(note.createdAt)}", fontSize = 13.sp, color = muted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Note")
                val description = note.description.orEmpty()
                if (description.isNotBlank()) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (palette.isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f))
                            .border(1.dp, palette.ramp.border, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    ) { MarkdownText(description, fontSize = 14) }
                } else {
                    Text("Nothing written yet", fontSize = 14.sp, color = muted)
                }
            }
            Column {
                IosDivider()
                Spacer(Modifier.height(12.dp))
                InfoRow("Collection", db.collections.firstOrNull { it.id == note.collectionID }?.collectionName)
                InfoRow("List", db.lists.firstOrNull { it.id == note.listID }?.listName)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ViewFooterButton(if (note.isPinned) "Unpin" else "Pin", if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, ViewColor.warning, onClick = onTogglePin)
            ViewFooterButton("Delete", Icons.Filled.Delete, ViewColor.danger, onClick = onDelete)
            Spacer(Modifier.weight(1f))
            ViewFooterButton("Share", Icons.Filled.Share, IosColor.blue, onClick = onShare)
        }
    }
}
