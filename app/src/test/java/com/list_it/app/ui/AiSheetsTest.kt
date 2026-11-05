package com.list_it.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.google.mlkit.genai.common.GenAiException
import com.list_it.app.data.FakeSupabase
import com.list_it.app.data.FoundationModelManager
import com.list_it.app.data.Supabase
import com.list_it.app.data.createListItClient
import com.list_it.app.data.friendlyMessage
import com.list_it.app.data.testPrefs
import com.list_it.app.model.AIFeatureMode
import com.list_it.app.ui.components.IosSheet
import com.list_it.app.ui.screens.ai.CreateNoteAISheet
import com.list_it.app.ui.screens.ai.CreateTaskAISheet
import com.list_it.app.ui.screens.ai.DescriptionTransformSheet
import com.list_it.app.ui.theme.ListItTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The AI sheets with a fake model in place of Gemini Nano; screenshots go to app/build/screenshots/ai_*. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi", application = TestApplication::class)
class AiSheetsTest {
    @get:Rule val compose = createComposeRule()

    private val sample = """
        ### Prep
        - Clear the counters and load the dishwasher
        - Throw out expired food from the fridge
        ### Clean
        - Wipe down the hob and **degrease** the oven door
        - Mop the floor
    """.trimIndent()

    private fun host(dark: Boolean = false, content: @Composable () -> Unit) {
        val prefs = testPrefs()
        prefs.darkMode = dark
        val db = Supabase(prefs, createListItClient("https://test.supabase.co", "anon", FakeSupabase().engine))
        compose.setContent {
            CompositionLocalProvider(LocalDb provides db, LocalPrefs provides prefs) {
                ListItTheme(prefs.darkMode, prefs::surface) { content() }
            }
        }
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage("build/screenshots/ai_$name.png")
    }

    private fun typeIntoPrompt(text: String) = compose.onNode(hasSetTextAction()).performTextInput(text)

    @Test
    fun taskSheetGeneratesAndShowsOutput() {
        val manager = FoundationModelManager { _, _ -> sample }
        var inserted: String? = null
        host { IosSheet(onDismiss = {}) { close -> CreateTaskAISheet(close, manager) { inserted = it } } }
        shot("01_task_empty")
        typeIntoPrompt("Clean the messy kitchen")
        compose.onNodeWithText("Generate Subtasks").performClick()
        compose.waitUntil(5_000) { manager.aiResponse.isNotEmpty() }
        shot("02_task_output")
        compose.onNodeWithText("Insert into Description").performClick()
        assertEquals(sample, inserted)
    }

    @Test
    fun taskSheetShowsFriendlyErrorAndRetries() {
        var calls = 0
        val manager = FoundationModelManager { _, _ ->
            calls++
            if (calls == 1) throw GenAiException("raw aicore failure", null, GenAiException.ErrorCode.BUSY) else sample
        }
        host { IosSheet(onDismiss = {}) { close -> CreateTaskAISheet(close, manager) {} } }
        typeIntoPrompt("Clean the messy kitchen")
        compose.onNodeWithText("Generate Subtasks").performClick()
        compose.waitUntil(5_000) { manager.errorMessage != null }
        assertTrue("raw exception text must not reach the user", compose.onAllNodesWithText("raw aicore failure", substring = true).fetchSemanticsNodes().isEmpty())
        shot("03_task_error")
        compose.onNodeWithText("Try Again").performClick()
        compose.waitUntil(5_000) { manager.aiResponse.isNotEmpty() }
        assertEquals(null, manager.errorMessage)
    }

    @Test
    fun firstRunShowsModelDownload() {
        val gate = CompletableDeferred<Unit>()
        val manager = FoundationModelManager { _, preparing -> preparing(); gate.await(); sample }
        host { IosSheet(onDismiss = {}) { close -> CreateTaskAISheet(close, manager) {} } }
        typeIntoPrompt("Clean the messy kitchen")
        compose.onNodeWithText("Generate Subtasks").performClick()
        compose.waitUntil(5_000) { manager.isPreparingModel }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(500)
        captureScreenRoboImage("build/screenshots/ai_04_task_downloading.png")
        compose.mainClock.autoAdvance = true
        gate.complete(Unit)
        compose.waitUntil(5_000) { manager.aiResponse.isNotEmpty() }
    }

    @Test
    fun noteSheetDarkMode() {
        val manager = FoundationModelManager { _, _ -> sample }
        host(dark = true) { IosSheet(onDismiss = {}) { close -> CreateNoteAISheet(close, manager) {} } }
        shot("05_note_dark_empty")
        typeIntoPrompt("Spring cleaning")
        compose.onNodeWithText("Generate Outline").performClick()
        compose.waitUntil(5_000) { manager.aiResponse.isNotEmpty() }
        shot("06_note_dark_output")
    }

    @Test
    fun switchingModeClearsOldOutput() {
        val manager = FoundationModelManager { _, _ -> sample }
        host { IosSheet(onDismiss = {}) { close -> CreateTaskAISheet(close, manager) {} } }
        typeIntoPrompt("Clean the messy kitchen")
        compose.onNodeWithText("Generate Subtasks").performClick()
        compose.waitUntil(5_000) { manager.aiResponse.isNotEmpty() }
        compose.onNodeWithText(AIFeatureMode.PLANNER.title).performClick()
        compose.waitForIdle()
        assertEquals("", manager.aiResponse)
    }

    @Test
    fun descriptionTransformRunsImmediately() {
        val manager = FoundationModelManager { _, _ -> sample }
        var applied: String? = null
        host { IosSheet(onDismiss = {}) { close -> DescriptionTransformSheet("clean kitchen, floor, oven", AIFeatureMode.REWRITE_LIST, close, manager) { applied = it } } }
        compose.waitUntil(5_000) { manager.aiResponse.isNotEmpty() }
        shot("07_transform_output")
        compose.onNodeWithText("Replace Description").performClick()
        assertEquals(sample, applied)
    }

    @Test
    fun customPromptAsksFirst() {
        val manager = FoundationModelManager { _, _ -> sample }
        host { IosSheet(onDismiss = {}) { close -> DescriptionTransformSheet("clean kitchen", AIFeatureMode.CUSTOM, close, manager) {} } }
        shot("08_custom_prompt")
        typeIntoPrompt("Make it a checklist")
        compose.onNodeWithText("Run Custom Prompt").performClick()
        compose.waitUntil(5_000) { manager.aiResponse.isNotEmpty() }
        shot("09_custom_output")
    }

    @Test
    fun emptyModelReplyIsAnError() = runBlocking {
        val manager = FoundationModelManager { _, _ -> "   " }
        manager.refineDescription("some text")
        assertEquals("", manager.aiResponse)
        assertTrue(manager.errorMessage!!.isNotBlank())
    }

    @Test
    fun errorMessagesAreFriendly() {
        assertEquals("The AI is busy right now. Please try again in a moment.", friendlyMessage(GenAiException("x", null, GenAiException.ErrorCode.BUSY)))
        assertEquals("Something went wrong while generating. Please try again.", friendlyMessage(IllegalStateException("boom")))
    }
}
