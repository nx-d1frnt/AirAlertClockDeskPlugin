package com.nxd1frnt.airalertclockdeskplugin

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var currentRegionTextView: TextView
    private lateinit var showWhenNoAlertSwitch: MaterialSwitch
    
    private val statesData = mutableListOf<StateData>()

    data class StateData(
        val id: String,
        val name: String,
        val communities: MutableList<Region> = mutableListOf()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        currentRegionTextView = findViewById(R.id.current_region_text)
        showWhenNoAlertSwitch = findViewById(R.id.show_when_no_alert_switch)
        val selectRegionCard = findViewById<MaterialCardView>(R.id.select_region_card)

        updateUI()
        loadRegionsFromJson()

        selectRegionCard.setOnClickListener {
            showStateSelectionDialog()
        }

        showWhenNoAlertSwitch.setOnCheckedChangeListener { _, isChecked ->
            SirenSharedPreferences.saveShowstate(this, isChecked)
        }
    }

    private fun updateUI() {
        currentRegionTextView.text = SirenSharedPreferences.getSelectedRegionName(this)
        showWhenNoAlertSwitch.isChecked = SirenSharedPreferences.getShowstate(this)
    }

    private fun loadRegionsFromJson() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val jsonString = resources.openRawResource(R.raw.regions)
                    .bufferedReader().use { it.readText() }

                val root = JSONObject(jsonString)
                val statesArray = root.getJSONArray("states")

                val tempStatesData = mutableListOf<StateData>()

                for (i in 0 until statesArray.length()) {
                    val stateJson = statesArray.getJSONObject(i)
                    val stateName = stateJson.getString("regionName")
                    val stateId = stateJson.getString("regionId")
                    val stateChildren = stateJson.optJSONArray("regionChildIds")

                    val stateData = StateData(id = stateId, name = stateName)

                    if (stateChildren != null && stateChildren.length() > 0) {
                        parseDistricts(stateChildren, stateData.communities)
                    }
                    
                    stateData.communities.sortBy { it.name }
                    tempStatesData.add(stateData)
                }
                
                tempStatesData.sortBy { it.name }
                
                withContext(Dispatchers.Main) {
                    statesData.clear()
                    statesData.addAll(tempStatesData)
                }

            } catch (e: Exception) {
                Log.e("SettingsActivity", "Помилка парсингу regions.json", e)
            }
        }
    }

    private fun parseDistricts(districtsArray: JSONArray, communitiesList: MutableList<Region>) {
        for (i in 0 until districtsArray.length()) {
            val district = districtsArray.getJSONObject(i)
            val communitiesArray = district.optJSONArray("regionChildIds")
            if (communitiesArray != null) {
                parseCommunities(communitiesArray, communitiesList)
            }
        }
    }

    private fun parseCommunities(communitiesArray: JSONArray, communitiesList: MutableList<Region>) {
        for (i in 0 until communitiesArray.length()) {
            val community = communitiesArray.getJSONObject(i)
            val communityId = community.getString("regionId")
            val communityName = community.getString("regionName")
            communitiesList.add(Region(id = communityId, name = communityName))
        }
    }

    private fun showStateSelectionDialog() {
        if (statesData.isEmpty()) return

        val stateNames = statesData.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Оберіть область")
            .setItems(stateNames) { _, which ->
                val selectedState = statesData[which]
                if (selectedState.communities.isEmpty()) {
                    // Наприклад, м. Київ - відразу зберігаємо
                    saveSelection(selectedState.id, selectedState.name)
                } else {
                    // Показуємо список громад
                    showCommunitySelectionDialog(selectedState)
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun showCommunitySelectionDialog(state: StateData) {
        val options = mutableListOf<Region>()
        // Додаємо можливість обрати всю область
        options.add(Region(state.id, "Вся область"))
        options.addAll(state.communities)

        val optionNames = options.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(state.name)
            .setItems(optionNames) { _, which ->
                val selected = options[which]
                val fullName = if (which == 0) state.name else "${state.name}: ${selected.name}"
                saveSelection(selected.id, fullName)
            }
            .setNegativeButton("Назад") { _, _ ->
                showStateSelectionDialog()
            }
            .show()
    }

    private fun saveSelection(id: String, name: String) {
        SirenSharedPreferences.saveSelectedRegion(this, id, name)
        updateUI()
        SirenSharedPreferences.saveLastNetworkRequestTime(applicationContext, 0L)
    }
}
