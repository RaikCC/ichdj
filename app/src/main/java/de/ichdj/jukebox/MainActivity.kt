package de.ichdj.jukebox

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.ichdj.jukebox.kiosk.KioskManager
import de.ichdj.jukebox.ui.MainViewModel
import de.ichdj.jukebox.ui.OperatorScreen
import de.ichdj.jukebox.ui.UiMode
import de.ichdj.jukebox.ui.VisitorScreen
import de.ichdj.jukebox.ui.theme.IchDjTheme

class MainActivity : AppCompatActivity() {

    private lateinit var kiosk: KioskManager

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as IchDjApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kiosk = KioskManager(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (application as IchDjApplication).container.engine.start()

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
     * Entsperrt das Veranstaltermenü über die Gerätesperre (Muster/PIN/
     * Fingerabdruck). Bewusst als In-App-BiometricPrompt und OHNE Aufhebung
     * des Kiosk-Pinnings: Ein stopLockTask() löst auf vielen Geräten den
     * Sperrbildschirm aus – das wäre dann eine zweite, ungewollte
     * Muster-Abfrage. Ungesicherte Geräte kommen direkt hinein; Abbruch oder
     * Fehlschlag führt zurück in die Besucheransicht (nie „gesperrt").
     */
    private fun promptOperatorUnlock() {
        val authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        val canAuth = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            viewModel.enterOperator() // keine Gerätesperre eingerichtet
            return
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.enterOperator()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Abbruch, Timeout, falsche Eingabe zu oft → einfach zurück
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.operator_unlock_failed),
                    Toast.LENGTH_SHORT,
                ).show()
                kiosk.enterKiosk()
            }
        }
        BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.operator_unlock_title))
                .setSubtitle(getString(R.string.operator_unlock_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }

    private fun exitApp() {
        kiosk.exitKiosk()
        finishAndRemoveTask()
    }
}
