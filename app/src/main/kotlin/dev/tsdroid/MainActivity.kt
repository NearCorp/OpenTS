package dev.tsdroid

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.tsdroid.data.SettingsStore
import dev.tsdroid.ui.theme.TsDroidTheme
import dev.tsdroid.ui.screen.AppNavigation
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val settingsStore by lazy { SettingsStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appLanguage by settingsStore.appLanguage.collectAsState(initial = "system")

            val locale = remember(appLanguage) {
                when (appLanguage) {
                    "ru" -> Locale("ru")
                    "en" -> Locale("en")
                    else -> {
                        val sys = Locale.getDefault()
                        if (sys.language.equals("ru", ignoreCase = true)) Locale("ru") else Locale("en")
                    }
                }
            }

            val configuration = remember(locale) {
                Configuration(resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }

            val localizedContext = remember(locale) {
                createConfigurationContext(configuration)
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides localizedContext,
            ) {
                TsDroidTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
