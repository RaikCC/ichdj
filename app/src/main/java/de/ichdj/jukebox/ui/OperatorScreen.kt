package de.ichdj.jukebox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ichdj.jukebox.R

@Composable
fun OperatorScreen(
    state: JukeboxUiState,
    vm: MainViewModel,
    isDeviceOwner: Boolean,
    onConnect: () -> Unit,
    onExitApp: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            stringResource(R.string.operator_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        SectionCard(stringResource(R.string.operator_section_spotify)) {
            // Lokaler Feldinhalt, damit der Cursor beim Tippen nicht springt
            var clientIdInput by rememberSaveable { mutableStateOf<String?>(null) }
            OutlinedTextField(
                value = clientIdInput ?: state.settings.clientId,
                onValueChange = {
                    clientIdInput = it
                    vm.setClientId(it)
                },
                label = { Text(stringResource(R.string.operator_client_id)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (state.accountName != null) {
                Text(
                    stringResource(R.string.operator_connected_as, state.accountName),
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = vm::disconnectSpotify) {
                    Text(stringResource(R.string.operator_disconnect))
                }
            } else {
                Text(
                    stringResource(R.string.operator_not_connected),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onConnect,
                    enabled = !state.authBusy &&
                        (clientIdInput ?: state.settings.clientId).isNotBlank(),
                ) {
                    Text(
                        if (state.authBusy) "…"
                        else stringResource(R.string.operator_connect),
                    )
                }
            }
            state.authError?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.operator_auth_failed, it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SectionCard(stringResource(R.string.operator_section_settings)) {
            StepperRow(
                label = stringResource(R.string.operator_wish_boxes),
                value = state.settings.wishBoxCount,
                range = 1..8,
                step = 1,
                onChange = vm::setWishBoxCount,
            )
            StepperRow(
                label = stringResource(R.string.operator_lock_minutes),
                value = state.settings.lockMinutes,
                range = 0..1440,
                step = 15,
                onChange = vm::setLockMinutes,
            )
            StepperRow(
                label = stringResource(R.string.operator_max_length),
                value = state.settings.maxTrackMinutes,
                range = 1..60,
                step = 1,
                onChange = vm::setMaxTrackMinutes,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.operator_wishes_enabled),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.settings.wishesEnabled,
                    onCheckedChange = vm::setWishesEnabled,
                )
            }
        }

        SectionCard(stringResource(R.string.operator_section_kiosk)) {
            Text(
                stringResource(
                    if (isDeviceOwner) R.string.operator_kiosk_owner
                    else R.string.operator_kiosk_pinned,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = vm::exitOperator) {
                    Text(stringResource(R.string.operator_back_to_visitor))
                }
                OutlinedButton(onClick = onExitApp) {
                    Text(stringResource(R.string.operator_exit_app))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - step).coerceIn(range)) }) { Text("–") }
        Text(
            "$value",
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .width(48.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = { onChange((value + step).coerceIn(range)) }) { Text("+") }
    }
}
