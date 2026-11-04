package com.list_it.app.ui.screens.defaultlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.list_it.app.model.ToDoTask
import com.list_it.app.ui.components.BackButton
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.SearchField
import com.list_it.app.ui.screens.detail.TaskDetailSheet
import com.list_it.app.ui.theme.appBackground

/** Shared layout for the default list screens: nav bar, search field and scrolling content. */
@Composable
internal fun DefaultListScaffold(
    title: String,
    onBack: () -> Unit,
    searchText: String?,
    onSearch: (String) -> Unit = {},
    searchPrompt: String = "",
    trailing: @Composable RowScope.() -> Unit = {},
    selectedTask: ToDoTask?,
    onTaskDismiss: () -> Unit,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = appBackground,
        topBar = { IosTopBar(title, leading = { BackButton(onBack) }, trailing = trailing) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (searchText != null) SearchField(searchText, onSearch, searchPrompt)
            Column(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier.fillMaxSize(), content = content)
        }
    }
    selectedTask?.let { task ->
        IosSheet(onDismiss = onTaskDismiss, dismissible = false) { dismiss -> TaskDetailSheet(task, dismiss) }
    }
}

/** Search used by Today, Tomorrow, Priority and Overdue: case and space insensitive. */
internal fun String.matchesListQuery(query: String): Boolean {
    val clean = query.lowercase().replace(" ", "")
    return clean.isEmpty() || lowercase().replace(" ", "").contains(clean)
}
