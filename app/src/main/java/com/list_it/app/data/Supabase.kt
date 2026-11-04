package com.list_it.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.list_it.app.model.AppColor
import com.list_it.app.model.AppUser
import com.list_it.app.model.ListCollection
import com.list_it.app.model.Note
import com.list_it.app.model.SortOption
import com.list_it.app.model.SupabaseDates
import com.list_it.app.model.ToDoTask
import com.list_it.app.model.TodoList
import com.list_it.app.model.VerificationStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

/** App state and every Supabase call, mirroring the iOS `Supabase` class one to one. */
class Supabase(private val prefs: Prefs, val client: SupabaseClient) {

    var alertTitle by mutableStateOf("")
    var alertMessage by mutableStateOf("")
    var verificationStatus by mutableStateOf<VerificationStatus>(VerificationStatus.Pending)

    var isSignedIn: Boolean
        get() = prefs.isSignedIn
        set(value) { prefs.isSignedIn = value }

    var userID by mutableStateOf("")
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var forgotPasswordEmail by mutableStateOf("")
    var currentUser by mutableStateOf<AppUser?>(null)
    var listColorHexes by mutableStateOf<List<String>>(emptyList())

    var defaultLists by mutableStateOf(
        listOf(
            defaultList("sun.max", "Today", "#FF9500"),
            defaultList("sunrise", "Tomorrow", "#007AFF"),
            defaultList("flag.fill", "Priority", "#5856D6"),
            defaultList("checkmark.square.fill", "Completed", "#34C759"),
            defaultList("square", "Not Completed", "#FF3B30"),
            defaultList("clock.arrow.circlepath", "Overdue", "#FF2D55"),
        )
    )
    var lists by mutableStateOf<List<TodoList>>(emptyList())
    var collections by mutableStateOf<List<ListCollection>>(emptyList())
    var tasks by mutableStateOf<List<ToDoTask>>(emptyList())
    var notes by mutableStateOf<List<Note>>(emptyList())

    // MARK: - Fetch All Lists
    fun getFilteredLists(query: String, sort: SortOption): List<TodoList> {
        val filtered = lists.filter { query.isEmpty() || it.listName.contains(query, ignoreCase = true) }
        val comparator: Comparator<TodoList> = when (sort) {
            SortOption.OLDEST -> compareByDescending<TodoList> { it.isDefault }.thenBy { it.createdAt }
            SortOption.NEWEST -> compareByDescending { it.createdAt }
            SortOption.ALPHABETICAL_AZ -> compareBy { it.listName.lowercase() }
            SortOption.ALPHABETICAL_ZA -> compareByDescending { it.listName.lowercase() }
        }
        return filtered.sortedWith(comparator)
    }

    // MARK: - Fetch pinned lists
    fun pinnedLists(query: String): List<TodoList> =
        (defaultLists + lists).sortedBy { it.createdAt }
            .filter { it.isPinned && (query.isEmpty() || it.listName.contains(query, ignoreCase = true)) }

    // MARK: - Setting up alerts
    fun setError(title: String, message: String) {
        alertTitle = title
        alertMessage = message
    }

    // MARK: - Date formatter
    fun formattedDate(date: Instant): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(date.atZone(ZoneId.systemDefault()))

    // MARK: - Check if user email exists in database
    suspend fun isEmailAvailable(email: String): Boolean = try {
        val response = client.from("users").select(Columns.list("id")) {
            head = true
            count(Count.EXACT)
            filter { eq("email", email) }
        }
        (response.countOrNull() ?: 0L) == 0L
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error checking Email", "Unable to verify email availability. Please try again.")
        false
    }

    // MARK: - Create user
    suspend fun createUser(name: String, email: String, password: String): Pair<String, Boolean> {
        try {
            val user = client.auth.signUpWith(Email, redirectUrl = "https://list-it-dom.netlify.app") {
                this.email = email
                this.password = password
                data = buildJsonObject { put("full_name", name) }
            }
            val userExists = user?.identities?.isEmpty() ?: true
            if (userExists) {
                setError("Check your email", "If this email is new, a confirmation link was sent.")
            }
            return (user?.id ?: "") to true
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            setError("Sign up failed", "Error signing up: ${e.text()}")
            throw e
        }
    }

    // MARK: - Sign out user
    suspend fun signOutUser() {
        try {
            client.auth.signOut(SignOutScope.LOCAL)
            lists = emptyList()
            currentUser = null
            isSignedIn = false
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            setError("Error", "Error signing out: ${e.text()}")
            isSignedIn = false
        }
    }

    // MARK: - Reset all fields
    fun resetFields() {
        name = ""
        email = ""
        password = ""
        confirmPassword = ""
    }

    // MARK: - Delete user account
    suspend fun deleteAccountById(userID: String): Boolean = try {
        client.postgrest.rpc("delete_user_by_id", buildJsonObject { put("user_id_param", userID) })
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error", "Error deleting account: ${e.text()}")
        false
    }

    // MARK: - Verification check
    suspend fun verificationCheck(userID: String, name: String, email: String, password: String) {
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            insertUserIntoDatabase(userID, name, email)
            verificationStatus = VerificationStatus.Verified
            isSignedIn = true
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            val errorMsg = e.text().lowercase()
            verificationStatus = if (errorMsg.contains("email not confirmed") || errorMsg.contains("confirmation")) {
                VerificationStatus.Pending
            } else {
                VerificationStatus.Error("Verification failed: ${e.text()}")
            }
        }
    }

    // MARK: - Insert user into table
    suspend fun insertUserIntoDatabase(userID: String, name: String, email: String) {
        try {
            client.from("users").insert(
                buildJsonObject {
                    put("id", userID)
                    put("full_name", name)
                    put("email", email)
                }
            )
            isSignedIn = true
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            setError("Error", "Failed to create user record: ${e.text()}")
        }
    }

    // MARK: - Check if email is valid
    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)

    // MARK: - Check if name is valid
    fun isValidName(): Boolean {
        val trimmed = name.trim()
        return trimmed.length in 2..30 && NAME_REGEX.matches(trimmed)
    }

    // MARK: - Check if password is valid
    fun isStrongPassword(): Boolean = PasswordRules.error(password) == null

    // MARK: - Check if passwords match
    fun passwordsMatch(): Boolean = password == confirmPassword

    // MARK: - Sign in user
    suspend fun signIn(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            setError("Error", "Email and Password must be filled in.")
            return false
        }
        if (!isValidEmail(email)) {
            setError("Error", "Invalid Email")
            return false
        }
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val success = initializeSession()
            isSignedIn = success
            success
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            setError("Login Error", e.text())
            false
        }
    }

    // MARK: - Reset password
    suspend fun resetPassword(): Boolean {
        if (forgotPasswordEmail.isEmpty()) {
            setError("Error", "Email field cannot be empty.")
            return false
        }
        if (!isValidEmail(forgotPasswordEmail)) {
            setError("Error", "Invalid email address. Please try again.")
            return false
        }
        return try {
            client.auth.resetPasswordForEmail(forgotPasswordEmail)
            true
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            setError("Error", "Failed to send reset email: ${e.text()}")
            false
        }
    }

    // MARK: - Save List with General collection
    suspend fun saveList(newList: TodoList, generalCollection: ListCollection): Boolean = try {
        val ownerId = currentUser!!.id
        client.from("list").insert(newList.copy(userId = ownerId))
        saveCollection(generalCollection.copy(userID = ownerId))
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error", "Error saving your new List: ${e.text()}")
        false
    }

    // MARK: - Fetch user Lists
    suspend fun fetchUserLists(): Boolean = try {
        val uid = authUserId()
        lists = emptyList()
        lists = client.from("list").select { filter { eq("user_id", uid) } }.decodeList<TodoList>()
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error", "Error fetching your Lists: ${e.text()}")
        false
    }

    // MARK: - Pin List
    suspend fun updateListPin(list: TodoList, isPinned: Boolean): Boolean = try {
        if (!list.isDefault) {
            client.from("list").update({ set("is_pinned", isPinned) }) { filter { eq("id", list.id) } }
        }
        if (list.isDefault) {
            defaultLists = defaultLists.map { if (it.id == list.id) it.copy(isPinned = isPinned) else it }
        } else {
            lists = lists.map { if (it.id == list.id) it.copy(isPinned = isPinned) else it }
        }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Update Failed", e.text())
        false
    }

    // MARK: - Delete List
    suspend fun deleteList(list: TodoList): Boolean = try {
        client.from("list").delete { filter { eq("id", list.id) } }
        tasks = tasks.filterNot { it.listID == list.id }
        notes = notes.filterNot { it.listID == list.id }
        collections = collections.filterNot { it.listID == list.id }
        lists = lists.filterNot { it.id == list.id }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Delete Failed", e.text())
        false
    }

    // MARK: - Update List
    suspend fun updateList(list: TodoList, name: String, hex: String): Boolean = try {
        client.from("list").update({
            set("list_name", name)
            set("bg_color_hex", hex)
        }) { filter { eq("id", list.id) } }
        lists = lists.map { if (it.id == list.id) it.copy(listName = name, bgColorHex = hex) else it }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Update Failed", e.text())
        false
    }

    // MARK: - Save Collection
    suspend fun saveCollection(newCollection: ListCollection): Boolean = try {
        client.from("collection").insert(newCollection.copy(userID = currentUser!!.id))
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error", "Error creating Collection, please try again. Error: ${e.text()}")
        false
    }

    // MARK: - Fetch user Collections
    suspend fun fetchUserCollections(): Boolean = try {
        val uid = authUserId()
        collections = client.from("collection").select { filter { eq("user_id", uid) } }.decodeList<ListCollection>()
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        false
    }

    // MARK: - Update Collection
    suspend fun updateCollection(collection: ListCollection, name: String, hex: String): Boolean = try {
        client.from("collection").update({
            set("collection_name", name)
            set("bg_color_hex", hex)
        }) { filter { eq("id", collection.id) } }
        collections = collections.map { if (it.id == collection.id) it.copy(collectionName = name, bgColorHex = hex) else it }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Update Failed", e.text())
        false
    }

    // MARK: - Delete Collection
    suspend fun deleteCollection(collection: ListCollection): Boolean = try {
        client.from("collection").delete { filter { eq("id", collection.id) } }
        tasks = tasks.filterNot { it.collectionID == collection.id }
        notes = notes.filterNot { it.collectionID == collection.id }
        collections = collections.filterNot { it.id == collection.id }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Delete Failed", e.text())
        false
    }

    // MARK: - Save Task
    suspend fun saveTask(newTask: ToDoTask): Boolean = try {
        client.from("task").insert(newTask.copy(userID = currentUser!!.id))
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error", "Error creating Task, please try again. Error: ${e.text()}")
        false
    }

    // MARK: - Fetch user Task
    suspend fun fetchUserTasks(): Boolean = try {
        val uid = authUserId()
        tasks = client.from("task").select { filter { eq("user_id", uid) } }.decodeList<ToDoTask>()
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error", "Task fetch error: ${e.text()}")
        false
    }

    // MARK: - Update Tasks
    suspend fun updateTask(task: ToDoTask, text: String, description: String, dueDate: Instant?, isPinned: Boolean): Boolean = try {
        client.from("task").update({
            set("text", text)
            set("description", description)
            if (dueDate != null) set("due_date", SupabaseDates.formatIso(dueDate)) else setToNull("due_date")
            set("is_pinned", isPinned)
        }) { filter { eq("id", task.id) } }
        tasks = tasks.map {
            if (it.id == task.id) it.copy(text = text, description = description, dueDate = dueDate, isPinned = isPinned) else it
        }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Update Failed", e.text())
        false
    }

    // MARK: - Update Pin status for Task
    suspend fun updatePinForTask(task: ToDoTask, isPinned: Boolean): Boolean = try {
        client.from("task").update({ set("is_pinned", isPinned) }) { filter { eq("id", task.id) } }
        tasks = tasks.map { if (it.id == task.id) it.copy(isPinned = isPinned) else it }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Pin Failed", e.text())
        false
    }

    // MARK: - Move Task
    suspend fun moveTask(task: ToDoTask, newCollectionID: String): Boolean = try {
        client.from("task").update({ set("collection_id", newCollectionID) }) { filter { eq("id", task.id) } }
        tasks = tasks.map { if (it.id == task.id) it.copy(collectionID = newCollectionID) else it }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Move Failed", "Failed to move Task to new Collection: ${e.text()}")
        false
    }

    // MARK: - Complete task
    suspend fun completeTask(task: ToDoTask): Boolean = try {
        val now = Instant.now()
        client.from("task").update({
            set("is_completed", true)
            set("date_completed", SupabaseDates.formatIso(now))
        }) { filter { eq("id", task.id) } }
        tasks = tasks.map { if (it.id == task.id) it.copy(isCompleted = true, dateCompleted = now) else it }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Update Failed", e.text())
        false
    }

    // MARK: - Uncomplete task
    suspend fun uncompleteTask(task: ToDoTask): Boolean = try {
        client.from("task").update({
            set("is_completed", false)
            setToNull("date_completed")
        }) { filter { eq("id", task.id) } }
        tasks = tasks.map { if (it.id == task.id) it.copy(isCompleted = false, dateCompleted = null) else it }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Update Failed", e.text())
        false
    }

    // MARK: - Delete Task
    suspend fun deleteTask(task: ToDoTask): Boolean = try {
        client.from("task").delete { filter { eq("id", task.id) } }
        tasks = tasks.filterNot { it.id == task.id }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Delete Failed", e.text())
        false
    }

    // MARK: - Save Note
    suspend fun saveNote(newNote: Note): Boolean = try {
        client.from("note").insert(newNote.copy(userID = currentUser!!.id))
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error", "Error creating Note, please try again. Error: ${e.text()}")
        false
    }

    // MARK: - Fetch user Notes
    suspend fun fetchUserNotes(): Boolean = try {
        val uid = authUserId()
        notes = client.from("note").select { filter { eq("user_id", uid) } }.decodeList<Note>()
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Error", "Error fetching your notes: ${e.text()}")
        false
    }

    // MARK: - Update Note
    suspend fun updateNote(note: Note, title: String, description: String, hex: String, isPinned: Boolean): Boolean = try {
        client.from("note").update({
            set("title", title)
            set("description", description)
            set("bg_color_hex", hex)
            set("is_pinned", isPinned)
        }) { filter { eq("id", note.id) } }
        notes = notes.map {
            if (it.id == note.id) it.copy(title = title, description = description, bgColorHex = hex, isPinned = isPinned) else it
        }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Update Failed", e.text())
        false
    }

    // MARK: - Update Pin status for Note
    suspend fun updatePinForNote(note: Note, isPinned: Boolean): Boolean = try {
        client.from("note").update({ set("is_pinned", isPinned) }) { filter { eq("id", note.id) } }
        notes = notes.map { if (it.id == note.id) it.copy(isPinned = isPinned) else it }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Pin Failed", e.text())
        false
    }

    // MARK: - Delete Note
    suspend fun deleteNote(note: Note): Boolean = try {
        client.from("note").delete { filter { eq("id", note.id) } }
        notes = notes.filterNot { it.id == note.id }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Delete Failed", e.text())
        false
    }

    // MARK: - Move Note
    suspend fun moveNote(note: Note, newCollectionID: String): Boolean = try {
        client.from("note").update({ set("collection_id", newCollectionID) }) { filter { eq("id", note.id) } }
        notes = notes.map { if (it.id == note.id) it.copy(collectionID = newCollectionID) else it }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Move Failed", "Failed to move Note to new Collection: ${e.text()}")
        false
    }

    // MARK: - Update user name
    suspend fun updateName(name: String): Boolean = try {
        val uid = authUserId()
        client.from("users").update({ set("full_name", name) }) { filter { eq("id", uid) } }
        currentUser = currentUser?.copy(name = name)
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError("Update Failed", e.text())
        false
    }

    // MARK: - Change password (Settings -> Security, as in the web app)
    /** Returns null on success, or the message to show. Signs the user out afterwards, like the web. */
    suspend fun changePassword(currentPassword: String, newPassword: String): String? {
        val email = currentUser?.email ?: return "An unexpected error occurred"
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = currentPassword
            }
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            return "Current password is incorrect"
        }
        return try {
            client.auth.updateUser { password = newPassword }
            null
        } catch (e: Exception) {
            rethrowIfCancelled(e)
            "Failed to update password: ${e.text()}"
        }
    }

    // MARK: - Delete Account
    suspend fun deleteAccount(): Boolean = try {
        client.postgrest.rpc("delete_user_account")
        client.auth.signOut()
        currentUser = null
        lists = emptyList()
        tasks = emptyList()
        notes = emptyList()
        collections = emptyList()
        isSignedIn = false
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        setError(
            "Deletion Failed",
            "We couldn't delete your account. You may need to re-authenticate (log out and back in) to perform this action."
        )
        false
    }

    // MARK: - Fetch all colours from Supabase
    suspend fun fetchAppColors(): Boolean = try {
        val colors = client.from("app_colors").select { order("created_at", Order.ASCENDING) }.decodeList<AppColor>()
        listColorHexes = colors.map { it.colorHex }
        true
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        false
    }

    // MARK: - Fetching all user data
    suspend fun initializeSession(): Boolean = try {
        val uid = authUserId()
        val users = client.from("users").select { filter { eq("id", uid) } }.decodeList<AppUser>()
        val fetchedUser = users.firstOrNull()
        if (fetchedUser == null) {
            false
        } else {
            currentUser = fetchedUser
            coroutineScope {
                listOf(
                    async { fetchUserLists() },
                    async { fetchUserCollections() },
                    async { fetchUserTasks() },
                    async { fetchUserNotes() },
                    async { fetchAppColors() },
                ).awaitAll().all { it }
            }
        }
    } catch (e: Exception) {
        rethrowIfCancelled(e)
        false
    }

    private suspend fun authUserId(): String = client.auth.retrieveUserForCurrentSession(updateSession = true).id

    companion object {
        private val EMAIL_REGEX =
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)+$")
        private val NAME_REGEX = Regex("^[a-zA-Z]+(?: [a-zA-Z]+)*$")

        private fun defaultList(icon: String, name: String, hex: String) = TodoList(
            id = UUID.randomUUID().toString(), createdAt = Instant.now(), listIcon = icon, listName = name,
            isDefault = true, bgColorHex = hex, userId = "", isPinned = false,
        )

        private fun rethrowIfCancelled(e: Exception) {
            if (e is CancellationException) throw e
        }

        /** The readable part of an error, like `localizedDescription` on iOS. */
        private fun Exception.text(): String =
            when (this) {
                is PostgrestRestException -> error
                is RestException -> description ?: error
                else -> message ?: toString()
            }
    }
}
