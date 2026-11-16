package com.list_it.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.list_it.app.data.Prefs
import com.list_it.app.data.Supabase

/** Gives every screen access to the app's data (lists, tasks, notes) without passing it around by hand. */
val LocalDb = staticCompositionLocalOf<Supabase> { error("Supabase not provided") }
val LocalPrefs = staticCompositionLocalOf<Prefs> { error("Prefs not provided") }
