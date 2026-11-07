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
import com.facefirst.applock.lock.BiometricStatus
import com.facefirst.applock.lock.Policy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun AppsScreen(
    protectionOn: Boolean,
    biometricStatus: BiometricStatus,
    onOpenA11ySettings: () -> Unit,
    onChangePin: () -> Unit,
    recoveryMasked: String?,
    recoveryEnabled: Boolean,
    onChangeEmail: () -> Unit,
    onRequestDisable: () -> Unit,
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
            stringResource(
                when (biometricStatus) {
                    BiometricStatus.STRONG -> R.string.bio_strong
                    BiometricStatus.WEAK_ONLY -> R.string.bio_weak
                    BiometricStatus.NOT_ENROLLED -> R.string.bio_not_enrolled
                    else -> R.string.bio_none
                }
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            pluralStringResource(R.plurals.locked_count, s.totalLocked, s.totalLocked),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            if (recoveryMasked != null) stringResource(R.string.rec_status_on, recoveryMasked) else stringResource(R.string.rec_status_off),
            style = MaterialTheme.typography.bodySmall,
        )
        Row {
            TextButton(onClick = onChangePin) { Text(stringResource(R.string.change_pin)) }
            if (recoveryEnabled) TextButton(onClick = onChangeEmail) { Text(stringResource(R.string.change_email)) }
        }
        if (s.enabled) {
            TextButton(onClick = onRequestDisable) { Text(stringResource(R.string.protection_disable)) }
        } else {
            Text(stringResource(R.string.protection_paused), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = vm::enable) { Text(stringResource(R.string.protection_resume)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.policy_global_label), style = MaterialTheme.typography.bodySmall)
            PolicyMenu(current = s.global, allowDefault = false, onPick = { vm.setGlobal(it!!) })
        }
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
                        if (app.packageName in s.locked) {
                            PolicyMenu(current = s.overrides[app.packageName], allowDefault = true, onPick = { vm.setOverride(app.packageName, it) })
                        }
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

@Composable
private fun policyLabel(p: Policy?): String = stringResource(
    when (p) {
        null -> R.string.policy_default
        Policy.EVERY_ENTRY -> R.string.policy_every
        Policy.SCREEN_OFF -> R.string.policy_screen_off
        Policy.T30 -> R.string.policy_30s
        Policy.T60 -> R.string.policy_1m
        Policy.T300 -> R.string.policy_5m
        Policy.T600 -> R.string.policy_10m
    }
)

/** current == null: uygulamaya özel ayar yok (genel geçerli). */
@Composable
private fun PolicyMenu(current: Policy?, allowDefault: Boolean, onPick: (Policy?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(policyLabel(current), style = MaterialTheme.typography.bodySmall) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (allowDefault) DropdownMenuItem(text = { Text(policyLabel(null)) }, onClick = { open = false; onPick(null) })
            Policy.entries.forEach { p -> DropdownMenuItem(text = { Text(policyLabel(p)) }, onClick = { open = false; onPick(p) }) }
        }
    }
}
