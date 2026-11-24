package com.list_it.app.ui.screens.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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
import com.list_it.app.ui.components.SheetHeader
import com.list_it.app.ui.theme.IosColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/** The form for making a new list. Every new list starts with a "General" collection inside it. */
@Composable
fun ColumnScope.AddListSheet(lists: List<TodoList>, dismiss: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var listName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }

    /** Checks the name isn't empty, then saves the new list. */
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
            if (db.saveList(newList, general)) dismiss() else showAlert = true
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
