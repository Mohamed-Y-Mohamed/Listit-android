package com.list_it.app.data

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.serialization.json.Json
import java.time.Instant

/** Remembers the last reminder time set for each task (iOS `ReminderStore`). */
object ReminderStore {
    private const val KEY = "taskReminderTimes"
    private val times = mutableStateMapOf<String, Long>()
    private var prefs: Prefs? = null

    fun init(prefs: Prefs) {
        this.prefs = prefs
        times.clear()
        prefs.getString(KEY)?.let { raw ->
            runCatching { Json.decodeFromString<Map<String, Long>>(raw) }.getOrNull()?.let(times::putAll)
        }
    }

    fun date(taskID: String): Instant? = times[taskID]?.let(Instant::ofEpochMilli)

    fun set(date: Instant, taskID: String) {
        times[taskID] = date.toEpochMilli()
        save()
    }

    fun remove(taskID: String) {
        times.remove(taskID)
        save()
    }

    private fun save() {
        prefs?.putString(KEY, Json.encodeToString(times.toMap()))
    }
}
