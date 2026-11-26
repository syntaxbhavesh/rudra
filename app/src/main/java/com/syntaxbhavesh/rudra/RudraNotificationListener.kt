package com.syntaxbhavesh.rudra

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

/** Explicitly opt-in notification capture. Content remains in app-private local preferences. */
class RudraNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.take(120).orEmpty()
        val body = (extras.getCharSequence("android.bigText") ?: extras.getCharSequence("android.text"))?.toString()?.take(400).orEmpty()
        if (title.isBlank() && body.isBlank()) return
        val prefs = getSharedPreferences("rudra_notifications", MODE_PRIVATE)
        val list = runCatching { JSONArray(prefs.getString("items", "[]") ?: "[]") }.getOrElse { JSONArray() }
        list.put(JSONObject().put("app", sbn.packageName.substringAfterLast('.').take(40)).put("title", title).put("text", body).put("time", sbn.postTime))
        while (list.length() > 40) list.remove(0)
        prefs.edit().putString("items", list.toString()).apply()
    }
}
