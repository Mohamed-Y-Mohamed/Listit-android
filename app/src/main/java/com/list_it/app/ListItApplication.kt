package com.list_it.app

import android.app.Application
import com.list_it.app.data.Prefs
import com.list_it.app.data.ReminderStore
import com.list_it.app.data.Reminders
import com.list_it.app.data.Supabase
import com.list_it.app.data.createListItClient

class ListItApplication : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var db: Supabase
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        ReminderStore.init(prefs)
        Reminders.init(this, prefs)
        db = Supabase(prefs, createListItClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY))
    }
}
