package com.moeketsifaso.ntsamaise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var currentScreen by remember {
                mutableStateOf("home")
            }

            when (currentScreen) {

                "home" -> {

                    HomeScreen(
                        onScanSurroundings = {
                            currentScreen = "camera"
                        },

                        onIdentifyMoney = {
                            // Will be implemented later
                        },

                        onIdentifyProduct = {
                            // Will be implemented later
                        },

                        onReadText = {
                            // Will be implemented later
                        }
                    )
                }

                "camera" -> {

                    BackHandler {
                        currentScreen = "home"
                    }

                    CameraScreen()
                }
            }
        }
    }
}