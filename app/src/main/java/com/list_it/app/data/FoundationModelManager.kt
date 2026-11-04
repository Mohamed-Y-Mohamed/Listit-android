package com.list_it.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.list_it.app.model.AIFeatureMode
import kotlinx.coroutines.CancellationException

/** On-device AI prompts and state, identical to the iOS `FoundationModelManager`. */
class FoundationModelManager {
    var aiResponse by mutableStateOf("")
    var isGenerating by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // MARK: - General Content Generation
    suspend fun generateSmartContent(userInput: String, mode: AIFeatureMode) {
        if (userInput.isBlank()) return
        setGeneratingState(true)
        val prompt = when (mode) {
            AIFeatureMode.BREAKDOWN -> """
                You are a smart productivity assistant. Break down the following task or request into a logical, sequential, step-by-step list of actionable items.
                CRITICAL RULES:
                1. Format each step starting strictly with "- " followed by the task (e.g., "- Example subtask").
                2. Do NOT add any introduction text, conclusion text, or conversational filler.
                3. Keep each subtask concise, clear, and actionable.
                4. Provide between 3 to 7 logical steps.
                User Request: $userInput
                Breakdown:
            """.trimIndent()
            AIFeatureMode.PLANNER -> """
                You are an expert master planner and specialized coach. Create an exhaustive, comprehensive, deep planner layout answering the user's request.
                CRITICAL RULES:
                1. Format using clean structure with explicit clear section headers (e.g., "### PHASE 1", "### EXERCISE ROUTINE").
                2. Use clear sub-bullets starting with "- " for specific execution protocols, sets, reps, or granular time indicators.
                3. Provide full context: do not just summarize high-level goals. Build the actual schedule, exact routines, or full action guidelines required.
                4. Do NOT include conversational filler, intro pleasantries, or outro sentences. Start directly with the structured plan.
                User Request: $userInput
                Deep Plan:
            """.trimIndent()
            else -> userInput
        }
        executeSession(prompt)
    }

    // MARK: - Description Transformation Functions
    suspend fun rewriteDescriptionAsList(description: String) = run(description) {
        "Rewrite the following text so that it is entirely formatted in a clean bulleted list layout. Do not include conversational filler.\nOriginal Text:\n$it\nFormatted List:"
    }

    suspend fun refineDescription(description: String) = run(description) {
        "Polish and refine the grammar, tone, and clarity of the following text while strictly preserving its original format and structure. Do not add intro/outro filler.\nOriginal Text:\n$it\nPolished Text:"
    }

    suspend fun processCustomPrompt(description: String, customPrompt: String) = run(description) {
        "$customPrompt\nTarget Text / Context:\n$it"
    }

    suspend fun summarizeDescription(description: String) = run(description) {
        "Summarize the following text concisely, highlighting the core takeaways and key points in a clean, structured overview or bulleted list. Do not include conversational filler or pleasantries.\nOriginal Text:\n$it\nSummary:"
    }

    // MARK: - Note AI features
    suspend fun generateNoteOutline(topic: String) = run(topic) {
        "You are an expert note-taking assistant. Generate a structured, multi-header outline with clear markdown headers and bullet points for the following topic. Do not include conversational filler or pleasantries. Start directly with the outline.\nTopic: $it\nOutline:"
    }

    suspend fun generateSmartTemplate(request: String) = run(request) {
        "You are a smart productivity assistant. Create a comprehensive, well-structured boilerplate template layout with sections, checkboxes, and placeholders based on the following request. Do not include conversational filler. Start directly with the template.\nRequest: $it\nTemplate:"
    }

    suspend fun brainstormAndIdeate(spark: String) = run(spark) {
        "You are a creative brainstorming partner. Generate a rich, categorized list of diverse ideas, perspectives, and key considerations based on the following creative spark. Do not include conversational filler. Start directly with the ideas.\nCreative Spark: $it\nBrainstorm Ideas:"
    }

    // MARK: - Helper Methods
    private suspend fun run(input: String, prompt: (String) -> String) {
        val target = input.trim()
        if (target.isEmpty()) return
        setGeneratingState(true)
        executeSession(prompt(target))
    }

    private fun setGeneratingState(generating: Boolean) {
        isGenerating = generating
        if (generating) errorMessage = null
    }

    private suspend fun executeSession(prompt: String) {
        try {
            aiResponse = OnDeviceModel.respond(prompt).trim()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = "Failed to process locally: ${e.message}"
        } finally {
            isGenerating = false
        }
    }
}

/** Removes markdown symbols before copying to the clipboard (iOS `strippingMarkdown`). */
fun String.strippingMarkdown(): String = this
    .replace(Regex("(?m)^[ \\t]*#{1,6}[ \\t]*"), "")
    .replace(Regex("\\*{1,2}|_{1,2}"), "")
    .replace("`", "")
