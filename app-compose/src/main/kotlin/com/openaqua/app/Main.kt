package com.openaqua.app

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.openaqua.app.model.AppState
import com.openaqua.app.theme.OpenAquaTheme

fun main() =
    application {
        val windowState =
            rememberWindowState(
                size = DpSize(1380.dp, 880.dp),
            )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "OpenAqua - Hydro-Telemetry Cockpit",
        ) {
            val appState = remember { AppState() }

            OpenAquaTheme {
                Text("Hello World!")
            }
        }
    }
