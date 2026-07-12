package de.ichdj.jukebox

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                        onOperatorRequest = viewModel::requestOperatorUnlock,
                    )
                    UiMode.OPERATOR -> OperatorScreen(
                        state = state,
                        vm = viewModel,
                        isDeviceOwner = deviceOwner,
                        onConnect = { viewModel.connectSpotify(this@MainActivity) },
                        onReleaseDeviceOwner = {
                            val released = kiosk.clearDeviceOwner()
                            deviceOwner = deviceOwner && !released
                            Toast.makeText(
                                this@MainActivity,
                                getString(
                                    if (released) R.string.operator_release_done
                                    else R.string.operator_release_failed,
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
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

    private fun exitApp() {
        kiosk.exitKiosk()
        finishAndRemoveTask()
    }
}
