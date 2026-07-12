package de.ichdj.jukebox

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.ichdj.jukebox.kiosk.KioskManager
import de.ichdj.jukebox.ui.MainViewModel
import de.ichdj.jukebox.ui.OperatorScreen
import de.ichdj.jukebox.ui.UiMode
import de.ichdj.jukebox.ui.VisitorScreen
import de.ichdj.jukebox.ui.theme.IchDjTheme

class MainActivity : AppCompatActivity() {

    private lateinit var kiosk: KioskManager
    private lateinit var credentialLauncher: ActivityResultLauncher<Intent>

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as IchDjApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kiosk = KioskManager(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (application as IchDjApplication).container.engine.start()

        // Ergebnis der Gerätesperren-Bestätigung: Erfolg → Veranstaltermenü,
        // Abbruch/Fehlschlag → einfach zurück in die Besucheransicht.
        credentialLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.enterOperator()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.operator_unlock_failed),
                    Toast.LENGTH_SHORT,
                ).show()
                kiosk.enterKiosk()
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Besuchermodus: Zurück ist gesperrt.
                    // Veranstaltermodus: Zurück führt in die Besucheransicht.
                    if (viewModel.currentMode() == UiMode.OPERATOR) viewModel.exitOperator()
                }
            },
        )

        setContent {
            IchDjTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(state.mode) {
                    if (state.mode == UiMode.VISITOR) kiosk.enterKiosk() else kiosk.exitKiosk()
                }
                when (state.mode) {
                    UiMode.VISITOR -> VisitorScreen(
                        state = state,
                        vm = viewModel,
                        onOperatorRequest = ::promptOperatorUnlock,
                    )
                    UiMode.OPERATOR -> OperatorScreen(
                        state = state,
                        vm = viewModel,
                        isDeviceOwner = kiosk.isDeviceOwner(),
                        onConnect = { viewModel.connectSpotify(this@MainActivity) },
                        onExitApp = ::exitApp,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Nach Unterbrechungen (z.B. Browser der OAuth-Anmeldung) Kiosk wiederherstellen
        if (viewModel.currentMode() == UiMode.VISITOR) kiosk.enterKiosk()
    }

    /**
     * Entsperrt das Veranstaltermenü über die Bestätigung der Gerätesperre
     * (KeyguardManager statt BiometricPrompt: nur eine Eingabe, und das Gerät
     * wird dabei nicht in den Display-Lock versetzt – Abbruch führt einfach
     * zurück in die Besucheransicht). Ungesicherte Geräte kommen direkt hinein.
     */
    private fun promptOperatorUnlock() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isDeviceSecure) {
            viewModel.enterOperator()
            return
        }
        // Pinning kurz aufheben, sonst kann der Bestätigungsdialog unterdrückt werden
        kiosk.unpin()
        @Suppress("DEPRECATION")
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            getString(R.string.operator_unlock_title),
            getString(R.string.operator_unlock_subtitle),
        )
        if (intent == null) {
            viewModel.enterOperator()
            return
        }
        credentialLauncher.launch(intent)
    }

    private fun exitApp() {
        kiosk.exitKiosk()
        finishAndRemoveTask()
    }
}
