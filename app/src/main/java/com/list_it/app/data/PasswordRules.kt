package com.list_it.app.data

/** The password rule the auth server enforces (shared with the web app). */
object PasswordRules {
    const val RULE = "At least 8 characters, including an uppercase letter, a lowercase letter, a number and a symbol."

    private val SYMBOLS = Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"|<>?,./`~\\\\]")

    /** The first rule [password] breaks, phrased for the user, or null when it passes. */
    fun error(password: String): String? = when {
        password.length < 8 -> "Password must be at least 8 characters"
        !password.any { it.isLowerCase() } -> "Password must include a lowercase letter"
        !password.any { it.isUpperCase() } -> "Password must include an uppercase letter"
        !password.any { it.isDigit() } -> "Password must include a number"
        !SYMBOLS.containsMatchIn(password) -> "Password must include a symbol, such as ! ? @ or -"
        else -> null
    }
}
