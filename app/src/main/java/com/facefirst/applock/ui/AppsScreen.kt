package com.facefirst.applock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.facefirst.applock.R
import com.facefirst.applock.data.InstalledApps

@Composable
fun AppsScreen(
    protectionOn: Boolean,
    onOpenA11ySettings: () -> Unit,
    vm: AppsViewModel = viewModel(),
) {
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (protectionOn) {
            Text(stringResource(R.string.protection_on), color = MaterialTheme.colorScheme.primary)
        } else {
            Text(stringResource(R.string.protection_off), color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.a11y_disclosure), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
            TextButton(onClick = onOpenA11ySettings) { Text(stringResource(R.string.open_a11y_settings)) }
        }
        Text(
            pluralStringResource(R.plurals.locked_count, s.totalLocked, s.totalLocked),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = s.query, onValueChange = vm::setQuery, singleLine = true,
            label = { Text(stringResource(R.string.search_hint)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.setAllVisible(true) }) { Text(stringResource(R.string.lock_all_visible)) }
            TextButton(onClick = { vm.setAllVisible(false) }) { Text(stringResource(R.string.unlock_all_visible)) }
        }
        if (s.loading) {
            CircularProgressIndicator()
        } else if (s.apps.isEmpty()) {
            Text(stringResource(R.string.no_apps))
        } else {
            LazyColumn {
                items(s.apps, key = { it.packageName }) { app ->
                    val ctx = LocalContext.current
                    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, app.packageName) {
                        value = InstalledApps.icon(ctx, app.packageName)?.asImageBitmap()
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        icon?.let { Image(it, null, Modifier.size(40.dp)) } ?: Column(Modifier.size(40.dp)) {}
                        Text(app.label, Modifier.weight(1f).padding(horizontal = 12.dp))
                        Switch(
                            checked = app.packageName in s.locked,
                            onCheckedChange = { vm.toggle(app.packageName, it) },
                        )
                    }
                }
            }
        }
    }
}
