package com.list_it.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.list_it.app.MainActivity
import com.list_it.app.R
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Local task reminders (iOS `UNUserNotificationCenter`). Each task has at most one pending
 * reminder, identified by the task id, exactly like the iOS notification requests.
 */
@SuppressLint("StaticFieldLeak") // Holds the application context only, never an Activity.
object Reminders {
    private const val CHANNEL_ID = "task_reminders"
    private const val KEY_PENDING = "pendingReminders"
    private const val EXTRA_TASK_ID = "taskID"
    private const val EXTRA_TITLE = "title"

    private lateinit var context: Context
    private lateinit var prefs: Prefs

    fun init(context: Context, prefs: Prefs) {
        this.context = context.applicationContext
        this.prefs = prefs
        // Channels only exist from API 26; the Compat builder is a no-op below that.
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.reminder_channel_name))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** Schedules (or replaces) the reminder for a task. Returns an error message on failure. */
    fun schedule(taskID: String, title: String, at: Instant): String? = runCatching {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val trigger = at.toEpochMilli()
        val intent = pendingIntent(taskID, title)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        }
        savePending(pending() + (taskID to Pending(title, trigger)))
        ReminderStore.set(at, taskID)
        null
    }.getOrElse { it.message ?: "Unable to schedule reminder" }

    /** Removes a pending reminder (iOS `removePendingNotificationRequests`). */
    fun cancel(taskID: String) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(taskID, ""))
        savePending(pending() - taskID)
    }

    /** The date of the task's pending reminder, if one is still waiting to fire. */
    fun pendingDate(taskID: String): Instant? =
        pending()[taskID]?.at?.takeIf { it > System.currentTimeMillis() }?.let(Instant::ofEpochMilli)

    internal fun rescheduleAll() {
        pending().forEach { (id, p) ->
            if (p.at > System.currentTimeMillis()) schedule(id, p.title, Instant.ofEpochMilli(p.at))
        }
    }

    @SuppressLint("MissingPermission") // Checked just below.
    internal fun show(context: Context, taskID: String, title: String) {
        savePending(pending() - taskID)
        if (!prefs.remindersEnabled) return // Settings -> Notifications is off: keep the reminder, skip delivery.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val open = PendingIntent.getActivity(
            context, taskID.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(EXTRA_TASK_ID, taskID),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.reminder_body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(taskID.hashCode(), notification)
    }

    private fun pendingIntent(taskID: String, title: String): PendingIntent = PendingIntent.getBroadcast(
        context, taskID.hashCode(),
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_TASK_ID, taskID).putExtra(EXTRA_TITLE, title),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @kotlinx.serialization.Serializable
    private data class Pending(val title: String, val at: Long)

    private fun pending(): Map<String, Pending> =
        prefs.getString(KEY_PENDING)?.let { runCatching { Json.decodeFromString<Map<String, Pending>>(it) }.getOrNull() } ?: emptyMap()

    private fun savePending(map: Map<String, Pending>) = prefs.putString(KEY_PENDING, Json.encodeToString(map))

    internal fun taskId(intent: Intent) = intent.getStringExtra(EXTRA_TASK_ID)
    internal fun title(intent: Intent) = intent.getStringExtra(EXTRA_TITLE)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = Reminders.taskId(intent) ?: return
        Reminders.show(context, id, Reminders.title(intent).orEmpty())
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Reminders.rescheduleAll()
    }
}
