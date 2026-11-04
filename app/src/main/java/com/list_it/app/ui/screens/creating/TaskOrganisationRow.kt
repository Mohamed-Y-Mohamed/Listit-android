package com.list_it.app.ui.screens.creating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.list_it.app.model.TodoList
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.components.MenuItem
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette

/** Collection picker for new tasks and notes (iOS `TaskOrganisationRow`). */
@Composable
fun TaskOrganisationRow(list: TodoList, selectedCollectionID: String?, onSelect: (String) -> Unit) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Collection", style = IosFont.body, color = palette.primary, modifier = Modifier.weight(1f))
            Text(db.collections.firstOrNull { it.id == selectedCollectionID }?.collectionName ?: "General", style = IosFont.body, color = palette.secondary)
            Icon(Icons.Filled.UnfoldMore, null, tint = palette.secondary, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            db.collections.filter { it.listID == list.id }.forEach { c ->
                MenuItem(c.collectionName, Icons.Outlined.Folder) { open = false; onSelect(c.id) }
            }
        }
    }
}
