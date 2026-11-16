package com.list_it.app.ui.screens.collection

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.list_it.app.model.ListCollection
import com.list_it.app.ui.LocalDb
import kotlinx.coroutines.launch
import com.list_it.app.ui.components.NameAndColourEditor

/** Lets you rename a collection or change its colour. */
@Composable
fun ColumnScope.UpdateCollectionSheet(collection: ListCollection, dismiss: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    NameAndColourEditor(
        title = "Update Collection",
        placeholder = "Collection name",
        initialName = collection.collectionName,
        initialHex = collection.bgColorHex,
        dismiss = dismiss,
    ) { name, hex, fail ->
        if (name.isBlank()) {
            db.setError("Required", "Your Collection must have a name.")
            fail()
        } else scope.launch { if (db.updateCollection(collection, name, hex)) dismiss() else fail() }
    }
}
