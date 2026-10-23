package com.moeketsifaso.ntsamaise

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class LanguageScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun chooseAndChangeLanguageUpdatesHomeControls() {
        compose.setContent {
            var language by remember { mutableStateOf(AppLanguage.ENGLISH) }
            var choosing by remember { mutableStateOf(true) }
            CompositionLocalProvider(LocalAppLanguage provides language) {
                if (choosing) LanguageScreen { language = it; choosing = false }
                else HomeScreen(onChangeLanguage = { choosing = true },
                    onScanSurroundings = {}, onIdentifyMoney = {}, onIdentifyProduct = {},
                    onReadText = {}, onDetectObstacles = {})
            }
        }
        compose.onNodeWithText("Sesotho").performClick()
        compose.onNodeWithText("Hlahloba tikoloho").assertExists()
        compose.onNodeWithText("Fetola puo").performScrollTo().performClick()
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("Scan Surroundings").assertExists()
        compose.onNodeWithText("Change language").assertExists()
    }
}
