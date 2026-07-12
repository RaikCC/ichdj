package de.ichdj.jukebox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.ichdj.jukebox.BuildConfig
import de.ichdj.jukebox.R

@Composable
fun OperatorScreen(
    state: JukeboxUiState,
    vm: MainViewModel,
    isDeviceOwner: Boolean,
    onConnect: () -> Unit,
    onReleaseDeviceOwner: () -> Unit,
    onExitApp: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding() // nicht unter Leisten/Display-Aussparung zeichnen
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.operator_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            // Immer sichtbar (nicht am scrollenden Listenende), damit am
            // Gerät zweifelsfrei erkennbar ist, welcher Build läuft.
            Text(
                "IchDJ v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
                OutlinedButton(
                    onClick = vm::disconnectSpotify,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(stringResource(R.string.operator_disconnect))
                }
            } else {
                Text(
                    stringResource(R.string.operator_not_connected),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                // Bewusst auch während eines laufenden Versuchs klickbar:
                // ein erneuter Tipp bricht ab und startet die Anmeldung neu.
                Button(
                    onClick = onConnect,
                    enabled = (clientIdInput ?: state.settings.clientId).isNotBlank(),
                ) {
                    Text(
                        if (state.authBusy) "… (neu starten)"
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
            SwitchRow(
                label = stringResource(R.string.operator_wishes_enabled),
                checked = state.settings.wishesEnabled,
                onChange = vm::setWishesEnabled,
            )
            SwitchRow(
                label = stringResource(R.string.operator_keep_screen_on),
                checked = state.settings.keepScreenOn,
                onChange = vm::setKeepScreenOn,
            )
            SwitchRow(
                label = stringResource(R.string.operator_wish_log),
                checked = state.settings.wishLogEnabled,
                onChange = vm::setWishLogEnabled,
            )
            Text(
                stringResource(R.string.operator_wish_log_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(stringResource(R.string.operator_section_kiosk)) {
            var confirmRelease by remember { mutableStateOf(false) }
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
                OutlinedButton(
                    onClick = onExitApp,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(stringResource(R.string.operator_exit_app))
                }
                if (isDeviceOwner) {
                    OutlinedButton(
                        onClick = { confirmRelease = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(R.string.operator_release_kiosk))
                    }
                }
            }
            if (confirmRelease) {
                AlertDialog(
                    onDismissRequest = { confirmRelease = false },
                    title = { Text(stringResource(R.string.operator_release_kiosk_confirm_title)) },
                    text = { Text(stringResource(R.string.operator_release_kiosk_confirm_text)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmRelease = false
                                onReleaseDeviceOwner()
                            },
                        ) {
                            Text(stringResource(R.string.operator_release_kiosk))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmRelease = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
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
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * Zahlenwert mit −/+ in Schritten; Tipp auf den Wert öffnet eine
 * Numpad-Eingabe für beliebige Werte innerhalb des erlaubten Bereichs.
 */
@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit,
) {
    var showInput by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Button(onClick = { onChange((value - step).coerceIn(range)) }) { Text("–") }
        TextButton(onClick = { showInput = true }) {
            Text(
                "$value",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp),
            )
        }
        Button(onClick = { onChange((value + step).coerceIn(range)) }) { Text("+") }
    }

    if (showInput) {
        var text by remember { mutableStateOf(value.toString()) }
        AlertDialog(
            onDismissRequest = { showInput = false },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter(Char::isDigit).take(5) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.number_input_range,
                                range.first,
                                range.last,
                            ),
                        )
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        text.toIntOrNull()?.let { onChange(it.coerceIn(range)) }
                        showInput = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInput = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
