package com.list_it.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.colorFromHex

/** Shared "name + colour" edit form used by the Update List and Update Collection sheets. */
@Composable
fun ColumnScope.NameAndColourEditor(
    title: String,
    placeholder: String,
    initialName: String,
    initialHex: String,
    dismiss: () -> Unit,
    onSave: (name: String, hex: String, fail: () -> Unit) -> Unit,
) {
    val db = LocalDb.current
    var editedName by remember { mutableStateOf(initialName) }
    var editedHex by remember { mutableStateOf(initialHex) }
    var showAlert by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(colorFromHex(initialHex).copy(alpha = 0.08f))) {
        IosTopBar(
            title,
            leading = { BarButton("Dismiss", onClick = dismiss) },
            trailing = { BarButton("Update", bold = true) { onSave(editedName, editedHex) { showAlert = true } } },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PlainTextField(editedName, { editedName = it }, placeholder, IosFont.title3.copy(fontWeight = FontWeight.SemiBold), Modifier.fillMaxWidth())
            IosDivider()
            ColorInputView(editedHex) { editedHex = it }
        }
    }
    if (showAlert) IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
}
