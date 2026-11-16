package com.list_it.app.data

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.serialization.json.Json
import java.time.Instant

/** Remembers when each task's last reminder was for, so the task page can show "Reminded at…". */
object ReminderStore {
    private const val KEY = "taskReminderTimes"
    private val times = mutableStateMapOf<String, Long>()
    private var prefs: Prefs? = null

    /** Loads the saved reminder times when the app starts. */
    fun init(prefs: Prefs) {
        this.prefs = prefs
        times.clear()
        prefs.getString(KEY)?.let { raw ->
            runCatching { Json.decodeFromString<Map<String, Long>>(raw) }.getOrNull()?.let(times::putAll)
        }
    }

    /** When this task's last reminder was set for, if it had one. */
    fun date(taskID: String): Instant? = times[taskID]?.let(Instant::ofEpochMilli)

    /** Remembers a task's reminder time. */
    fun set(date: Instant, taskID: String) {
        times[taskID] = date.toEpochMilli()
        save()
    }

    /** Forgets a task's reminder time. */
    fun remove(taskID: String) {
        times.remove(taskID)
        save()
    }

    /** Writes all reminder times to the phone's storage. */
    private fun save() {
        prefs?.putString(KEY, Json.encodeToString(times.toMap()))
    }
}
