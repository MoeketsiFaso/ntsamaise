package com.moeketsifaso.ntsamaise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onScanSurroundings: () -> Unit,
    onIdentifyMoney: () -> Unit,
    onIdentifyProduct: () -> Unit,
    onReadText: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "NTSAMAISE",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mothusi oa hau ea hloa-hloa",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onScanSurroundings,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .semantics {
                    contentDescription =
                        "Scan surroundings. Opens the camera to identify objects around you."
                }
        ) {
            Text(
                text = "Scan Surroundings",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onIdentifyMoney,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .semantics {
                    contentDescription =
                        "Identify money. Scan a Lesotho banknote."
                }
        ) {
            Text(
                text = "Identify Money",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onIdentifyProduct,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .semantics {
                    contentDescription =
                        "Identify product. Scan a local product or logo."
                }
        ) {
            Text(
                text = "Identify Product",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onReadText,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .semantics {
                    contentDescription =
                        "Read text. Use the camera to read printed text aloud."
                }
        ) {
            Text(
                text = "Read Text",
                fontSize = 20.sp
            )
        }
    }
}