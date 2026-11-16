package com.list_it.app.ui.screens.list

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.list_it.app.model.TodoList
import com.list_it.app.ui.LocalDb
import kotlinx.coroutines.launch
import com.list_it.app.ui.components.NameAndColourEditor

/** Lets you rename a list or change its colour. */
@Composable
fun ColumnScope.UpdateListSheet(list: TodoList, dismiss: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    NameAndColourEditor(
        title = "Update List",
        placeholder = "List name",
        initialName = list.listName,
        initialHex = list.bgColorHex,
        dismiss = dismiss,
    ) { name, hex, fail ->
        if (name.isBlank()) {
            db.setError("Required", "Your List must have a name.")
            fail()
        } else scope.launch { if (db.updateList(list, name, hex)) dismiss() else fail() }
    }
}
