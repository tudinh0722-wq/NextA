package com.nexta.alarm

import android.content.Context
import org.json.JSONObject


data class AlarmSettings(
    val enabled: Boolean = true,
    val leadTimeMinutes: Int = 15,
    val repeatEnabled: Boolean = true,
    val repeatIntervalMinutes: Int = 5,
    val maxRepeats: Int = 3,
    val title: String,
    val startMillis: Long,
    val note: String
)

class AlarmSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("nexta_alarms", Context.MODE_PRIVATE)

    fun save(eventId: String, settings: AlarmSettings) {
        prefs.edit().putString(eventId, JSONObject().apply {
            put("enabled", settings.enabled)
            put("lead", settings.leadTimeMinutes)
            put("repeat", settings.repeatEnabled)
            put("interval", settings.repeatIntervalMinutes)
            put("max", settings.maxRepeats)
            put("title", settings.title)
            put("start", settings.startMillis)
            put("note", settings.note)
            put("ack", false)
        }.toString()).apply()
    }

    fun get(eventId: String): AlarmSettings? = prefs.getString(eventId, null)?.let { raw ->
        runCatching {
            val json = JSONObject(raw)
            AlarmSettings(
                enabled = json.optBoolean("enabled", true),
                leadTimeMinutes = json.optInt("lead", 15),
                repeatEnabled = json.optBoolean("repeat", true),
                repeatIntervalMinutes = json.optInt("interval", 5),
                maxRepeats = json.optInt("max", 3),
                title = json.optString("title"),
                startMillis = json.optLong("start"),
                note = json.optString("note")
            )
        }.getOrNull()
    }

    fun acknowledge(eventId: String) {
        val json = prefs.getString(eventId, null)?.let { JSONObject(it) } ?: return
        json.put("ack", true)
        prefs.edit().putString(eventId, json.toString()).apply()
    }

    fun isAcknowledged(eventId: String): Boolean = prefs.getString(eventId, null)?.let {
        runCatching { JSONObject(it).optBoolean("ack", false) }.getOrDefault(false)
    } ?: false

    fun allIds(): Set<String> = prefs.all.keys

    fun remove(eventId: String) { prefs.edit().remove(eventId).apply() }
}
