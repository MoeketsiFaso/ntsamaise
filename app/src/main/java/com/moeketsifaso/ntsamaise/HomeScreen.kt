package com.moeketsifaso.ntsamaise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable

fun HomeScreen(
    onChangeLanguage: () -> Unit,
    onScanSurroundings: () -> Unit,
    onIdentifyMoney: () -> Unit,
    onIdentifyProduct: () -> Unit,
    onReadText: () -> Unit,
    onDetectObstacles: () -> Unit
) {
    val language = LocalAppLanguage.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        UiText(
            text = "NTSAMAISE",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        UiText(
            text = "Your visual assistant",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = onChangeLanguage, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            UiText("Change language")
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onScanSurroundings,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .semantics {
                    contentDescription =
                        language.ui("Scan Surroundings")
                }
        ) {
            UiText(
                text = "Scan Surroundings",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDetectObstacles,
            modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp).semantics {
                contentDescription = language.ui("Detect Obstacles")
            }
        ) {
            UiText("Detect Obstacles", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onIdentifyMoney,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .semantics {
                    contentDescription =
                        language.ui("Identify Money")
                }
        ) {
            UiText(
                text = "Identify Money",
                fontSize = 20.sp
            )
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onReadText,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .semantics {
                    contentDescription =
                        language.ui("Read Text")
                }
        ) {
            UiText(
                text = "Read Text",
                fontSize = 20.sp
            )
        }
    }
}
