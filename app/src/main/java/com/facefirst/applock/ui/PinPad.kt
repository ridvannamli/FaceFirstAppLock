package com.facefirst.applock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Rakam tuş takımı. `resetKey` değişince girilen rakamlar silinir. PIN tamamlanınca onComplete çağrılır. */
@Composable
fun PinPad(length: Int, resetKey: Int, enabled: Boolean, onComplete: (String) -> Unit) {
    var pin by remember(resetKey) { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(length) { i ->
                Box(
                    Modifier.size(14.dp).background(
                        if (i < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫").chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { k ->
                    if (k.isEmpty()) Spacer(Modifier.size(72.dp)) else FilledTonalButton(
                        onClick = {
                            if (!enabled) return@FilledTonalButton
                            if (k == "⌫") pin = pin.dropLast(1)
                            else if (pin.length < length) {
                                val np = pin + k
                                pin = np
                                if (np.length == length) onComplete(np)
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) { Text(k, style = MaterialTheme.typography.headlineSmall) }
                }
            }
        }
    }
}

