package com.nxd1frnt.airalertclockdeskplugin

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AlertLevelItem(
    val level: String,
    val reason: String,
    val createdAt: String
) {
    val isRed: Boolean
        get() = level.equals("Red", ignoreCase = true)

    val isYellow: Boolean
        get() = level.equals("Yellow", ignoreCase = true)

    fun getFormattedTime(context: Context): String {
        val date = parseIsoDate(createdAt) ?: return createdAt
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val localTimeStr = timeFormat.format(date)

        val now = System.currentTimeMillis()
        val diffMillis = (now - date.time).coerceAtLeast(0L)
        val diffMinutes = (diffMillis / 60_000L).toInt()
        val diffHours = (diffMillis / 3600_000L).toInt()

        val timeAgoStr = when {
            diffMinutes < 1 -> context.getString(R.string.time_just_now)
            diffHours < 1 -> context.getString(R.string.time_ago_min, diffMinutes)
            else -> context.getString(R.string.time_ago_hour, diffHours)
        }

        return "$localTimeStr ($timeAgoStr)"
    }

    fun getLocalizedTitle(context: Context): String {
        return when {
            isRed -> context.getString(R.string.missile_threat)
            isYellow -> context.getString(R.string.drone_threat)
            else -> reason.ifBlank { context.getString(R.string.alert_active) }
        }
    }

    companion object {
        fun parseIsoDate(isoString: String): Date? {
            if (isoString.isBlank()) return null
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val instant = java.time.Instant.parse(isoString)
                    Date(instant.toEpochMilli())
                } else {
                    // Normalize nanoseconds to milliseconds e.g. 2026-09-23T21:54:44.057712Z -> 2026-09-23T21:54:44.057+0000
                    val normalized = if (isoString.contains(".")) {
                        val dotIndex = isoString.indexOf('.')
                        val zIndex = isoString.indexOf('Z')
                        val fraction = isoString.substring(dotIndex + 1, if (zIndex != -1) zIndex else isoString.length)
                        val msFraction = fraction.padEnd(3, '0').take(3)
                        isoString.substring(0, dotIndex) + "." + msFraction + "Z"
                    } else {
                        isoString
                    }
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    format.parse(normalized)
                }
            } catch (e: Exception) {
                try {
                    val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    fallbackFormat.parse(isoString)
                } catch (e2: Exception) {
                    null
                }
            }
        }

        fun toJsonArray(items: List<AlertLevelItem>): String {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("alertLevel", item.level)
                    put("reason", item.reason)
                    put("createdAt", item.createdAt)
                }
                array.put(obj)
            }
            return array.toString()
        }

        fun fromJsonArray(jsonString: String?): List<AlertLevelItem> {
            if (jsonString.isNullOrBlank()) return emptyList()
            val list = mutableListOf<AlertLevelItem>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AlertLevelItem(
                            level = obj.optString("alertLevel", ""),
                            reason = obj.optString("reason", ""),
                            createdAt = obj.optString("createdAt", "")
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
            return list
        }
    }
}
