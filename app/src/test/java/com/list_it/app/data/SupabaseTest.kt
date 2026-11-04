package com.list_it.app.data

import com.list_it.app.model.ListCollection
import com.list_it.app.model.Note
import com.list_it.app.model.SortOption
import com.list_it.app.model.SupabaseDates
import com.list_it.app.model.ToDoTask
import com.list_it.app.model.TodoList
import com.list_it.app.model.VerificationStatus
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupabaseTest {
    private lateinit var server: FakeSupabase
    private lateinit var prefs: Prefs
    private lateinit var db: Supabase

    @BeforeTest
    fun setUp() = runBlocking {
        server = FakeSupabase()
        prefs = testPrefs()
        val client = createListItClient("https://test.supabase.co", "anon", server.engine)
        client.auth.awaitInitialization()
        client.auth.clearSession()
        db = Supabase(prefs, client)
    }

    private fun seedColors() {
        listOf("#FF9500", "#007AFF").forEachIndexed { i, hex ->
            server.tables["app_colors"]!! += buildJsonObject {
                put("id", JsonPrimitive(i)); put("color_hex", JsonPrimitive(hex))
                put("color_name", JsonPrimitive("c$i")); put("created_at", JsonPrimitive("2026-01-0${i + 1}T00:00:00Z"))
            }
        }
    }

    private suspend fun signedIn(): FakeSupabase.Account {
        val acc = server.seedUser()
        seedColors()
        assertTrue(db.signIn(acc.email, acc.password), "sign in should succeed: ${db.alertMessage}")
        return acc
    }

    // ---------- Validation (identical regexes to iOS) ----------

    @Test
    fun validationRules() {
        assertTrue(db.isValidEmail("a.b+c@mail.co.uk"))
        assertFalse(db.isValidEmail("no-at-sign.com"))
        assertFalse(db.isValidEmail("a@b"))
        db.name = "  Abdul Moiz "; assertTrue(db.isValidName())
        db.name = "A"; assertFalse(db.isValidName())
        db.name = "Abdul1"; assertFalse(db.isValidName())
        db.password = "abc123"; assertFalse(db.isStrongPassword())
        db.password = "Abcdef12"; assertFalse(db.isStrongPassword())
        db.password = "Abcdef12!"; assertTrue(db.isStrongPassword())
        db.confirmPassword = "Abcdef12?"; assertFalse(db.passwordsMatch())
        db.confirmPassword = "Abcdef12!"; assertTrue(db.passwordsMatch())
        db.resetFields()
        assertEquals("", db.name + db.email + db.password + db.confirmPassword)
    }

    // ---------- Dates ----------

    @Test
    fun parsesEverySupabaseDateFormat() {
        val expected = Instant.parse("2026-03-28T23:16:43Z")
        assertEquals(expected, SupabaseDates.parse("2026-03-28T23:16:43"))
        assertEquals(expected, SupabaseDates.parse("2026-03-28 23:16:43"))
        assertEquals(expected, SupabaseDates.parse("2026-03-28T23:16:43+00:00"))
        assertEquals(expected.plusMillis(808), SupabaseDates.parse("2026-03-28T23:16:43.808"))
        assertEquals(expected.plusMillis(808), SupabaseDates.parse("2026-03-28 23:16:43.808"))
        assertEquals(expected.plusMillis(808), SupabaseDates.parse("2026-03-28T23:16:43.808Z"))
        assertEquals(expected.plusNanos(808123000), SupabaseDates.parse("2026-03-28T23:16:43.808123+00:00"))
        assertEquals("2026-03-28 23:16:43", SupabaseDates.formatPlain(expected))
    }

    // ---------- Auth flows ----------

    @Test
    fun signUpVerifyAndSignInFlow() = runBlocking {
        assertTrue(db.isEmailAvailable("new@test.com"))
        val (id, ok) = db.createUser("New User", "new@test.com", "pass123")
        assertTrue(ok); assertTrue(id.isNotEmpty())
        assertEquals("New User", server.accounts.single().name)

        // Not confirmed yet → stays pending
        db.verificationCheck(id, "New User", "new@test.com", "pass123")
        assertEquals(VerificationStatus.Pending, db.verificationStatus)
        assertTrue(server.tables["users"]!!.isEmpty())

        // User taps the email link
        server.accounts.single().confirmed = true
        db.verificationCheck(id, "New User", "new@test.com", "pass123")
        assertEquals(VerificationStatus.Verified, db.verificationStatus)
        assertTrue(prefs.isSignedIn)
        val row = server.tables["users"]!!.single()
        assertEquals(id, row["id"]!!.jsonPrimitive.content)
        assertEquals("New User", row["full_name"]!!.jsonPrimitive.content)

        // Email now taken
        assertFalse(db.isEmailAvailable("new@test.com"))

        // redirectUser(): sign in loads the session
        assertTrue(db.signIn("new@test.com", "pass123"))
        assertEquals("New User", db.currentUser?.name)
    }

    @Test
    fun cancelVerificationDeletesPendingUser() = runBlocking {
        val (id, _) = db.createUser("Temp User", "temp@test.com", "pass123")
        assertTrue(db.deleteAccountById(id))
        assertTrue(server.accounts.isEmpty())
        assertTrue(server.requests.any { it.contains("/rest/v1/rpc/delete_user_by_id") })
    }

    @Test
    fun signInErrors() = runBlocking {
        server.seedUser()
        assertFalse(db.signIn("", "x")); assertEquals("Email and Password must be filled in.", db.alertMessage)
        assertFalse(db.signIn("bad", "x")); assertEquals("Invalid Email", db.alertMessage)
        assertFalse(db.signIn("abdul@test.com", "wrong1"))
        assertEquals("Login Error", db.alertTitle)
        assertTrue(db.alertMessage.contains("Invalid login credentials"), db.alertMessage)
        assertFalse(prefs.isSignedIn)
    }

    @Test
    fun signInLoadsEverything() = runBlocking {
        val acc = server.seedUser()
        seedColors()
        server.tables["list"]!! += json("""{"id":"l1","created_at":"2026-01-05 09:00:00","list_icon":"checklist","list_name":"Work","is_default":false,"bg_color_hex":"#007AFF","user_id":"${acc.id}","is_pinned":true}""")
        server.tables["list"]!! += json("""{"id":"other","created_at":"2026-01-05 09:00:00","list_icon":"checklist","list_name":"Not mine","is_default":false,"bg_color_hex":"#007AFF","user_id":"someone","is_pinned":false}""")
        server.tables["collection"]!! += json("""{"id":"c1","created_at":"2026-01-05T09:00:00","collection_name":"General","bg_color_hex":"#007AFF","list_id":"l1","user_id":"${acc.id}"}""")
        server.tables["task"]!! += json("""{"id":"t1","created_at":"2026-03-28T23:16:43.808+00:00","text":"Ship","description":null,"due_date":"2026-04-01T12:00:00","is_completed":false,"date_completed":null,"is_deleted":false,"is_pinned":false,"user_id":"${acc.id}","collection_id":"c1","list_id":"l1"}""")
        server.tables["note"]!! += json("""{"id":"n1","created_at":"2026-01-05 09:00:00","title":"Idea","description":"x","is_deleted":false,"bg_color_hex":"#FFCC00","is_pinned":false,"collection_id":"c1","user_id":"${acc.id}","list_id":"l1"}""")

        assertTrue(db.signIn("ABDUL@test.com".lowercase(), "secret1"), db.alertMessage)
        assertTrue(prefs.isSignedIn)
        assertEquals("Abdul Moiz", db.currentUser?.name)
        assertNotNull(db.currentUser?.createdAt)
        assertEquals(listOf("Work"), db.lists.map { it.listName })
        assertEquals(1, db.collections.size)
        assertEquals(Instant.parse("2026-04-01T12:00:00Z"), db.tasks.single().dueDate)
        assertEquals("Idea", db.notes.single().title)
        assertEquals(listOf("#FF9500", "#007AFF"), db.listColorHexes)
        assertEquals(listOf("Work"), db.pinnedLists("").map { it.listName })
        assertEquals(emptyList(), db.pinnedLists("zzz"))
    }

    @Test
    fun resetPasswordValidationAndSend() = runBlocking {
        db.forgotPasswordEmail = ""
        assertFalse(db.resetPassword()); assertEquals("Email field cannot be empty.", db.alertMessage)
        db.forgotPasswordEmail = "nope"
        assertFalse(db.resetPassword()); assertEquals("Invalid email address. Please try again.", db.alertMessage)
        db.forgotPasswordEmail = "abdul@test.com"
        assertTrue(db.resetPassword())
        assertEquals(listOf("abdul@test.com"), server.resetEmails)
    }

    @Test
    fun signOutAndDeleteAccount() = runBlocking {
        signedIn()
        db.signOutUser()
        assertNull(db.currentUser); assertFalse(prefs.isSignedIn); assertTrue(db.lists.isEmpty())

        signedIn2()
        assertTrue(db.deleteAccount())
        assertNull(db.currentUser); assertFalse(prefs.isSignedIn)
        assertTrue(server.accounts.none { it.email == "abdul@test.com" })
    }

    private suspend fun signedIn2() {
        assertTrue(db.signIn("abdul@test.com", "secret1"), db.alertMessage)
    }

    @Test
    fun updateName() = runBlocking {
        signedIn()
        assertTrue(db.updateName("New Name"))
        assertEquals("New Name", db.currentUser?.name)
        assertEquals("New Name", server.tables["users"]!!.single()["full_name"]!!.jsonPrimitive.content)
    }

    // ---------- Lists & collections ----------

    @Test
    fun listLifecycle() = runBlocking {
        val acc = signedIn()
        val list = TodoList("l1", Instant.now(), "checklist", "Home", false, "#007AFF", "", false)
        val general = ListCollection(UUID.randomUUID().toString().uppercase(), Instant.now(), "General", "#007AFF", "l1", "")
        assertTrue(db.saveList(list, general))
        val savedList = server.tables["list"]!!.single()
        assertEquals(acc.id, savedList["user_id"]!!.jsonPrimitive.content)
        assertTrue(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}").matches(savedList["created_at"]!!.jsonPrimitive.content))
        assertEquals("General", server.tables["collection"]!!.single()["collection_name"]!!.jsonPrimitive.content)

        assertTrue(db.fetchUserLists()); assertTrue(db.fetchUserCollections())
        assertTrue(db.updateList(db.lists.single(), "House", "#FF9500"))
        assertEquals("House", db.lists.single().listName)
        assertEquals("House", server.tables["list"]!!.single()["list_name"]!!.jsonPrimitive.content)

        assertTrue(db.updateListPin(db.lists.single(), true))
        assertTrue(db.lists.single().isPinned)
        assertEquals("true", server.tables["list"]!!.single()["is_pinned"]!!.jsonPrimitive.content)

        // Default lists are pinned locally only
        val before = server.requests.size
        assertTrue(db.updateListPin(db.defaultLists.first(), true))
        assertTrue(db.defaultLists.first().isPinned)
        assertEquals(before, server.requests.size)

        // Collections
        val maths = ListCollection("c2", Instant.now(), "Maths", "#FF9500", "l1", "")
        assertTrue(db.saveCollection(maths)); assertTrue(db.fetchUserCollections())
        assertEquals(2, db.collections.size)
        assertTrue(db.updateCollection(db.collections.first { it.id == "c2" }, "Algebra", "#000000"))
        assertEquals("Algebra", db.collections.first { it.id == "c2" }.collectionName)

        // Delete collection removes its tasks/notes locally
        db.tasks = listOf(task("t1", "c2"))
        db.notes = listOf(note("n1", "c2"))
        assertTrue(db.deleteCollection(db.collections.first { it.id == "c2" }))
        assertTrue(db.tasks.isEmpty()); assertTrue(db.notes.isEmpty())
        assertEquals(1, db.collections.size)

        // Delete list cascades locally
        db.tasks = listOf(task("t2", db.collections.single().id))
        assertTrue(db.deleteList(db.lists.single()))
        assertTrue(db.lists.isEmpty()); assertTrue(db.collections.isEmpty()); assertTrue(db.tasks.isEmpty())
        assertTrue(server.tables["list"]!!.isEmpty())
    }

    @Test
    fun sortingAndSearchMatchIos() {
        fun l(name: String, day: Int, def: Boolean = false) =
            TodoList(name, Instant.parse("2026-01-%02dT00:00:00Z".format(day)), "", name, def, "#000", "", false)
        db.lists = listOf(l("beta", 3), l("Alpha", 1), l("gamma", 2))
        assertEquals(listOf("Alpha", "gamma", "beta"), db.getFilteredLists("", SortOption.OLDEST).map { it.listName })
        assertEquals(listOf("beta", "gamma", "Alpha"), db.getFilteredLists("", SortOption.NEWEST).map { it.listName })
        assertEquals(listOf("Alpha", "beta", "gamma"), db.getFilteredLists("", SortOption.ALPHABETICAL_AZ).map { it.listName })
        assertEquals(listOf("gamma", "beta", "Alpha"), db.getFilteredLists("", SortOption.ALPHABETICAL_ZA).map { it.listName })
        assertEquals(listOf("Alpha"), db.getFilteredLists("ALP", SortOption.OLDEST).map { it.listName })
    }

    // ---------- Tasks ----------

    @Test
    fun taskLifecycle() = runBlocking {
        val acc = signedIn()
        val t = task("t1", "c1").copy(dueDate = Instant.parse("2026-05-01T09:30:00Z"))
        assertTrue(db.saveTask(t))
        val row = server.tables["task"]!!.single()
        assertEquals(acc.id, row["user_id"]!!.jsonPrimitive.content)
        assertEquals("2026-05-01T09:30:00Z", row["due_date"]!!.jsonPrimitive.content)
        db.tasks = db.tasks + t

        assertTrue(db.updateTask(t, "Renamed", "Desc", null, true))
        with(db.tasks.single()) { assertEquals("Renamed", text); assertNull(dueDate); assertTrue(isPinned) }
        assertTrue(server.tables["task"]!!.single()["due_date"] is JsonNull)

        assertTrue(db.updatePinForTask(db.tasks.single(), false)); assertFalse(db.tasks.single().isPinned)
        assertTrue(db.moveTask(db.tasks.single(), "c9")); assertEquals("c9", db.tasks.single().collectionID)
        assertEquals("c9", server.tables["task"]!!.single()["collection_id"]!!.jsonPrimitive.content)

        assertTrue(db.completeTask(db.tasks.single()))
        assertTrue(db.tasks.single().isCompleted); assertNotNull(db.tasks.single().dateCompleted)
        assertNotNull(SupabaseDates.parse(server.tables["task"]!!.single()["date_completed"]!!.jsonPrimitive.content))

        assertTrue(db.uncompleteTask(db.tasks.single()))
        assertFalse(db.tasks.single().isCompleted); assertNull(db.tasks.single().dateCompleted)

        // Round trip through fetch
        assertTrue(db.fetchUserTasks()); assertEquals("Renamed", db.tasks.single().text)

        assertTrue(db.deleteTask(db.tasks.single()))
        assertTrue(db.tasks.isEmpty()); assertTrue(server.tables["task"]!!.isEmpty())
    }

    // ---------- Notes ----------

    @Test
    fun noteLifecycle() = runBlocking {
        signedIn()
        val n = note("n1", "c1")
        assertTrue(db.saveNote(n)); db.notes = db.notes + n
        assertTrue(db.updateNote(n, "Title 2", "Body", "#FF0000", true))
        with(db.notes.single()) { assertEquals("Title 2", title); assertEquals("#FF0000", bgColorHex); assertTrue(isPinned) }
        assertTrue(db.updatePinForNote(db.notes.single(), false)); assertFalse(db.notes.single().isPinned)
        assertTrue(db.moveNote(db.notes.single(), "c5")); assertEquals("c5", db.notes.single().collectionID)
        assertTrue(db.fetchUserNotes()); assertEquals("Title 2", db.notes.single().title)
        assertTrue(db.deleteNote(db.notes.single())); assertTrue(db.notes.isEmpty())
    }

    // ---------- Errors surface the same alert titles/messages as iOS ----------

    @Test
    fun failuresSetAlerts() = runBlocking {
        signedIn()
        server.failTable = "task"
        assertFalse(db.saveTask(task("t1", "c1")))
        assertEquals("Error", db.alertTitle)
        assertTrue(db.alertMessage.startsWith("Error creating Task, please try again. Error: "), db.alertMessage)
        assertTrue(db.alertMessage.contains("boom from task"), db.alertMessage)
        assertFalse(db.deleteTask(task("t1", "c1"))); assertEquals("Delete Failed", db.alertTitle)
        server.failTable = "note"
        assertFalse(db.moveNote(note("n1", "c1"), "c2")); assertEquals("Move Failed", db.alertTitle)
        assertTrue(db.alertMessage.startsWith("Failed to move Note to new Collection: "))
    }

    // ---------- Reminders ----------

    @Test
    fun reminderStorePersists() {
        val p = testPrefs()
        ReminderStore.init(p)
        val at = Instant.parse("2026-12-01T10:00:00Z")
        ReminderStore.set(at, "t1")
        ReminderStore.init(p) // reload from storage
        assertEquals(at, ReminderStore.date("t1"))
        ReminderStore.remove("t1")
        assertNull(ReminderStore.date("t1"))
    }

    // ---------- helpers ----------

    private fun json(s: String) = Json.parseToJsonElement(s) as JsonObject

    private fun task(id: String, collectionID: String) = ToDoTask(
        id = id, createdAt = Instant.parse("2026-03-01T10:00:00Z"), text = "Task $id", description = "", dueDate = null,
        isCompleted = false, isDeleted = false, isPinned = false, userID = "", collectionID = collectionID, listID = "l1",
    )

    private fun note(id: String, collectionID: String) = Note(
        id = id, createdAt = Instant.parse("2026-03-01T10:00:00Z"), title = "Note $id", description = null, isDeleted = false,
        bgColorHex = "#FFCC00", isPinned = false, collectionID = collectionID, userID = "", listID = "l1",
    )
}
