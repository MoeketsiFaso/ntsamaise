package com.moeketsifaso.ntsamaise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences("language", MODE_PRIVATE)
        setContent {
            var languageCode by rememberSaveable { mutableStateOf(preferences.getString("selected", null)) }
            val language = AppLanguage.fromCode(languageCode)

            var currentScreen by rememberSaveable {
                mutableStateOf(if (language == null) "language" else "home")
            }

            CompositionLocalProvider(LocalAppLanguage provides (language ?: AppLanguage.ENGLISH)) {
                key(languageCode) {
                when (currentScreen) {
                    "language" -> {
                        BackHandler(enabled = language != null) { currentScreen = "home" }
                        LanguageScreen(onSelect = {
                            preferences.edit().putString("selected", it.code).apply()
                            languageCode = it.code
                            currentScreen = "home"
                        })
                    }

                    "home" -> {

                        HomeScreen(
                            onChangeLanguage = { currentScreen = "language" },
                            onScanSurroundings = {
                                currentScreen = "camera"
                            },

                            onIdentifyMoney = {
                                currentScreen = "currency"
                            },

                            onIdentifyProduct = {
                                // Will be implemented later
                            },

                            onReadText = {
                                currentScreen = "text"
                            },
                            onDetectObstacles = {
                                currentScreen = "obstacles"
                            }
                        )
                    }

                    "camera" -> {

                        BackHandler {
                            currentScreen = "home"
                        }

                        CameraScreen()
                    }

                    "obstacles" -> {
                        BackHandler { currentScreen = "home" }
                        ObstacleScreen(onBack = { currentScreen = "home" })
                    }

                    "currency" -> {
                        BackHandler { currentScreen = "home" }
                        CurrencyScreen(onBack = { currentScreen = "home" })
                    }

                    "text" -> {
                        BackHandler { currentScreen = "home" }
                        ReadTextScreen(onBack = { currentScreen = "home" })
                    }
                }
                }
            }
        }
    }
}
