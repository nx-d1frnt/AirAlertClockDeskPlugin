package com.nxd1frnt.airalertclockdeskplugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL

class AlertChipReceiver : BroadcastReceiver() {

    var isAlarmActive: Boolean = false
    companion object {
        const val ACTION_REQUEST_DATA = "com.nxd1frnt.clockdesk2.ACTION_REQUEST_CHIP_DATA"
        const val ACTION_UPDATE_DATA = "com.nxd1frnt.clockdesk2.ACTION_UPDATE_CHIP_DATA"
        const val CLOCKDESK_PACKAGE = "com.nxd1frnt.clockdesk2"
        const val NETWORK_REQUEST_INTERVAL_MS: Long = 60 * 1000 // 60 секунд
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == ACTION_REQUEST_DATA) {
            val pendingResult = goAsync()

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val selectedRegionId = SirenSharedPreferences.getSelectedRegionId(context)
                    if (selectedRegionId == null) {
                        sendPushUpdate(context, "Оберіть регіон", "ic_question_circle", true)
                        return@launch
                    }
                    val lastRequestTime = SirenSharedPreferences.getLastNetworkRequestTime(context)
                    val now = System.currentTimeMillis()
                    val needsRefresh = (now - lastRequestTime) > NETWORK_REQUEST_INTERVAL_MS

                    if (needsRefresh) {
                        Log.i("ExampleChipReceiver", "Пройшло 60+ сек. Виконую мережевий запит...")

                        SirenSharedPreferences.saveLastNetworkRequestTime(context, now)

                        val (chipText, chipIcon) = performCheckLogic(context, selectedRegionId)

                        SirenSharedPreferences.saveSirenState(context, chipText, chipIcon)

                        if (!isAlarmActive) {
                            sendPushUpdate(context, chipText, chipIcon, SirenSharedPreferences.getShowstate(context))
                        } else
                        {
                            sendPushUpdate(context, chipText, chipIcon, true)
                        }

                    } else {
                        // Log.d("ExampleChipReceiver", "Відповідь із кешу (ще не пройшло 60 сек)")
                        val cachedText = SirenSharedPreferences.getChipText(context)
                        val cachedIcon = SirenSharedPreferences.getChipIcon(context)
                        if (!isAlarmActive) {
                            sendPushUpdate(context, cachedText, cachedIcon, SirenSharedPreferences.getShowstate(context))
                        } else
                        {
                            sendPushUpdate(context, cachedText, cachedIcon, true)
                        }
                    }

                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun performCheckLogic(context: Context, selectedRegionId: String): Pair<String, String> {
        val selectedRegionName = SirenSharedPreferences.getSelectedRegionName(context)
        return try {
            val url = "https://siren.pp.ua/api/v3/alerts/$selectedRegionId"
            val responseString = URL(url).readText()

            val jsonArray = JSONArray(responseString)
            val regionData = jsonArray.getJSONObject(0)
            val alertsArray = regionData.getJSONArray("activeAlerts")

            isAlarmActive = alertsArray.length() > 0

            if (isAlarmActive) {
                Pair("Тривога!", "ic_alarm_on")
            } else {
                Pair("Все спокійно", "ic_alarm_off")
            }

        } catch (e: Exception) {
            Log.e("ExampleChipReceiver", "Помилка мережевого запиту", e)
            Pair("Помилка мережі", "ic_error_circle")
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
        }
        context.sendBroadcast(responseIntent)
    }
}