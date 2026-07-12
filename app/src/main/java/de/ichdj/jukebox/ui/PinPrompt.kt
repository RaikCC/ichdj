package de.ichdj.jukebox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.ichdj.jukebox.R
import de.ichdj.jukebox.ui.theme.ElectricRose
import de.ichdj.jukebox.ui.theme.NeonIce
import kotlinx.coroutines.delay

private const val MAX_PIN_LENGTH = 6
private const val IDLE_TIMEOUT_MS = 30_000L

/**
 * App-eigener PIN-Dialog. Vollständig in der App gerendert (funktioniert auch
 * im Kiosk-/Lock-Task): falsche Eingabe, Abbruch und 30 Sekunden Untätigkeit
 * führen zuverlässig zurück in die Besucheransicht.
 */
@Composable
fun PinPrompt(vm: MainViewModel) {
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    // Auto-Abbruch nach Untätigkeit: Timer startet bei jeder Eingabe neu.
    LaunchedEffect(entered) {
        delay(IDLE_TIMEOUT_MS)
        vm.cancelPinPrompt()
    }

    fun press(digit: String) {
        wrong = false
        if (entered.length < MAX_PIN_LENGTH) entered += digit
    }

    fun submit() {
        if (!vm.submitPin(entered)) {
            // Falsche Eingabe → sofort zurück in die Besucheransicht
            wrong = true
            entered = ""
            vm.cancelPinPrompt()
        }
    }

    Dialog(onDismissRequest = vm::cancelPinPrompt) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    stringResource(R.string.pin_prompt_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                PinDots(length = entered.length, error = wrong)
                if (wrong) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.pin_wrong),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Keypad(
                    onDigit = ::press,
                    onDelete = { wrong = false; entered = entered.dropLast(1) },
                    onSubmit = ::submit,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = vm::cancelPinPrompt) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun PinDots(length: Int, error: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(MAX_PIN_LENGTH) { i ->
            val filled = i < length
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            error -> MaterialTheme.colorScheme.error
                            filled -> NeonIce
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { d -> KeyButton(d) { onDigit(d) } }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeyButton("⌫", onClick = onDelete)
            KeyButton("0") { onDigit("0") }
            KeyButton("OK", accent = true, onClick = onSubmit)
        }
    }
}

@Composable
private fun KeyButton(label: String, accent: Boolean = false, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (accent) ElectricRose else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (accent) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
