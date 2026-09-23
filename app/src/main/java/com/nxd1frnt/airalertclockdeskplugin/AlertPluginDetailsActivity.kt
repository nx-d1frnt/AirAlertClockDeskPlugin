package com.nxd1frnt.airalertclockdeskplugin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

class AlertPluginDetailsActivity : AppCompatActivity() {

    private lateinit var currentRegionTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var statusIcon: ImageView
    private lateinit var threatsContainer: View
    private lateinit var threatsList: android.widget.LinearLayout

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
        statusTextView = findViewById(R.id.status_text)
        statusIcon = findViewById(R.id.status_icon)
        threatsContainer = findViewById(R.id.threats_container)
        threatsList = findViewById(R.id.threats_list)
        val closeButton = findViewById<Button>(R.id.close_button)

        updateUI()

        closeButton.setOnClickListener {
            finishAfterTransition()
        }


        val rootScrim = findViewById<View>(R.id.root_scrim)
        rootScrim.setOnClickListener {
            finishAfterTransition()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val regionName = SirenSharedPreferences.getSelectedRegionName(this)
        currentRegionTextView.text = getString(R.string.region_prefix, regionName)

        val cachedText = SirenSharedPreferences.getChipText(this)
        val cachedIconName = SirenSharedPreferences.getChipIcon(this)

        statusTextView.text = cachedText
        
        val iconResId = resources.getIdentifier(cachedIconName, "drawable", packageName)
        if (iconResId != 0) {
            statusIcon.setImageResource(iconResId)
            if (cachedIconName == "ic_alarm_red" || cachedIconName == "ic_alarm_yellow") {
                statusIcon.imageTintList = null
            } else {
                statusIcon.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_primary))
            }
        }

        val activeLevels = SirenSharedPreferences.getActiveAlertLevels(this)
        if (activeLevels.isNotEmpty()) {
            threatsContainer.visibility = View.VISIBLE
            threatsList.removeAllViews()
            val inflater = layoutInflater

            for (threat in activeLevels) {
                val itemView = inflater.inflate(R.layout.item_alert_level, threatsList, false)
                val badge = itemView.findViewById<ImageView>(R.id.threat_badge)
                val titleView = itemView.findViewById<TextView>(R.id.threat_level_title)
                val timeView = itemView.findViewById<TextView>(R.id.threat_time)
                val reasonView = itemView.findViewById<TextView>(R.id.threat_reason)

                val badgeColor = when {
                    threat.isRed -> getColor(R.color.alert_red)
                    threat.isYellow -> getColor(R.color.alert_yellow)
                    else -> getColor(R.color.md_theme_primary)
                }
                badge.imageTintList = android.content.res.ColorStateList.valueOf(badgeColor)

                titleView.text = threat.getLocalizedTitle(this)
                timeView.text = threat.getFormattedTime(this)
                reasonView.text = threat.reason

                threatsList.addView(itemView)
            }
        } else {
            threatsContainer.visibility = View.GONE
        }
    }
}
