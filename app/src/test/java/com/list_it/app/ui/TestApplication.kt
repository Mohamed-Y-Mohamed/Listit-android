package com.list_it.app.ui

import android.app.Application
import com.list_it.app.data.Prefs
import com.list_it.app.data.ReminderStore
import com.list_it.app.data.Reminders

/** Robolectric app: sets up reminders but leaves the Supabase client to each test. */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = Prefs(this)
        ReminderStore.init(prefs)
        Reminders.init(this, prefs)
    }
}
