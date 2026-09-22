package com.moeketsifaso.ntsamaise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var currentScreen by rememberSaveable {
                mutableStateOf("home")
            }

            when (currentScreen) {

                "home" -> {

                    HomeScreen(
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
                        }
                    )
                }

                "camera" -> {

                    BackHandler {
                        currentScreen = "home"
                    }

                    CameraScreen()
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
