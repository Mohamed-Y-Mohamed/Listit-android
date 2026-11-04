package com.list_it.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.list_it.app.data.Prefs
import com.list_it.app.data.Supabase

/** The shared app state, injected like the iOS `@Environment(Supabase.self)`. */
val LocalDb = staticCompositionLocalOf<Supabase> { error("Supabase not provided") }
val LocalPrefs = staticCompositionLocalOf<Prefs> { error("Prefs not provided") }
