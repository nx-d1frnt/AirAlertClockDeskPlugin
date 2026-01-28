package com.nxd1frnt.airalertclockdeskplugin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

// Модель данных остается той же
data class Region(val id: String, val name: String)

class AlertPluginDetailsActivity : AppCompatActivity() {

    private lateinit var currentRegionTextView: TextView
    private lateinit var showstatecheck: CheckBox
    private val regionsList = mutableListOf<Region>()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)

        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        val surfaceColor = getColor(R.color.md_theme_surface)

        window.sharedElementEnterTransition = MaterialContainerTransform().apply {
            addTarget(R.id.dialog_card)
            duration = 400L
            scrimColor = android.graphics.Color.TRANSPARENT
            setAllContainerColors(surfaceColor)
            containerColor = surfaceColor
            startContainerColor = surfaceColor
            endContainerColor = surfaceColor
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
        }

        window.sharedElementReturnTransition = MaterialContainerTransform().apply {
            addTarget(R.id.dialog_card)
            duration = 300L
            scrimColor = android.graphics.Color.TRANSPARENT
            setAllContainerColors(surfaceColor)
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
        }

        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        setContentView(R.layout.activity_plugin_details)

        currentRegionTextView = findViewById(R.id.current_region_text)
        showstatecheck = findViewById(R.id.show_when_no_alert_check)
        val selectButton = findViewById<Button>(R.id.select_region_button)
        val closeButton = findViewById<Button>(R.id.close_button)

        updateCurrentRegionText()

        loadRegionsFromJson()

        closeButton.setOnClickListener {
            finishAfterTransition()
        }
        selectButton.setOnClickListener {
            showRegionSelectionDialog()
        }
        showstatecheck.setOnCheckedChangeListener { _, isChecked ->
            SirenSharedPreferences.saveShowstate(this, isChecked)
        }
        val rootScrim = findViewById<android.view.View>(R.id.root_scrim)
        rootScrim.setOnClickListener {
            finishAfterTransition()
        }
    }

    private fun updateCurrentRegionText() {
        currentRegionTextView.text = SirenSharedPreferences.getSelectedRegionName(this)
        showstatecheck.isChecked = SirenSharedPreferences.getShowstate(this)
    }

    // --- НОВАЯ ЛОГИКА ПАРСИНГА ---

    private fun loadRegionsFromJson() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Читаем файл из res/raw/regions.json
                val jsonString = resources.openRawResource(R.raw.regions)
                    .bufferedReader().use { it.readText() }

                val root = JSONObject(jsonString)
                val statesArray = root.getJSONArray("states")

                regionsList.clear() // Очищаем список

                // Парсим только "States" (области)
                for (i in 0 until statesArray.length()) {
                    val state = statesArray.getJSONObject(i)
                    val stateName = state.getString("regionName")
                    val stateId = state.getString("regionId")
                    val stateChildren =
                        state.optJSONArray("regionChildIds") // Используем optJSONArray

                    if (stateChildren == null || stateChildren.length() == 0) {
                        // Это "State" без детей, (напр. "м. Київ")
                        // Добавляем его как есть
                        regionsList.add(Region(id = stateId, name = stateName))
                    } else {
                        // Это "State" с районами, парсим районы
                        parseDistricts(stateChildren, stateName)
                    }
                }

                // Сортируем для удобства
                regionsList.sortBy { it.name }

            } catch (e: Exception) {
                Log.e("RegionParser", "Помилка парсингу regions.json", e)
                // TODO: Показать ошибку пользователю
            }
        }
    }

    private fun parseDistricts(districtsArray: JSONArray, stateName: String) {
        try {
            for (i in 0 until districtsArray.length()) {
                val district = districtsArray.getJSONObject(i)
                val communitiesArray = district.optJSONArray("regionChildIds")

                if (communitiesArray != null) {
                    // Проваливаемся в громады
                    parseCommunities(communitiesArray, stateName)
                }
            }
        } catch (e: Exception) {
            Log.e("RegionParser", "Ошибка парсинга районов", e)
        }
    }

    /**
     * Парсит громады (Communities) и добавляет их в список
     */
    private fun parseCommunities(communitiesArray: JSONArray, stateName: String) {
        try {
            for (i in 0 until communitiesArray.length()) {
                val community = communitiesArray.getJSONObject(i)
                val communityId = community.getString("regionId")
                val communityName = community.getString("regionName")

                // Добавляем громаду с префиксом области для уникальности
                // Пример: "Полтавська область: м. Кременчук та Кременчуцька..."
                regionsList.add(
                    Region(
                        id = communityId,
                        name = "$stateName: $communityName"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("RegionParser", "Ошибка парсинга громад", e)
        }
    }

    // --- ОСТАЛЬНОЙ КОД ОСТАЕТСЯ БЕЗ ИЗМЕНЕНИЙ ---

    private fun showRegionSelectionDialog() {
        if (regionsList.isEmpty()) {
            // Данные еще не загружены
            return
        }

        val regionNames = regionsList.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Выберите регион или громаду")
            .setItems(regionNames) { dialog, which ->

                val selectedRegion = regionsList[which]

                // 1. Сохраняем выбор
                SirenSharedPreferences.saveSelectedRegion(
                    this,
                    selectedRegion.id,
                    selectedRegion.name
                )

                // 2. Обновляем UI
                updateCurrentRegionText()

                // 3. (ВАЖНО!) Сбрасываем таймер
                // Это гарантирует, что следующий 5-секундный запрос
                // немедленно выполнит сетевую проверку.
                SirenSharedPreferences.saveLastNetworkRequestTime(applicationContext, 0L)

                // (Мы больше НЕ запускаем performCheck() вручную,
                // это произойдет автоматически через 0-5 секунд)

                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}