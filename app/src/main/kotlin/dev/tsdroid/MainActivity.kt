package dev.tsdroid

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.tsdroid.data.SettingsStore
import dev.tsdroid.ui.theme.TsDroidTheme
import dev.tsdroid.ui.screen.AppNavigation
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val settingsStore by lazy { SettingsStore(applicationContext) }
    private var lastHandledLanguage: String? = null

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val appLang = prefs.getString("app_language", "system") ?: "system"
        val locale = getLocaleForLanguage(appLang)
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appLanguage by settingsStore.appLanguage.collectAsState(initial = settingsStore.appLanguageSync)

            LaunchedEffect(appLanguage) {
                if (lastHandledLanguage == null) {
                    lastHandledLanguage = appLanguage
                    return@LaunchedEffect
                }
                if (lastHandledLanguage != appLanguage) {
                    lastHandledLanguage = appLanguage
                    val currentLang = resources.configuration.locales.takeIf { !it.isEmpty }?.get(0)?.language ?: ""
                    val targetLang = getLocaleForLanguage(appLanguage).language
                    if (!currentLang.equals(targetLang, ignoreCase = true)) {
                        recreate()
                    }
                }
            }

            TsDroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    companion object {
        fun getLocaleForLanguage(lang: String): Locale {
            return when (lang) {
                "ru" -> Locale("ru")
                "en" -> Locale("en")
                else -> {
                    val sys = Locale.getDefault()
                    if (sys.language.equals("ru", ignoreCase = true) ||
                        sys.language.equals("uk", ignoreCase = true) ||
                        sys.language.equals("be", ignoreCase = true)
                    ) {
                        Locale("ru")
                    } else {
                        Locale("en")
                    }
                }
            }
        }
    }
}
