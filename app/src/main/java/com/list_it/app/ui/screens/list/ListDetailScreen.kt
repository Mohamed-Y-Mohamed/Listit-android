package com.list_it.app.ui.screens.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
// Both folder icons are used; the outlined one is aliased so `Icons.Filled.Folder` keeps its name.
import androidx.compose.material.icons.outlined.Folder as FolderOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.list_it.app.data.ReminderStore
import com.list_it.app.model.AlertMode
import com.list_it.app.model.CollectionLayout
import com.list_it.app.model.ListCollection
import com.list_it.app.model.Note
import com.list_it.app.model.SortOption
import com.list_it.app.model.ToDoTask
import com.list_it.app.model.TodoList
import com.list_it.app.ui.Dates
import com.list_it.app.ui.LocalDb
import com.list_it.app.ui.screens.task.TaskInfoSheet
import com.list_it.app.ui.screens.collection.UpdateCollectionSheet
import com.list_it.app.ui.LocalPrefs
import com.list_it.app.ui.components.AlertButton
import com.list_it.app.ui.components.BackButton
import com.list_it.app.ui.components.ContentUnavailable
import com.list_it.app.ui.components.ContextMenuBox
import com.list_it.app.ui.components.FoldFlapShape
import com.list_it.app.ui.components.FoldedNoteShape
import com.list_it.app.ui.components.IosAlert
import com.list_it.app.ui.components.IosDivider
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.components.IosTopBar
import com.list_it.app.ui.components.MenuDivider
import com.list_it.app.ui.components.AnchoredMenu
import com.list_it.app.ui.components.MenuItem
import com.list_it.app.ui.components.SubMenuItem
import com.list_it.app.ui.components.SearchField
import com.list_it.app.ui.components.TintedButton
import com.list_it.app.ui.components.UnderlineTab
import com.list_it.app.ui.components.diagonalSlicePath
import com.list_it.app.ui.screens.collection.AddCollectionSheet
import com.list_it.app.ui.screens.note.AddNoteSheet
import com.list_it.app.ui.screens.task.AddTaskSheet
import com.list_it.app.ui.screens.note.NoteDetailSheet
import com.list_it.app.ui.screens.task.TaskDetailSheet
import com.list_it.app.ui.theme.IosColor
import com.list_it.app.ui.theme.IosFont
import com.list_it.app.ui.theme.LocalPalette
import com.list_it.app.ui.theme.appBackground
import com.list_it.app.ui.theme.colorFromHex
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * An open create sheet. [collectionID] is the collection it was launched from, so the empty-state
 * buttons drop the new task or note into the collection the user was looking at; null falls back
 * to General, as the toolbar menu does.
 */
private data class CreateIn(val collectionID: String?)

/** The page for one list: its collections, and the tasks and notes inside each one. */
@Composable
fun ListDetailScreen(list: TodoList, onBack: () -> Unit) {
    var addTask by remember { mutableStateOf<CreateIn?>(null) }
    var addNote by remember { mutableStateOf<CreateIn?>(null) }
    var showAddCollection by remember { mutableStateOf(false) }
    var showTaskInfo by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(SortOption.OLDEST) }
    var searchText by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = appBackground,
        topBar = {
            IosTopBar(list.listName, leading = { BackButton(onBack) }, trailing = {
                if (!list.isDefault) {
                    IconButton(onClick = { showTaskInfo = true }) { Icon(Icons.Outlined.Info, "Task Info", tint = MaterialTheme.colorScheme.primary) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreHoriz, "More", tint = MaterialTheme.colorScheme.primary) }
                        AnchoredMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            MenuItem("Add Task", Icons.Filled.Edit) { menuOpen = false; addTask = CreateIn(null) }
                            MenuItem("Add Note", Icons.AutoMirrored.Filled.Notes) { menuOpen = false; addNote = CreateIn(null) }
                            MenuItem("Add Collection", Icons.Outlined.FolderOutline) { menuOpen = false; showAddCollection = true }
                            MenuDivider()
                            Text("Sort by", style = IosFont.footnote, color = LocalPalette.current.secondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            SortOption.entries.forEach { option ->
                                MenuItem(option.title, checked = currentSort == option) { menuOpen = false; currentSort = option }
                            }
                        }
                    }
                }
            })
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(searchText, { searchText = it }, "Search tasks & notes")
            AllCollectionsView(
                list, currentSort, searchText,
                onAddTask = { addTask = CreateIn(it) },
                onAddNote = { addNote = CreateIn(it) },
            )
        }
    }

    addTask?.let { request ->
        IosSheet(onDismiss = { addTask = null }) { dismiss -> AddTaskSheet(list, request.collectionID, dismiss) }
    }
    addNote?.let { request ->
        IosSheet(onDismiss = { addNote = null }) { dismiss -> AddNoteSheet(list, request.collectionID, dismiss) }
    }
    if (showAddCollection) IosSheet(onDismiss = { showAddCollection = false }, dismissible = false) { dismiss -> AddCollectionSheet(list, dismiss) }
    if (showTaskInfo) IosSheet(onDismiss = { showTaskInfo = false }) { dismiss -> TaskInfoSheet(dismiss) }
}

/** Open tasks and live notes in one collection. */
private data class CollectionCounts(val tasks: Int, val notes: Int)

private val NoCounts = CollectionCounts(0, 0)

/**
 * Counts for every collection on screen, grouped once per change to the task and note lists.
 * Counting inside each column instead walked all tasks and all notes per collection, on every
 * recomposition, so a search keystroke cost collections x rows.
 */
@Composable
private fun rememberCollectionCounts(): Map<String, CollectionCounts> {
    val db = LocalDb.current
    val tasks = db.tasks
    val notes = db.notes
    return remember(tasks, notes) {
        val open = tasks.asSequence()
            .filter { !it.isCompleted && !it.isDeleted }
            .groupingBy { it.collectionID }.eachCount()
        val live = notes.asSequence()
            .filter { !it.isDeleted }
            .groupingBy { it.collectionID }.eachCount()
        (open.keys + live.keys).associateWith { CollectionCounts(open[it] ?: 0, live[it] ?: 0) }
    }
}

@Composable
private fun ColumnScope.AllCollectionsView(
    list: TodoList,
    currentSort: SortOption,
    searchText: String,
    onAddTask: (String) -> Unit,
    onAddNote: (String) -> Unit,
) {
    val db = LocalDb.current
    val prefs = LocalPrefs.current
    val scope = rememberCoroutineScope()
    var showAlert by remember { mutableStateOf(false) }
    var alertMode by remember { mutableStateOf(AlertMode.ERROR) }
    var selectedToUpdate by remember { mutableStateOf<ListCollection?>(null) }
    var selectedToDelete by remember { mutableStateOf<ListCollection?>(null) }

    val base = db.collections.filter { collection ->
        if (collection.listID != list.id) return@filter false
        if (searchText.isEmpty()) return@filter true
        val hasMatchingTasks = db.tasks.any { it.collectionID == collection.id && it.text.contains(searchText, true) && !it.isCompleted }
        val hasMatchingNotes = db.notes.any { it.collectionID == collection.id && it.title.contains(searchText, true) }
        hasMatchingTasks || hasMatchingNotes
    }
    val filteredCollections = when (currentSort) {
        SortOption.OLDEST -> base.sortedBy { it.createdAt }
        SortOption.NEWEST -> base.sortedByDescending { it.createdAt }
        SortOption.ALPHABETICAL_AZ -> base.sortedBy { it.collectionName.lowercase() }
        SortOption.ALPHABETICAL_ZA -> base.sortedByDescending { it.collectionName.lowercase() }
    }
    val counts = rememberCollectionCounts()

    /** Deletes whatever you chose to delete (a collection, task or note) after you confirm. */
    fun performDelete() = scope.launch {
        val toDelete = selectedToDelete
        if (toDelete == null) {
            alertMode = AlertMode.ERROR
            db.setError("Error", "No Collection selected for deletion")
            showAlert = true
            return@launch
        }
        if (!db.deleteCollection(toDelete)) {
            alertMode = AlertMode.ERROR
            showAlert = true
        }
    }

    /** Update and delete, for whichever layout is showing. "General" cannot be edited. */
    val collectionMenu: @Composable ColumnScope.(ListCollection, () -> Unit) -> Unit = { collection, close ->
        MenuItem("Update Collection", Icons.Filled.Edit) { close(); selectedToUpdate = collection }
        MenuItem("Delete Collection", Icons.Filled.Delete, destructive = true) {
            close(); selectedToDelete = collection; alertMode = AlertMode.DELETE; showAlert = true
        }
    }

    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Hold items for more options", style = IosFont.caption, color = LocalPalette.current.secondary)
        }
        if (filteredCollections.isEmpty()) {
            ContentUnavailable("No Collections", Icons.Filled.CreateNewFolder, description = "Add a collection to organize your tasks.", modifier = Modifier.padding(top = 40.dp))
        } else when (prefs.collectionLayout) {
            CollectionLayout.COLUMNS -> CollectionColumnsView(
                filteredCollections, counts, searchText, onAddTask, onAddNote, collectionMenu,
            )
            CollectionLayout.EXPANDABLE -> Column(Modifier.verticalScroll(rememberScrollState())) {
                filteredCollections.forEach { collection ->
                    val canEdit = collection.collectionName.lowercase() != "general"
                    ContextMenuBox(
                        modifier = Modifier.padding(vertical = 4.dp),
                        menu = if (canEdit) { close -> collectionMenu(collection, close) } else null,
                    ) {
                        CollectionView(collection, counts[collection.id] ?: NoCounts, searchText, onAddTask, onAddNote)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAlert) {
        if (alertMode == AlertMode.DELETE) {
            IosAlert(
                "Delete Collection?", "This action cannot be undone.", onDismiss = { showAlert = false },
                buttons = listOf(AlertButton("Cancel", cancel = true), AlertButton("Delete", destructive = true) { performDelete() }),
            )
        } else IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    }
    selectedToUpdate?.let { c ->
        IosSheet(onDismiss = { selectedToUpdate = null }, dismissible = false) { dismiss -> UpdateCollectionSheet(c, dismiss) }
    }
}

// MARK: - Columns layout

/**
 * Collections side by side, one nearly-full-width column each, snapping as it scrolls
 * (iOS `ScrollView(.horizontal)` + `.scrollTargetBehavior(.viewAligned)`).
 */
@Composable
private fun CollectionColumnsView(
    collections: List<ListCollection>,
    counts: Map<String, CollectionCounts>,
    searchText: String,
    onAddTask: (String) -> Unit,
    onAddNote: (String) -> Unit,
    menu: @Composable ColumnScope.(ListCollection, () -> Unit) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // A sliver of the next column stays in view, so it reads as scrollable.
        val columnWidth = maxOf(260.dp, maxWidth - 48.dp)
        val pager = rememberPagerState { collections.size }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            pageSize = PageSize.Fixed(columnWidth),
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) { page ->
            val collection = collections[page]
            val canEdit = collection.collectionName.lowercase() != "general"
            ContextMenuBox(
                modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
                menu = if (canEdit) { close -> menu(collection, close) } else null,
            ) {
                CollectionColumn(collection, counts[collection.id] ?: NoCounts, searchText, onAddTask, onAddNote)
            }
        }
    }
}

/** One full-height collection card (iOS `CollectionColumnView`). */
@Composable
private fun CollectionColumn(
    collection: ListCollection,
    counts: CollectionCounts,
    searchText: String,
    onAddTask: (String) -> Unit,
    onAddNote: (String) -> Unit,
) {
    val palette = LocalPalette.current
    val color = colorFromHex(collection.bgColorHex)
    val shape = RoundedCornerShape(20.dp)
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(palette.secondaryGroupedBackground)
            .border(1.dp, color.copy(alpha = 0.22f), shape),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    collection.collectionName,
                    style = IosFont.title3.copy(fontWeight = FontWeight.Bold),
                    color = palette.primary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text("${counts.tasks} Tasks · ${counts.notes} Notes", style = IosFont.caption, color = palette.secondary)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Folder, null, tint = color, modifier = Modifier.size(20.dp))
        }

        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("Tasks", "Notes").forEachIndexed { index, title ->
                UnderlineTab(
                    title, selectedTab == index, color,
                    spacing = 7.dp, indicatorHeight = 3.dp, capsule = true,
                ) { selectedTab = index }
            }
        }
        IosDivider()

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (selectedTab == 0) {
                TaskListView(collection, searchText) { onAddTask(collection.id) }
            } else {
                NoteListView(collection, searchText) { onAddNote(collection.id) }
            }
        }
    }
}

// MARK: - Expandable layout

/** Expandable collection card (iOS `CollectionView` + `CollectionHeader`). */
@Composable
private fun CollectionView(
    collection: ListCollection,
    counts: CollectionCounts,
    searchText: String,
    onAddTask: (String) -> Unit,
    onAddNote: (String) -> Unit,
) {
    var isExpandedState by remember { mutableStateOf(false) }
    val isExpanded = searchText.isNotEmpty() || isExpandedState
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val color = colorFromHex(collection.bgColorHex)
    val shape = RoundedCornerShape(17.dp)
    val anim = spring<Float>(dampingRatio = 0.85f, stiffness = 500f)
    val tilt by animateFloatAsState(if (isExpanded) 0f else 1f, anim, label = "tilt")
    val highlightWidth by animateFloatAsState(if (isExpanded) 1f else 0.30f, anim, label = "width")
    val skew by animateFloatAsState(if (isExpanded) 0f else 24f, anim, label = "skew")
    val chevron by animateFloatAsState(if (isExpanded) 90f else 0f, anim, label = "chevron")

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                rotationX = -6f * tilt
                rotationY = 2.4f * tilt
                cameraDistance = 12f * density
                translationX = 6.dp.toPx() * tilt
                translationY = -4.dp.toPx() * tilt
            }
            .clip(shape)
            .border(1.dp, color.copy(alpha = if (isDark) 0.45f else 0.35f), shape),
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (isDark) palette.secondaryGroupedBackground else Color.White)
                .drawBehind {
                    val w = size.width * highlightWidth
                    drawPath(
                        diagonalSlicePath(Size(w, size.height), skew.dp.toPx()),
                        SolidColor(color.copy(alpha = if (isDark) 0.25f else 0.16f)),
                    )
                }
                .clickable { isExpandedState = !isExpanded }
                .padding(vertical = 14.dp)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CollectionDetailView(collection, counts, Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = color,
                modifier = Modifier.padding(end = 16.dp).rotate(chevron),
            )
        }
        AnimatedVisibility(isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Box(Modifier.background(if (isDark) Color.Black.copy(alpha = 0.12f) else palette.groupedBackground.copy(alpha = 0.3f)).padding(start = 12.dp, top = 14.dp, bottom = 12.dp)) {
                CollectionTabView(collection, searchText, onAddTask, onAddNote)
            }
        }
    }
}

/** The open part of a collection card: its Tasks and Notes tabs. */
@Composable
private fun CollectionDetailView(collection: ListCollection, counts: CollectionCounts, modifier: Modifier) {
    val palette = LocalPalette.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(collection.collectionName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.primary)
        Text("${counts.tasks} Tasks · ${counts.notes} Notes", style = IosFont.caption, color = palette.secondary)
    }
}

/** Switches between the Tasks tab and the Notes tab inside a collection. */
@Composable
private fun CollectionTabView(
    collection: ListCollection,
    searchText: String,
    onAddTask: (String) -> Unit,
    onAddNote: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val color = colorFromHex(collection.bgColorHex)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("Tasks", "Notes").forEachIndexed { index, title ->
                UnderlineTab(title, selectedTab == index, color) { selectedTab = index }
            }
        }
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            if (selectedTab == 0) {
                TaskListView(collection, searchText) { onAddTask(collection.id) }
            } else {
                NoteListView(collection, searchText) { onAddNote(collection.id) }
            }
        }
    }
}

// MARK: - Tasks

/** The tasks inside a collection, filtered by the search box. */
@Composable
private fun TaskListView(collection: ListCollection, searchText: String, onCreate: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var selectedTask by remember { mutableStateOf<ToDoTask?>(null) }
    var taskToDelete by remember { mutableStateOf<ToDoTask?>(null) }
    var showAlert by remember { mutableStateOf(false) }
    var alertMode by remember { mutableStateOf(AlertMode.ERROR) }

    val filteredTasks = db.tasks
        .filter { it.collectionID == collection.id && !it.isCompleted && !it.isDeleted && (searchText.isEmpty() || it.text.contains(searchText, true)) }
        .sortedWith(compareByDescending<ToDoTask> { it.isPinned }.thenByDescending { it.createdAt })

    if (filteredTasks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ContentUnavailable("No Tasks", Icons.Outlined.EditNote, description = "Try adding a Task to this Collection.") {
                TintedButton("Create Task", Icons.Filled.Add, MaterialTheme.colorScheme.primary, prominent = true, onClick = onCreate)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            filteredTasks.forEach { task ->
                ContextMenuBox(onClick = { selectedTask = task }, menu = { close ->
                    MoveMenu(db.collections.filter { it.listID == task.listID }, task.collectionID, "Move Task") { target ->
                        close()
                        scope.launch { if (!db.moveTask(task, target.id)) showAlert = true }
                    }
                    MenuItem(if (task.isPinned) "Unpin Task" else "Pin Task", if (task.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin) {
                        close()
                        scope.launch {
                            if (!db.updatePinForTask(task, !task.isPinned)) {
                                db.setError("Pin Error", "Error updating Task Pin")
                                showAlert = true
                            }
                        }
                    }
                    MenuItem("Delete Task", Icons.Filled.Delete) {
                        close()
                        taskToDelete = task
                        alertMode = AlertMode.DELETE
                        db.setError("Delete Task", "Are you sure you want to delete this Task?")
                        showAlert = true
                    }
                }) { TaskView(task) }
                IosDivider()
            }
        }
    }

    selectedTask?.let { task ->
        IosSheet(onDismiss = { selectedTask = null }) { dismiss -> TaskDetailSheet(task, dismiss) }
    }
    if (showAlert) {
        if (alertMode == AlertMode.DELETE) {
            IosAlert(
                db.alertTitle, db.alertMessage, onDismiss = { showAlert = false },
                buttons = listOf(
                    AlertButton("Cancel", cancel = true) { taskToDelete = null },
                    AlertButton("Delete", destructive = true) {
                        val task = taskToDelete ?: return@AlertButton
                        scope.launch {
                            if (!db.deleteTask(task)) { alertMode = AlertMode.ERROR; showAlert = true } else taskToDelete = null
                        }
                    },
                ),
            )
        } else IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    }
}

/** The "Move to…" menu: shows the other collections in this list so you can move an item there. */
@Composable
private fun MoveMenu(collections: List<ListCollection>, currentID: String, title: String, onMove: (ListCollection) -> Unit) {
    SubMenuItem(title, Icons.Filled.DriveFileMove) {
        collections.forEach { c ->
            MenuItem(c.collectionName, enabled = c.id != currentID, checked = c.id == currentID) { onMove(c) }
        }
    }
}

/** One task in the list, with a coloured bar on the side showing if it's overdue, pinned, etc. */
@Composable
private fun TaskView(task: ToDoTask) {
    val db = LocalDb.current
    val palette = LocalPalette.current
    val now = Instant.now()
    val due = task.dueDate
    val barColor = when {
        due != null && due < now -> IosColor.red
        task.isPinned -> if (due == null) IosColor.yellow else IosColor.orange
        else -> IosColor.cyan
    }
    val upcomingReminder = ReminderStore.date(task.id)?.takeIf { it > now }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 10.dp)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(barColor, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(task.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = palette.primary)
            task.description?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = IosFont.footnote, color = palette.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.CalendarMonth, null, tint = palette.secondary, modifier = Modifier.size(11.dp))
                if (due != null) {
                    Text(
                        "Due: ${db.formattedDate(due)}", style = IosFont.caption2, maxLines = 1,
                        fontWeight = if (due < now) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (due < now) IosColor.red else palette.secondary,
                    )
                } else {
                    Text("Created: ${db.formattedDate(task.createdAt)}", style = IosFont.caption2, color = palette.secondary, maxLines = 1)
                }
                if (upcomingReminder != null) {
                    Text("·", style = IosFont.caption2, color = palette.secondary)
                    Icon(Icons.Outlined.Notifications, null, tint = palette.secondary, modifier = Modifier.size(11.dp))
                    Text(Dates.monthDayTime(upcomingReminder), style = IosFont.caption2, color = palette.secondary, maxLines = 1)
                }
            }
        }
    }
}

// MARK: - Notes

/** The notes inside a collection, filtered by the search box. */
@Composable
private fun NoteListView(collection: ListCollection, searchText: String, onCreate: () -> Unit) {
    val db = LocalDb.current
    val scope = rememberCoroutineScope()
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showAlert by remember { mutableStateOf(false) }
    var alertMode by remember { mutableStateOf(AlertMode.ERROR) }

    val filteredNotes = db.notes
        .filter { it.collectionID == collection.id && !it.isDeleted && (searchText.isEmpty() || it.title.contains(searchText, true)) }
        .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt })

    if (filteredNotes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ContentUnavailable("No Notes", Icons.AutoMirrored.Filled.Notes, description = "Try adding a Note to this Collection.") {
                TintedButton("Create Note", Icons.Filled.Add, MaterialTheme.colorScheme.primary, prominent = true, onClick = onCreate)
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            filteredNotes.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { note ->
                        ContextMenuBox(modifier = Modifier.weight(1f), onClick = { selectedNote = note }, menu = { close ->
                            MoveMenu(db.collections.filter { it.listID == note.listID }, note.collectionID, "Move Note") { target ->
                                close()
                                scope.launch { if (!db.moveNote(note, target.id)) showAlert = true }
                            }
                            MenuItem(if (note.isPinned) "Unpin Note" else "Pin Note", if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin) {
                                close()
                                scope.launch { if (!db.updatePinForNote(note, !note.isPinned)) showAlert = true }
                            }
                            MenuItem("Delete Note", Icons.Filled.Delete) {
                                close()
                                noteToDelete = note
                                alertMode = AlertMode.DELETE
                                db.setError("Delete Note", "Are you sure you want to delete this Note?")
                                showAlert = true
                            }
                        }) { NoteView(note) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    selectedNote?.let { note ->
        IosSheet(onDismiss = { selectedNote = null }) { dismiss -> NoteDetailSheet(note, dismiss) }
    }
    if (showAlert) {
        if (alertMode == AlertMode.DELETE) {
            IosAlert(
                db.alertTitle, db.alertMessage, onDismiss = { showAlert = false },
                buttons = listOf(
                    AlertButton("Cancel", cancel = true) { noteToDelete = null },
                    AlertButton("Delete", destructive = true) {
                        val note = noteToDelete ?: return@AlertButton
                        scope.launch {
                            if (!db.deleteNote(note)) { alertMode = AlertMode.ERROR; showAlert = true } else noteToDelete = null
                        }
                    },
                ),
            )
        } else IosAlert(db.alertTitle, db.alertMessage, onDismiss = { showAlert = false })
    }
}

/** One note, drawn as a sticky note with a folded corner. */
@Composable
private fun NoteView(note: Note) {
    val palette = LocalPalette.current
    val isDark = palette.isDark
    val color = colorFromHex(note.bgColorHex)
    val shape = FoldedNoteShape(cornerRadius = 8f, foldSize = 16f)
    Box(Modifier.fillMaxWidth().height(90.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .shadow(3.dp, shape, ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.12f), spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.12f))
                .background(palette.systemBackground, shape)
                .background(color.copy(alpha = 0.08f), shape)
                .border(0.8.dp, color.copy(alpha = 0.35f), shape)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                note.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = palette.primary, modifier = Modifier.padding(end = if (note.isPinned) 12.dp else 0.dp),
            )
            note.description?.takeIf { it.isNotEmpty() }?.let {
                Text(it, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = palette.secondary)
            }
        }
        if (note.isPinned) {
            Icon(Icons.Filled.PushPin, null, tint = palette.secondary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp))
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp)
                .shadow(1.5.dp, FoldFlapShape)
                .background(palette.systemBackground, FoldFlapShape)
                .background(color.copy(alpha = if (isDark) 0.55f else 0.45f), FoldFlapShape),
        )
    }
}
