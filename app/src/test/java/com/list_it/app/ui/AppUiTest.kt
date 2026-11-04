package com.list_it.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.list_it.app.data.FakeSupabase
import com.list_it.app.data.Prefs
import com.list_it.app.data.Supabase
import com.list_it.app.data.createListItClient
import com.list_it.app.data.testPrefs
import com.list_it.app.model.SurfaceChoice
import com.list_it.app.model.ListDisplayStyle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * End-to-end UI tests: the real app UI and Supabase class against an in-memory Supabase server.
 * Each test also saves screenshots to app/build/screenshots.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi", application = TestApplication::class)
class AppUiTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var server: FakeSupabase
    private lateinit var prefs: Prefs
    private lateinit var db: Supabase

    @Before
    fun setUp() {
        server = FakeSupabase()
        prefs = testPrefs()
        db = Supabase(prefs, createListItClient("https://test.supabase.co", "anon", server.engine))
    }

    // ---------- helpers ----------

    private fun shot(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage("build/screenshots/$name.png")
    }

    private fun waitForText(text: String, substring: Boolean = false) =
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

    private fun click(text: String) {
        waitForText(text)
        val node = compose.onAllNodesWithText(text)[0]
        runCatching { node.performScrollTo() } // bring it on screen when it sits in a scrolling column
        node.performClick()
        compose.waitForIdle()
    }

    private fun type(description: String, text: String) {
        val node = compose.onNodeWithContentDescription(description)
        runCatching { node.performScrollTo() }
        node.performTextInput(text)
    }

    private fun row(s: String) = Json.parseToJsonElement(s) as JsonObject

    private fun iso(daysFromToday: Long, hour: Int = 18): String =
        LocalDate.now().plusDays(daysFromToday).atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toString()

    /** Seeds a user with lists, collections, tasks and notes. */
    private fun seedData(): FakeSupabase.Account {
        val acc = server.seedUser()
        val uid = acc.id
        listOf("#FF9500", "#007AFF", "#34C759", "#FF3B30", "#AF52DE", "#5856D6", "#FF2D55", "#32ADE6").forEachIndexed { i, hex ->
            server.tables["app_colors"]!! += row("""{"id":$i,"color_hex":"$hex","color_name":"c$i","created_at":"2026-01-0${i + 1}T00:00:00Z"}""")
        }
        fun list(id: String, name: String, hex: String, pinned: Boolean, day: Int) =
            row("""{"id":"$id","created_at":"2026-01-0$day 09:00:00","list_icon":"checklist","list_name":"$name","is_default":false,"bg_color_hex":"$hex","user_id":"$uid","is_pinned":$pinned}""")
        server.tables["list"]!! += listOf(list("l1", "Work", "#007AFF", true, 1), list("l2", "School", "#34C759", false, 2), list("l3", "Home", "#FF9500", false, 3))
        fun col(id: String, name: String, list: String, hex: String) =
            row("""{"id":"$id","created_at":"2026-01-05T09:00:00","collection_name":"$name","bg_color_hex":"$hex","list_id":"$list","user_id":"$uid"}""")
        server.tables["collection"]!! += listOf(col("c1", "General", "l1", "#007AFF"), col("c2", "Projects", "l1", "#AF52DE"), col("c3", "General", "l2", "#34C759"), col("c4", "General", "l3", "#FF9500"))
        fun task(id: String, text: String, col: String, list: String, due: String?, pinned: Boolean = false, done: String? = null) =
            row("""{"id":"$id","created_at":"2026-03-01T10:00:00Z","text":"$text","description":"Details for $text","due_date":${due?.let { "\"$it\"" } ?: "null"},"is_completed":${done != null},"date_completed":${done?.let { "\"$it\"" } ?: "null"},"is_deleted":false,"is_pinned":$pinned,"user_id":"$uid","collection_id":"$col","list_id":"$list"}""")
        server.tables["task"]!! += listOf(
            task("t1", "Prepare slides", "c1", "l1", iso(0), pinned = true),
            task("t2", "Email client", "c1", "l1", iso(1)),
            task("t3", "Fix login bug", "c2", "l1", iso(-3)),
            task("t4", "Maths homework", "c3", "l2", null, pinned = true),
            task("t5", "Buy groceries", "c4", "l3", null, done = Instant.now().minusSeconds(600).toString()),
        )
        server.tables["note"]!! += row("""{"id":"n1","created_at":"2026-01-05 09:00:00","title":"Meeting notes","description":"## Agenda\n- Budget\n- **Launch**","is_deleted":false,"bg_color_hex":"#FFCC00","is_pinned":true,"collection_id":"c1","user_id":"$uid","list_id":"l1"}""")
        return acc
    }

    private fun launchSignedIn() {
        val acc = seedData()
        runBlocking { assertTrue(db.signIn(acc.email, acc.password)) }
        prefs.isSignedIn = true
        compose.setContent { ListItApp(db, prefs) }
        waitForText("Welcome Back 👋")
        waitForText("Work")
    }

    // ---------- Onboarding & auth ----------

    @Test
    fun introSignUpValidationAndEmailConfirmation() {
        compose.setContent { ListItApp(db, prefs) }
        waitForText("Start with a List")
        compose.mainClock.advanceTimeBy(2_000)
        shot("01_intro")
        click("Skip")
        waitForText("Already have an account?")
        shot("02_sign_up")

        // Sign Up stays disabled until the privacy policy is accepted.
        click("Sign Up")
        assertTrue(compose.onAllNodesWithText("Form incomplete").fetchSemanticsNodes().isEmpty())
        compose.onNode(isToggleable()).apply { runCatching { performScrollTo() } }.performClick()
        compose.onNode(isToggleable()).assertIsOn()
        click("Sign Up")
        waitForText("Form incomplete")
        shot("03_sign_up_alert")
        click("OK")

        type("Full Name", "New User")
        type("Email", "new@test.com")
        type("Password", "Abc")
        type("Confirm Password", "Abc")
        click("Sign Up")
        waitForText("Invalid input")
        click("OK")

        // Valid details → account created and the email confirmation sheet opens
        type("Password", "def12!")
        type("Confirm Password", "def12!")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Sign Up").performClick()
        repeat(40) { compose.mainClock.advanceTimeBy(250); Thread.sleep(25) }
        compose.mainClock.advanceTimeBy(1_000)
        assertTrue(compose.onAllNodesWithText("Check Your Email").fetchSemanticsNodes().isNotEmpty())
        captureScreenRoboImage("build/screenshots/04_email_confirmation.png")
        assertEquals("new@test.com", server.accounts.single().email)
    }

    @Test
    fun loginShowsHomeScreen() {
        seedData()
        compose.setContent { ListItApp(db, prefs) }
        click("Skip")
        click("Login")
        waitForText("Forgot password?")
        type("Email", "abdul@test.com")
        type("Password", "wrong1")
        shot("05_login")
        click("Log In")
        waitForText("Login Error")
        click("OK")
        db.password = "secret1" // the password field is bound to db.password
        click("Log In")
        waitForText("Welcome Back 👋")
        waitForText("Your Lists")
        shot("06_home_cards")
        assertTrue(prefs.isSignedIn)
    }

    // ---------- Home, tabs and settings ----------

    @Test
    fun homeMenuTabsAndSettings() {
        launchSignedIn()
        compose.onNodeWithContentDescription("Menu").performClick()
        waitForText("Today's Tasks")
        assertEquals(1, compose.onAllNodesWithText("Settings").fetchSemanticsNodes().size) // only the tab, not a menu row
        shot("07_home_menu")
        compose.onNodeWithText("Today's Tasks").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        waitForText("Your Lists")

        // Progress tab
        click("Progress")
        waitForText("Activity")
        waitForText("20% of tasks completed")
        shot("08_progress")

        // Settings tab: Appearance
        click("Settings")
        waitForText("Manage your account, security, and preferences")
        shot("09_settings")
        click("Appearance")
        waitForText("Background")
        shot("10_appearance_light")
        compose.onNodeWithText("Ice blue").performClick()
        assertEquals(SurfaceChoice.COOL, prefs.surface(false))
        compose.onNodeWithContentDescription("Dark mode").performClick()
        assertEquals(true, prefs.darkMode)
        waitForText("Midnight Velvet")
        compose.onNodeWithText("Midnight Velvet").performClick()
        assertEquals(SurfaceChoice.WARM, prefs.surface(true))
        compose.onNodeWithText("One full-width row each").performClick()
        assertEquals(ListDisplayStyle.LIST, prefs.displayStyle)
        shot("11_appearance_dark")

        // Profile: rename
        click("Profile")
        waitForText("Read-only")
        compose.onNodeWithContentDescription("Full Name").performTextReplacement("Abdul Moiz Jr")
        click("Save Changes")
        waitForText("Profile updated successfully")
        assertEquals("Abdul Moiz Jr", db.currentUser?.name)
        shot("12_profile")

        // Security: the server's password rule is enforced before any request.
        click("Security")
        type("Current Password", "secret1")
        type("New Password", "weak")
        type("Confirm New Password", "weak")
        click("Update Password")
        waitForText("Password must be at least 8 characters")
        shot("13_security")

        // Notifications toggle
        click("Notifications")
        compose.onNodeWithContentDescription("Task reminders").performClick()
        assertEquals(false, prefs.remindersEnabled)

        // Back to Lists in the chosen row layout and dark background.
        click("Lists")
        waitForText("Your Lists")
        shot("14_home_rows_velvet")
    }

    @Test
    fun pinnedRailAndContextMenu() {
        prefs.darkMode = true
        prefs.setSurface(true, SurfaceChoice.SOFT)
        launchSignedIn()
        runBlocking {
            db.updateListPin(db.lists.first { it.id == "l2" }, true)
            db.updateListPin(db.lists.first { it.id == "l3" }, true)
            db.updateListPin(db.defaultLists.first(), true)
        }
        waitForText("Pinned Lists")
        shot("15_pinned_rail_midnight")
        compose.onAllNodesWithText("Work")[0].performTouchInput { longClick() }
        waitForText("Unpin List")
        shot("16_pinned_context_menu")
    }

    @Test
    fun createListFlow() {
        launchSignedIn()
        compose.onNodeWithContentDescription("Menu").performClick()
        click("Create List")
        waitForText("Create a new List to stay organised.")
        type("e.g. Personal, Work, Fitness", "Fitness")
        click("Create")
        waitForText("Please enter a name and choose a color.")
        click("OK")
        compose.onNodeWithContentDescription("Colour #34C759").performClick()
        shot("32_add_list")
        click("Create")
        compose.waitUntil(10_000) { db.lists.any { it.listName == "Fitness" } }
        waitForText("Fitness")
        assertTrue(server.tables["collection"]!!.any { it["collection_name"]!!.jsonPrimitive.content == "General" && it["list_id"]!!.jsonPrimitive.content == db.lists.first { l -> l.listName == "Fitness" }.id })
        shot("33_home_after_create")
    }

    // ---------- List detail, tasks and notes ----------

    @Test
    fun listDetailTaskViewEditAndComplete() {
        launchSignedIn()
        compose.onAllNodesWithText("Work")[0].performClick()
        waitForText("Hold items for more options")
        shot("17_list_detail")
        click("General")
        waitForText("Prepare slides")
        shot("18_collection_expanded")
        click("Prepare slides")
        waitForText("Task Details")
        waitForText("Incomplete")
        shot("19_task_view")

        // Edit, change the title, save: back to the view with the new title.
        click("Edit")
        waitForText("Edit Task")
        shot("20_task_edit")
        compose.onNodeWithContentDescription("Task title").performTextReplacement("Prepare slides v2")
        click("Save")
        compose.waitUntil(10_000) { db.tasks.first { it.id == "t1" }.text == "Prepare slides v2" }
        waitForText("Task Details")
        waitForText("Prepare slides v2")

        // Cancel discards edits.
        click("Edit")
        compose.onNodeWithContentDescription("Task title").performTextReplacement("junk")
        click("Cancel")
        waitForText("Task Details")
        assertEquals("Prepare slides v2", db.tasks.first { it.id == "t1" }.text)

        // Pin and Complete apply straight from the view.
        click("Unpin")
        compose.waitUntil(10_000) { !db.tasks.first { it.id == "t1" }.isPinned }
        click("Complete")
        compose.waitUntil(10_000) { db.tasks.first { it.id == "t1" }.isCompleted }
        assertTrue(server.tables["task"]!!.first { it["id"]!!.jsonPrimitive.content == "t1" }["is_completed"]!!.jsonPrimitive.content == "true")
    }

    @Test
    fun addTaskAndNoteViewEdit() {
        launchSignedIn()
        compose.onAllNodesWithText("Work")[0].performClick()
        waitForText("Hold items for more options")
        compose.onNodeWithContentDescription("More").performClick()
        click("Add Task")
        compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasContentDescription("Task title")).fetchSemanticsNodes().isNotEmpty() }
        type("Task title", "Write report")
        shot("21_add_task")
        click("Create")
        compose.waitUntil(10_000) { db.tasks.any { it.text == "Write report" } }
        assertEquals("c1", db.tasks.first { it.text == "Write report" }.collectionID)

        click("General")
        click("Notes")
        waitForText("Meeting notes")
        shot("22_notes_tab")
        click("Meeting notes")
        waitForText("Note Details")
        waitForText("Agenda")
        waitForText("Launch", substring = true)
        shot("23_note_view")
        click("Edit")
        waitForText("Edit Note")
        click("Markdown Preview")
        shot("24_note_edit")
        click("Save")
        waitForText("Note Details")
    }

    // ---------- Default lists ----------

    @Test
    fun defaultListScreens() {
        launchSignedIn()
        val screens = listOf(
            Triple("Today's Tasks", "Prepare slides", "25_today"),
            Triple("Tomorrow's Tasks", "Email client", "26_tomorrow"),
            Triple("Scheduled Tasks", "Nothing Scheduled", "27_scheduled"),
            Triple("Priority Tasks", "Maths homework", "28_priority"),
            Triple("Completed Tasks", "Buy groceries", "29_completed"),
            Triple("Not Completed Tasks", "Fix login bug", "30_pending"),
            Triple("Overdue Tasks", "Fix login bug", "31_overdue"),
        )
        screens.forEach { (menu, expect, file) ->
            compose.onNodeWithContentDescription("Menu").performClick()
            click(menu)
            compose.waitUntil(10_000) { compose.onAllNodes(hasText(expect, substring = true)).fetchSemanticsNodes().isNotEmpty() || expect == "Nothing Scheduled" }
            shot(file)
            compose.onNodeWithContentDescription("Back").performClick()
            waitForText("Your Lists")
        }
    }

    @Test
    fun completedUndoRestoresTask() {
        launchSignedIn()
        compose.onNodeWithContentDescription("Menu").performClick()
        click("Completed Tasks")
        waitForText("Buy groceries")
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.waitUntil(10_000) { !db.tasks.first { it.id == "t5" }.isCompleted }
        waitForText("No Completed Tasks")
    }
}
