package com.nxd1frnt.airalertclockdeskplugin

import android.R
import android.content.Context

object SirenSharedPreferences {

    private const val PREFS_NAME = "SirenCache"

    private const val KEY_CHIP_TEXT = "chip_text"
    private const val KEY_CHIP_ICON = "chip_icon"
    private const val KEY_SHOW_WHEN_NO_ALERT = "no_alert_show"
    private const val KEY_SELECTED_REGION_ID = "selected_region_id"
    private const val KEY_SELECTED_REGION_NAME = "selected_region_name"

    // --- НОВЫЙ КЛЮЧ ---
    private const val KEY_LAST_NETWORK_REQUEST_TIME = "last_network_request_time"


    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ... (getChipText, getChipIcon, saveSelectedRegion, getSelectedRegionId, getSelectedRegionName... остаются без изменений) ...

    fun saveSirenState(context: Context, text: String, icon: String) {
        getPrefs(context).edit()
            .putString(KEY_CHIP_TEXT, text)
            .putString(KEY_CHIP_ICON, icon)
            .apply()
    }

    // --- НОВЫЕ МЕТОДЫ ДЛЯ ВРЕМЕНИ ---

    fun saveLastNetworkRequestTime(context: Context, timeMillis: Long) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_NETWORK_REQUEST_TIME, timeMillis)
            .apply()
    }

    fun getLastNetworkRequestTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_NETWORK_REQUEST_TIME, 0L)
    }

    // --- Методы для НАСТРОЕК ПОЛЬЗОВАТЕЛЯ ---
    // (Остаются без изменений)
    fun saveSelectedRegion(context: Context, regionId: String, regionName: String) {
        getPrefs(context).edit()
            .putString(KEY_SELECTED_REGION_ID, regionId)
            .putString(KEY_SELECTED_REGION_NAME, regionName)
            .apply()
    }

    fun saveShowstate(context: Context, showWhenNoAlert: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_SHOW_WHEN_NO_ALERT, showWhenNoAlert)
            .apply()
    }

    fun getShowstate(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_WHEN_NO_ALERT, true)
    }
    fun getSelectedRegionId(context: Context): String? {
        return getPrefs(context).getString(KEY_SELECTED_REGION_ID, null)
    }

    fun getSelectedRegionName(context: Context): String {
        return getPrefs(context).getString(KEY_SELECTED_REGION_NAME, "Не выбрано") ?: "Не выбрано"
    }

    fun getChipText(context: Context): String {
        return getPrefs(context).getString(KEY_CHIP_TEXT, "Оберіть регіон") ?: "Оберіть регіон"
    }

    fun getChipIcon(context: Context): String {
        return getPrefs(context).getString(KEY_CHIP_ICON, "ic_android_black") ?: "ic_android_black"
    }
}