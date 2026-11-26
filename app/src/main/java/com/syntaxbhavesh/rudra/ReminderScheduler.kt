package com.syntaxbhavesh.rudra

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

object ReminderScheduler {
    const val CHANNEL_ID = "rudra_reminders"
    private const val PREFS = "rudra_scheduled_reminders"
    private const val ITEMS = "items"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "RUDRA reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Local reminders scheduled with RUDRA" }
            manager.createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context, title: String, delaySeconds: Long) {
        require(delaySeconds in 10L..604800L) { "Reminder must be between 10 seconds and 7 days." }
        val id = java.util.UUID.randomUUID().toString()
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs.getString(ITEMS, "[]"))
        items.put(JSONObject().put("id", id).put("title", title.take(200)).put("triggerAt", triggerAt))
        prefs.edit().putString(ITEMS, items.toString()).apply()
        scheduleAt(context, id, title, triggerAt)
    }

    private fun scheduleAt(context: Context, id: String, title: String, triggerAt: Long) {
        val requestCode = id.hashCode()
        val pending = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ReminderReceiver::class.java)
                .putExtra(ReminderReceiver.EXTRA_TITLE, title)
                .putExtra(ReminderReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    internal fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs.getString(ITEMS, "[]"))
        val now = System.currentTimeMillis()
        val kept = JSONArray()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val title = item.optString("title")
            val triggerAt = item.optLong("triggerAt", 0L)
            if (id.isBlank() || title.isBlank() || triggerAt <= 0L) continue
            if (triggerAt <= now) {
                notifyReminder(context, title, id.hashCode())
            } else {
                kept.put(item)
                scheduleAt(context, id, title, triggerAt)
            }
        }
        prefs.edit().putString(ITEMS, kept.toString()).apply()
    }

    fun pendingReminders(context: Context): List<String> {
        val items = readItems(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ITEMS, "[]"))
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val triggerAt = item.optLong("triggerAt", 0L)
            if (triggerAt <= 0L) null else "$title — " +
                java.text.SimpleDateFormat("EEE, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(triggerAt))
        }
    }

    fun cancelAll(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs.getString(ITEMS, "[]"))
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var cancelled = 0
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            val pending = PendingIntent.getBroadcast(
                context, id.hashCode(),
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
            cancelled++
        }
        prefs.edit().remove(ITEMS).apply()
        return cancelled
    }

    internal fun complete(context: Context, id: String) {
        if (id.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = readItems(prefs.getString(ITEMS, "[]"))
        val kept = JSONArray()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            if (item.optString("id") != id) kept.put(item)
        }
        prefs.edit().putString(ITEMS, kept.toString()).apply()
    }

    private fun readItems(raw: String?): JSONArray =
        runCatching { JSONArray(raw ?: "[]") }.getOrElse { JSONArray() }

    internal fun notifyReminder(context: Context, title: String, notificationId: Int) {
        createChannel(context)
        val openApp = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("RUDRA reminder")
                .setContentText(title)
                .setStyle(Notification.BigTextStyle().bigText(title))
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("RUDRA reminder")
                .setContentText(title)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()
        }
        context.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.restore(context)
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: "Your reminder"
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        ReminderScheduler.complete(context, id)
        ReminderScheduler.notifyReminder(context, title, id.hashCode())
    }

    companion object {
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_ID = "reminder_id"
    }
}
