package com.list_it.app.ui.screens.creating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.list_it.app.model.ListCollection
import com.list_it.app.model.TodoList
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.BarButton
import com.list_it.app.ui.components.ColorInputView
import com.list_it.app.ui.components.InputField
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/** iOS `AddListView`: creates a List together with its "General" collection. */
@Composable
fun ColumnScope.AddListSheet(lists: List<TodoList>, dismiss: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var listName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }

    fun createList() {
        if (listName.isEmpty() || selectedColorHex.isEmpty()) {
            db.setError("Error", "Please enter a name and choose a color.")
            showAlert = true
            return
        }
        if (lists.any { it.listName.lowercase().trim() == listName.lowercase().trim() }) {
            db.setError("Error", "A list with this name already exists.")
            showAlert = true
            return
        }
        val listID = UUID.randomUUID().toString().lowercase()
        val newList = TodoList(listID, Instant.now(), "checklist", listName, false, selectedColorHex, "", false)
        val general = ListCollection(UUID.randomUUID().toString().uppercase(), Instant.now(), "General", selectedColorHex, listID, "")
        scope.launch {
            if (!db.saveList(newList, general)) {
                showAlert = true
                return@launch
            }
            db.lists = db.lists + newList.copy(userId = db.currentUser?.id.orEmpty())
            db.collections = db.collections + general.copy(userID = db.currentUser?.id.orEmpty())
            dismiss()
        }
    }

    IosTopBar(
        "Add List",
        leading = { BarButton("Dismiss", onClick = dismiss) },
        trailing = { BarButton("Create", bold = true, enabled = listName.isNotEmpty()) { createList() } },
    )
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SheetHeader(Icons.Filled.Checklist, IosColor.orange, "Create a new List to stay organised.")
        IosDivider()
        InputField(Icons.Filled.Edit, listName, { listName = it }, "e.g. Personal, Work, Fitness")
        IosDivider()
        ColorInputView(selectedColorHex) { selectedColorHex = it }
    }
    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
}

/** Centered icon and subtitle at the top of the create sheets. */
@Composable
internal fun SheetHeader(icon: ImageVector, tint: Color, subtitle: String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(40.dp))
        subtitle?.let { Text(it, style = IosFont.subheadline, color = LocalPalette.current.secondary, textAlign = TextAlign.Center) }
    }
}
