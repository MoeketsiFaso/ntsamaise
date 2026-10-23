package com.moeketsifaso.ntsamaise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun LanguageScreen(onSelect: (AppLanguage) -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("NTSAMAISE", style = MaterialTheme.typography.headlineLarge)
        Text("Choose your language / Khetha puo ea hau",
            style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Choose the language for text and spoken feedback. You can change it from the home screen.")
        Text("Khetha puo ea mongolo le ea lentsoe. U ka e fetola leqepheng la hae.")
        Button(onClick = { onSelect(AppLanguage.ENGLISH) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            Text("English", style = MaterialTheme.typography.titleLarge)
        }
        Button(onClick = { onSelect(AppLanguage.SESOTHO) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            Text("Sesotho", style = MaterialTheme.typography.titleLarge)
        }
    }
}
