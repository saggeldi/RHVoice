package com.github.olga_yakovleva.rhvoice.compose.actions

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VolumeButtonsHandler(
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onPowerButtonSingleClick: () -> Unit = {},
    onPowerButtonDoubleClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current

    var lastPowerKeyTime by remember { mutableStateOf(0L) }
    var powerClickJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(context) {
        val keyEventDispatcher = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            println("Key pressed: ${event.keyCode}")
            if (event.action != KeyEvent.ACTION_DOWN) return@OnUnhandledKeyEventListenerCompat false


            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    onVolumeUp()
                    true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    onVolumeDown()
                    true
                }

                KeyEvent.KEYCODE_POWER -> {
                    val currentTime = SystemClock.uptimeMillis()
                    val delta = currentTime - lastPowerKeyTime

                    if (delta < 400) {
                        powerClickJob?.cancel()
                        onPowerButtonDoubleClick()
                    } else {
                        powerClickJob?.cancel()
                        powerClickJob = kotlinx.coroutines.MainScope().launch {
                            delay(400)
                            onPowerButtonSingleClick()
                        }
                    }

                    lastPowerKeyTime = currentTime
                    true
                }

                else -> false
            }
        }

        ViewCompat.addOnUnhandledKeyEventListener(view, keyEventDispatcher)

        onDispose {
            ViewCompat.removeOnUnhandledKeyEventListener(view, keyEventDispatcher)
            powerClickJob?.cancel()
        }
    }
}
