package com.list_it.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.list_it.app.model.ListDisplayStyle
import com.list_it.app.model.SurfaceChoice

/** Observable app settings (the iOS app's @AppStorage keys plus the Android appearance options). */
class Prefs(private val store: SharedPreferences) {

    constructor(context: Context) : this(context.getSharedPreferences("list_it", Context.MODE_PRIVATE))

    private var _isSignedIn by mutableStateOf(store.getBoolean(KEY_SIGNED_IN, false))
    var isSignedIn: Boolean
        get() = _isSignedIn
        set(value) { _isSignedIn = value; store.edit { putBoolean(KEY_SIGNED_IN, value) } }

    /** null follows the device; true/false once the user flips Dark mode in Settings. */
    private var _darkMode by mutableStateOf(if (store.contains(KEY_DARK_MODE)) store.getBoolean(KEY_DARK_MODE, false) else null)
    var darkMode: Boolean?
        get() = _darkMode
        set(value) { _darkMode = value; store.edit { if (value == null) remove(KEY_DARK_MODE) else putBoolean(KEY_DARK_MODE, value) } }

    /** Background for each theme. Defaults to Midnight (dark) and Soft grey (light). */
    private var _surfaceDark by mutableStateOf(readSurface(KEY_SURFACE_DARK))
    private var _surfaceLight by mutableStateOf(readSurface(KEY_SURFACE_LIGHT))
    fun surface(isDark: Boolean): SurfaceChoice = if (isDark) _surfaceDark else _surfaceLight
    fun setSurface(isDark: Boolean, choice: SurfaceChoice) {
        if (isDark) _surfaceDark = choice else _surfaceLight = choice
        store.edit { putString(if (isDark) KEY_SURFACE_DARK else KEY_SURFACE_LIGHT, choice.name.lowercase()) }
    }

    private var _displayStyle by mutableStateOf(
        if (store.getString(KEY_DISPLAY_STYLE, null) == "list") ListDisplayStyle.LIST else ListDisplayStyle.CARD
    )
    var displayStyle: ListDisplayStyle
        get() = _displayStyle
        set(value) { _displayStyle = value; store.edit { putString(KEY_DISPLAY_STYLE, value.name.lowercase()) } }

    /** Task reminders are delivered only while this is on (Settings -> Notifications). */
    private var _remindersEnabled by mutableStateOf(store.getBoolean(KEY_REMINDERS, true))
    var remindersEnabled: Boolean
        get() = _remindersEnabled
        set(value) { _remindersEnabled = value; store.edit { putBoolean(KEY_REMINDERS, value) } }

    private var _lastSeenWhatsNewVersion by mutableIntStateOf(store.getInt(KEY_WHATS_NEW, 0))
    var lastSeenWhatsNewVersion: Int
        get() = _lastSeenWhatsNewVersion
        set(value) { _lastSeenWhatsNewVersion = value; store.edit { putInt(KEY_WHATS_NEW, value) } }

    /** Raw access for small maps such as reminder times. */
    fun getString(key: String): String? = store.getString(key, null)
    fun putString(key: String, value: String) = store.edit { putString(key, value) }

    private fun readSurface(key: String): SurfaceChoice =
        SurfaceChoice.entries.firstOrNull { it.name.lowercase() == store.getString(key, null) } ?: SurfaceChoice.SOFT

    private companion object {
        const val KEY_SIGNED_IN = "isSignedIn"
        const val KEY_DARK_MODE = "darkMode"
        const val KEY_SURFACE_DARK = "surface.dark"
        const val KEY_SURFACE_LIGHT = "surface.light"
        const val KEY_DISPLAY_STYLE = "listDisplayStyle"
        const val KEY_REMINDERS = "remindersEnabled"
        const val KEY_WHATS_NEW = "lastSeenWhatsNewVersion"
    }
}
