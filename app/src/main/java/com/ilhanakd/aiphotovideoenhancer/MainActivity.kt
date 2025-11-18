package com.ilhanakd.aiphotovideoenhancer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ilhanakd.aiphotovideoenhancer.data.preferences.PreferenceRepository
import com.ilhanakd.aiphotovideoenhancer.domain.model.AppThemeOption
import com.ilhanakd.aiphotovideoenhancer.domain.model.LocaleOption
import com.ilhanakd.aiphotovideoenhancer.theme.AIEnhancerTheme
import com.ilhanakd.aiphotovideoenhancer.ui.home.HomeScreen
import com.ilhanakd.aiphotovideoenhancer.ui.photo.PhotoEnhanceScreen
import com.ilhanakd.aiphotovideoenhancer.ui.settings.SettingsScreen
import com.ilhanakd.aiphotovideoenhancer.ui.video.VideoEnhanceScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferenceRepository = PreferenceRepository(applicationContext)
        setContent {
            val languageFlow = remember { preferenceRepository.language }
            val themeFlow = remember { preferenceRepository.theme }
            val language by languageFlow.collectAsState(initial = LocaleOption.SYSTEM)
            val theme by themeFlow.collectAsState(initial = AppThemeOption.SYSTEM)

            val localeContext = remember(language) {
                val code = when (language) {
                    LocaleOption.SYSTEM -> null
                    LocaleOption.ENGLISH -> "en"
                    LocaleOption.TURKISH -> "tr"
                }
                LocaleUtils.updateLocale(this, code)
            }

            CompositionLocalProvider(LocalContext provides localeContext) {
                AIEnhancerTheme(themeOption = theme) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onPhoto = { navController.navigate("photo") },
                                onVideo = { navController.navigate("video") },
                                onSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("photo") { PhotoEnhanceScreen { navController.popBackStack() } }
                        composable("video") { VideoEnhanceScreen { navController.popBackStack() } }
                        composable("settings") { SettingsScreen { navController.popBackStack() } }
                    }
                }
            }
        }
    }
}
