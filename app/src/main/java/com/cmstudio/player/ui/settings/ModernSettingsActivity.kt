package com.cmstudio.player.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.theme.MpvxTheme

class ModernSettingsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CATEGORY = "extra_settings_category"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        val initialCategory = intent?.getStringExtra(EXTRA_CATEGORY)

        setContent {
            val darkMode by AppSettings.darkMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (darkMode.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }
            val themePreset by AppSettings.themePreset.collectAsState()
            val isAmoled by AppSettings.isAmoled.collectAsState()
            val customPrimary by AppSettings.customPrimaryColor.collectAsState()
            val customAccent by AppSettings.customAccentColor.collectAsState()
            val customBg by AppSettings.customBackgroundColor.collectAsState()
            val customSurface by AppSettings.customSurfaceColor.collectAsState()

            MpvxTheme(
                darkTheme = effectiveDark,
                themePreset = themePreset,
                isAmoled = isAmoled,
                customPrimaryArgb = customPrimary,
                customAccentArgb = customAccent,
                customBackgroundArgb = customBg,
                customSurfaceArgb = customSurface
            ) {
                SettingsScreen(
                    onBackClick = { finish() },
                    initialCategory = initialCategory
                )
            }
        }
    }
}
