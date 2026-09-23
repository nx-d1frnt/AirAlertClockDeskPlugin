package com.nxd1frnt.airalertclockdeskplugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL

class AlertChipReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var isAlarmActive: Boolean = false
    companion object {
        const val ACTION_REQUEST_DATA = "com.nxd1frnt.clockdesk2.ACTION_REQUEST_CHIP_DATA"
        const val ACTION_UPDATE_DATA = "com.nxd1frnt.clockdesk2.ACTION_UPDATE_CHIP_DATA"
        const val CLOCKDESK_PACKAGE = "com.nxd1frnt.clockdesk2"
        const val UPDATE_INTERVAL_SEC = 60  // update interval requested from ClockDesk
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == ACTION_REQUEST_DATA) {
            val pendingResult = goAsync()

            receiverScope.launch {
                try {
                    val selectedRegionId = SirenSharedPreferences.getSelectedRegionId(context)
                    if (selectedRegionId == null) {
                        sendPushUpdate(context, context.getString(R.string.select_region_title), "ic_question_circle", true)
                        return@launch
                    }

                    val (chipText, chipIcon, isAlarmActive) = performCheckLogic(context, selectedRegionId)

                    // Save data
                    SirenSharedPreferences.saveSirenState(context, chipText, chipIcon)

                    val userWantsVisibleAlways = SirenSharedPreferences.getShowstate(context)
                    val visibility = isAlarmActive || userWantsVisibleAlways

                    sendPushUpdate(context, chipText, chipIcon, visibility)

                } catch (e: Exception) {
                    Log.e("AlertChipReceiver", "Update error", e)
                    sendPushUpdate(context, context.getString(R.string.network_error), "ic_error_circle", true)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun performCheckLogic(context: Context, selectedRegionId: String): Triple<String, String, Boolean> {
        return try {
            val url = "https://siren.pp.ua/api/v3/alerts/$selectedRegionId"
            val connection = (URL(url).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            val responseString = connection.inputStream.bufferedReader().use { it.readText() }

            val jsonArray = JSONArray(responseString)
            val regionData = jsonArray.getJSONObject(0)
            val alertsArray = regionData.optJSONArray("activeAlerts") ?: JSONArray()

            val activeLevels = mutableListOf<AlertLevelItem>()
            for (i in 0 until alertsArray.length()) {
                val alertObj = alertsArray.getJSONObject(i)
                val levelsArray = alertObj.optJSONArray("activeAlertLevels")
                if (levelsArray != null) {
                    for (j in 0 until levelsArray.length()) {
                        val levelObj = levelsArray.getJSONObject(j)
                        activeLevels.add(
                            AlertLevelItem(
                                level = levelObj.optString("alertLevel", ""),
                                reason = levelObj.optString("reason", ""),
                                createdAt = levelObj.optString("createdAt", "")
                            )
                        )
                    }
                }
            }

            // Cache active levels for Details activity
            SirenSharedPreferences.saveActiveAlertLevels(context, activeLevels)
            SirenSharedPreferences.saveLastNetworkRequestTime(context, System.currentTimeMillis())

            val hasRed = activeLevels.any { it.isRed }
            val hasYellow = activeLevels.any { it.isYellow }
            val hasAlerts = alertsArray.length() > 0
            val onlyRedAlerts = SirenSharedPreferences.getOnlyRedAlerts(context)

            when {
                hasRed -> {
                    Triple(context.getString(R.string.missile_threat), "ic_alarm_red", true)
                }
                hasYellow -> {
                    if (onlyRedAlerts) {
                        Triple(context.getString(R.string.all_clear), "ic_alarm_off", false)
                    } else {
                        Triple(context.getString(R.string.drone_threat), "ic_alarm_yellow", true)
                    }
                }
                hasAlerts -> {
                    if (onlyRedAlerts) {
                        Triple(context.getString(R.string.all_clear), "ic_alarm_off", false)
                    } else {
                        Triple(context.getString(R.string.alert_active), "ic_alarm_on", true)
                    }
                }
                else -> {
                    Triple(context.getString(R.string.all_clear), "ic_alarm_off", false)
                }
            }
        } catch (e: Exception) {
            Log.e("AlertChipReceiver", "Network request error", e)
            throw e
        }
    }

    private fun sendPushUpdate(context: Context, text: String, icon: String, visibility: Boolean) {
        val responseIntent = Intent(ACTION_UPDATE_DATA).apply {
            setPackage(CLOCKDESK_PACKAGE)
            putExtra("chip_visible", visibility)
            putExtra("plugin_package_name", context.packageName)
            putExtra("chip_text", text)
            putExtra("chip_icon_name", icon)
            putExtra("chip_click_activity", ".AlertPluginDetailsActivity")

            // Ask ClockDesk to ping plugin after UPDATE_INTERVAL_SEC seconds
            putExtra("update_interval_seconds", UPDATE_INTERVAL_SEC)
        }
        context.sendBroadcast(responseIntent)
    }
}