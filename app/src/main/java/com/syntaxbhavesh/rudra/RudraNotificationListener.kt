package com.syntaxbhavesh.rudra

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

/** Opt-in notification reader. Android binds this service only after user approval. */
class RudraNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.take(120).orEmpty()
        val text = (extras.getCharSequence("android.bigText")
            ?: extras.getCharSequence("android.text"))?.toString()?.take(400).orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val prefs = getSharedPreferences("rudra_notifications", MODE_PRIVATE)
        val items = runCatching { JSONArray(prefs.getString("items", "[]") ?: "[]") }
            .getOrElse { JSONArray() }
        items.put(JSONObject()
            .put("app", sbn.packageName.substringAfterLast('.').take(40))
            .put("title", title)
            .put("text", text)
            .put("time", sbn.postTime))
        while (items.length() > 40) items.remove(0)
        prefs.edit().putString("items", items.toString()).apply()
    }
}
