package com.list_it.app.ui.screens.creating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.list_it.app.ui.theme.colorFromHex
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/** iOS `AddCollectionView`. */
@Composable
fun ColumnScope.AddCollectionSheet(list: TodoList, dismiss: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var collectionName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }

    fun createCollection() {
        if (collectionName.isEmpty()) {
            db.setError("Error", "Please enter a name for your new Collection")
            showAlert = true
            return
        }
        val newName = collectionName.trim().lowercase()
        if (db.collections.any { it.listID == list.id && it.collectionName.trim().lowercase() == newName }) {
            db.setError("Error", "A Collection with this name already exists within this List.")
            showAlert = true
            return
        }
        val finalColorHex = selectedColorHex.ifEmpty { list.bgColorHex }
        val newCollection = ListCollection(UUID.randomUUID().toString().lowercase(), Instant.now(), collectionName, finalColorHex, list.id, "")
        scope.launch {
            if (!db.saveCollection(newCollection)) {
                showAlert = true
                return@launch
            }
            if (db.fetchUserCollections()) dismiss() else showAlert = true
        }
    }

    Column(Modifier.fillMaxSize().background(colorFromHex(list.bgColorHex).copy(alpha = 0.08f))) {
        IosTopBar(
            "Add Collection",
            leading = { BarButton("Dismiss", onClick = dismiss) },
            trailing = { BarButton("Create", bold = true, enabled = collectionName.isNotEmpty()) { createCollection() } },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SheetHeader(Icons.Filled.CreateNewFolder, IosColor.blue, "Group your tasks & notes into collections.")
            IosDivider()
            InputField(Icons.Outlined.Folder, collectionName, { collectionName = it }, "e.g. Groceries, High Priority")
            IosDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorInputView(selectedColorHex) { selectedColorHex = it }
                Text(
                    buildAnnotatedString {
                        append("If no color is selected, this will inherit the ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(list.listName) }
                        append(" list colour.")
                    },
                    style = IosFont.caption2, color = LocalPalette.current.secondary,
                )
            }
        }
    }
    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
}
