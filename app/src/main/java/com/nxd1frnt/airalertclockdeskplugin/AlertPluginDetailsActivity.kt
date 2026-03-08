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
        currentRegionTextView.text = "Регіон: $regionName"

        val cachedText = SirenSharedPreferences.getChipText(this)
        val cachedIconName = SirenSharedPreferences.getChipIcon(this)

        statusTextView.text = cachedText
        
        val iconResId = resources.getIdentifier(cachedIconName, "drawable", packageName)
        if (iconResId != 0) {
            statusIcon.setImageResource(iconResId)
        }
    }
}
