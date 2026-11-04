package com.list_it.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.model.AlertMode
import com.list_it.app.model.ListDisplayStyle
import com.list_it.app.model.QuickAction
import com.list_it.app.model.SortOption
import com.list_it.app.model.TodoList
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.LocalPrefs
import com.list_it.app.ui.components.AlertButton
import com.list_it.app.ui.components.CapsuleCount
import com.list_it.app.ui.components.ContentUnavailable
import com.list_it.app.ui.components.ContextMenuBox
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosMenu
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.MenuDivider
import com.list_it.app.ui.components.MenuItem
import com.list_it.app.ui.components.RequestNotificationPermission
import com.list_it.app.ui.components.SearchField
import com.list_it.app.ui.components.GridListCard
import com.list_it.app.ui.components.PinnedListCard
import com.list_it.app.ui.components.RowListCard
import com.list_it.app.ui.components.TintedButton
import com.list_it.app.ui.components.wavePath
import com.list_it.app.ui.screens.creating.AddListSheet
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.appBackground
import com.list_it.app.ui.theme.colorFromHex
import kotlinx.coroutines.launch

const val CURRENT_VERSION = 5

/** Home screen (iOS `AllListsView`). */
@Composable
fun AllListsScreen(onOpenList: (TodoList) -> Unit, onQuickAction: (QuickAction) -> Unit) {
    val db = LocalDb.current
    val prefs = LocalPrefs.current
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var showAddListView by remember { mutableStateOf(false) }
    var showWhatsNewView by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var alertMode by remember { mutableStateOf(AlertMode.ERROR) }
    var selectedListToDelete by remember { mutableStateOf<TodoList?>(null) }
    var selectedListToUpdate by remember { mutableStateOf<TodoList?>(null) }
    var currentSort by remember { mutableStateOf(SortOption.OLDEST) }

    val filteredLists = db.getFilteredLists(searchText, currentSort)

    RequestNotificationPermission()

    fun performDelete() {
        val listToDelete = selectedListToDelete
        if (listToDelete == null) {
            alertMode = AlertMode.ERROR
            db.setError("Error", "No List selected for deletion")
            showAlert = true
            return
        }
        scope.launch {
            if (!db.deleteList(listToDelete)) {
                alertMode = AlertMode.ERROR
                showAlert = true
            }
        }
    }

    val actions = ListActions(
        togglePin = { list -> scope.launch { db.updateListPin(list, !list.isPinned) } },
        update = { list -> selectedListToUpdate = list },
        delete = { list -> selectedListToDelete = list; alertMode = AlertMode.DELETE; showAlert = true },
    )

    Scaffold(
        containerColor = appBackground,
        topBar = {
            IosTopBar("Welcome Back 👋", trailing = { QuickMenu({ showAddListView = true }, onQuickAction) })
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(searchText, { searchText = it }, "Search List")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderView()
                PinnedListView(searchText, onOpenList, actions)
                ListInfoSection(currentSort, { currentSort = it }, onWhatsNew = { showWhatsNewView = true })
            }
            Box(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(2.dp)
                    .background(if (palette.isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)),
            )
            when {
                filteredLists.isEmpty() -> ContentUnavailable(
                    "No Lists", Icons.Filled.Checklist, description = "Create a List to start organising your tasks and notes.",
                ) {
                    TintedButton("Create List", Icons.Filled.Add, MaterialTheme.colorScheme.primary, prominent = true) { showAddListView = true }
                }
                prefs.displayStyle == ListDisplayStyle.CARD -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                ) {
                    items(filteredLists, key = { "${it.id}-${it.listName}-${it.bgColorHex}" }) { list ->
                        ContextMenuBox(onClick = { onOpenList(list) }, menu = { close -> ListContextMenu(list, actions, close) }) {
                            GridListCard(list)
                        }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filteredLists, key = { "${it.id}-${it.listName}-${it.bgColorHex}" }) { list ->
                        // Hold for pin, update and delete; the web build dropped the swipe actions.
                        ContextMenuBox(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            onClick = { onOpenList(list) },
                            menu = { close -> ListContextMenu(list, actions, close) },
                        ) { RowListCard(list) }
                    }
                }
            }
        }
    }

    if (showAlert) {
        if (alertMode == AlertMode.DELETE) {
            IosAlert(
                "Delete List?", "This action cannot be undone.", onDismiss = { showAlert = false },
                buttons = listOf(AlertButton("Cancel", cancel = true), AlertButton("Delete", destructive = true) { performDelete() }),
            )
        } else {
            IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
        }
    }
    if (showAddListView) IosSheet(onDismiss = { showAddListView = false }, dismissible = false) { dismiss -> AddListSheet(filteredLists, dismiss) }
    selectedListToUpdate?.let { list ->
        IosSheet(onDismiss = { selectedListToUpdate = null }, dismissible = false) { dismiss -> UpdateListSheet(list, dismiss) }
    }
    if (showWhatsNewView) IosSheet(onDismiss = { showWhatsNewView = false }, dismissible = false) { dismiss -> WhatsNewSheet(CURRENT_VERSION, dismiss) }
}

private class ListActions(val togglePin: (TodoList) -> Unit, val update: (TodoList) -> Unit, val delete: (TodoList) -> Unit)

@Composable
private fun QuickMenu(onCreate: () -> Unit, onQuickAction: (QuickAction) -> Unit) {
    IosMenu(label = { Icon(Icons.Filled.FilterList, "Menu", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp)) }) { close ->
        MenuItem("Create List", Icons.Filled.Add) { close(); onCreate() }
        MenuDivider()
        MenuItem("Today's Tasks", Icons.Filled.WbSunny) { close(); onQuickAction(QuickAction.TODAY) }
        MenuItem("Tomorrow's Tasks", Icons.Filled.WbTwilight) { close(); onQuickAction(QuickAction.TOMORROW) }
        MenuItem("Scheduled Tasks", Icons.Outlined.EventAvailable) { close(); onQuickAction(QuickAction.SCHEDULED) }
        MenuItem("Priority Tasks", Icons.Filled.Star) { close(); onQuickAction(QuickAction.PRIORITY) }
        MenuItem("Completed Tasks", Icons.Outlined.CheckCircle) { close(); onQuickAction(QuickAction.COMPLETED) }
        MenuItem("Not Completed Tasks", Icons.Outlined.Circle) { close(); onQuickAction(QuickAction.NOT_COMPLETED) }
        MenuItem("Overdue Tasks", Icons.Outlined.ErrorOutline) { close(); onQuickAction(QuickAction.OVERDUE) }
    }
}

@Composable
private fun ListContextMenu(list: TodoList, actions: ListActions, close: () -> Unit) {
    MenuItem(if (list.isPinned) "Unpin List" else "Pin List", if (list.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin) { close(); actions.togglePin(list) }
    if (!list.isDefault) {
        MenuItem("Update List", Icons.Filled.Edit) { close(); actions.update(list) }
        MenuItem("Delete List", Icons.Filled.Delete, destructive = true) { close(); actions.delete(list) }
    }
}

// MARK: - Header

@Composable
private fun HeaderView() {
    val db = LocalDb.current
    Text(db.currentUser?.name ?: "Loading...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LocalPalette.current.primary)
}

@Composable
private fun ListInfoSection(currentSort: SortOption, onSort: (SortOption) -> Unit, onWhatsNew: () -> Unit) {
    val db = LocalDb.current
    val prefs = LocalPrefs.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your Lists", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalPalette.current.primary)
            Spacer(Modifier.width(8.dp))
            CapsuleCount(db.lists.size, IosColor.orange)
            Spacer(Modifier.weight(1f))
            IosMenu(label = {
                Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SwapVert, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sort", color = MaterialTheme.colorScheme.primary, style = IosFont.body)
                }
            }) { close ->
                MenuItem("Oldest", Icons.Outlined.CalendarToday, checked = currentSort == SortOption.OLDEST) { close(); onSort(SortOption.OLDEST) }
                MenuItem("Newest", Icons.Filled.CalendarMonth, checked = currentSort == SortOption.NEWEST) { close(); onSort(SortOption.NEWEST) }
                MenuItem("A-Z", Icons.Filled.SortByAlpha, checked = currentSort == SortOption.ALPHABETICAL_AZ) { close(); onSort(SortOption.ALPHABETICAL_AZ) }
                MenuItem("Z-A", Icons.Filled.SortByAlpha, checked = currentSort == SortOption.ALPHABETICAL_ZA) { close(); onSort(SortOption.ALPHABETICAL_ZA) }
            }
        }
        if (prefs.lastSeenWhatsNewVersion < CURRENT_VERSION) {
            Row(Modifier.clickable(onClick = onWhatsNew).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("See what's new", color = MaterialTheme.colorScheme.primary, style = IosFont.body)
            }
        }
    }
}

// MARK: - Pinned lists

@Composable
private fun PinnedListView(searchText: String, onOpenList: (TodoList) -> Unit, actions: ListActions) {
    val db = LocalDb.current
    val pinned = db.pinnedLists(searchText)
    if (pinned.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pinned Lists", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LocalPalette.current.primary)
            Spacer(Modifier.width(8.dp))
            CapsuleCount(pinned.size, IosColor.orange)
        }
        // One standard card size, three in view at most; the rest scroll sideways.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 12.dp
            val cardWidth = (maxWidth - gap * 2) / 3
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                pinned.forEach { list ->
                    ContextMenuBox(onClick = { onOpenList(list) }, menu = { close -> ListContextMenu(list, actions, close) }) {
                        PinnedListCard(list, cardWidth)
                    }
                }
            }
        }
    }
}
