package com.list_it.app.model

enum class AlertMode { ERROR, DELETE }

enum class QuickAction { TODAY, TOMORROW, SCHEDULED, PRIORITY, COMPLETED, NOT_COMPLETED, OVERDUE }

enum class SortOption(val title: String) {
    NEWEST("Newest"), OLDEST("Oldest"), ALPHABETICAL_AZ("A-Z"), ALPHABETICAL_ZA("Z-A")
}

enum class ListDisplayStyle { CARD, LIST }

sealed interface VerificationStatus {
    data object Pending : VerificationStatus
    data object Verified : VerificationStatus
    data class Error(val message: String) : VerificationStatus
}

enum class AIFeatureMode(val title: String) {
    // Task AI features
    BREAKDOWN("Quick Breakdown"),
    PLANNER("Deep Planner"),
    REWRITE_LIST("Format as List"),
    REFINE("Refine & Polish"),

    // Note AI features
    NOTE_OUTLINE("Generate Outline"),
    NOTE_TEMPLATE("Smart Template"),
    NOTE_BRAINSTORM("Brainstorm"),
    SUMMARISE("Summarise"),
    CUSTOM("Custom Prompt"),
}

/** Background choice per theme, from the web app's surface ramps (Settings -> Appearance). */
enum class SurfaceChoice(val darkLabel: String, val lightLabel: String) {
    MONO("Black", "White"),
    SOFT("Midnight", "Soft grey"),
    COOL("Navy", "Ice blue"),
    WARM("Midnight Velvet", "Warm paper"),
    ;

    fun label(isDark: Boolean) = if (isDark) darkLabel else lightLabel
}
