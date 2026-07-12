package de.ichdj.jukebox

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    private val mainHandler = Handler(Looper.getMainLooper())

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as IchDjApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kiosk = KioskManager(this)
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
                var deviceOwner by remember { mutableStateOf(kiosk.isDeviceOwner()) }

                LaunchedEffect(state.mode) {
                    if (state.mode == UiMode.VISITOR) kiosk.enterKiosk() else kiosk.exitKiosk()
                }
                // Schaltbar: Display wachhalten übersteuert den OS-Sleeptimer,
                // solange die App im Vordergrund ist.
                LaunchedEffect(state.settings.keepScreenOn) {
                    if (state.settings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
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
                        isDeviceOwner = deviceOwner,
                        onConnect = { viewModel.connectSpotify(this@MainActivity) },
                        onReleaseDeviceOwner = {
                            if (kiosk.clearDeviceOwner()) {
                                deviceOwner = false
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.operator_release_done),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.operator_release_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
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
     * Fingerabdruck). In-App-Prompt OHNE Aufhebung des Pinnings (stopLockTask
     * würde den Sperrbildschirm auslösen). Damit die Abfrage nie offen stehen
     * bleibt, wenn ein Besucher einfach weggeht: falsche Biometrie bricht
     * sofort ab, und nach 30 Sekunden ohne Erfolg wird automatisch
     * abgebrochen – beides führt zurück in die Besucheransicht.
     */
    private fun promptOperatorUnlock() {
        val authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        val canAuth = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            viewModel.enterOperator() // keine Gerätesperre eingerichtet
            return
        }

        var prompt: BiometricPrompt? = null
        val autoCancel = Runnable { prompt?.cancelAuthentication() }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                mainHandler.removeCallbacks(autoCancel)
                viewModel.enterOperator()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                mainHandler.removeCallbacks(autoCancel)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.operator_unlock_failed),
                    Toast.LENGTH_SHORT,
                ).show()
                kiosk.enterKiosk()
            }

            override fun onAuthenticationFailed() {
                // Falsche Biometrie: sofort zurück in die Besucheransicht
                prompt?.cancelAuthentication()
            }
        }

        prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback)
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.operator_unlock_title))
                .setSubtitle(getString(R.string.operator_unlock_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
        mainHandler.postDelayed(autoCancel, 30_000)
    }

    private fun exitApp() {
        kiosk.exitKiosk()
        finishAndRemoveTask()
    }
}
