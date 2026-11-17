package io.github.lamba92.openaqua.app.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState

fun WindowStateStoredData.toPosition(): WindowPosition =
    when {
        xDp != null && yDp != null -> WindowPosition(xDp.dp, yDp.dp)
        else -> WindowPosition.PlatformDefault
    }

fun WindowStateStoredData.toSize(): DpSize =
    when {
        widthDp > 0f && heightDp > 0f -> DpSize(widthDp.dp, heightDp.dp)
        else -> DpSize(1380.dp, 880.dp)
    }

fun WindowPlacement.toAppWindowPlacement(): AppWindowPlacement =
    when (this) {
        WindowPlacement.Floating -> AppWindowPlacement.Floating
        WindowPlacement.Maximized -> AppWindowPlacement.Maximized
        WindowPlacement.Fullscreen -> AppWindowPlacement.Fullscreen
    }

fun AppWindowPlacement.toWindowPlacement(): WindowPlacement =
    when (this) {
        AppWindowPlacement.Floating -> WindowPlacement.Floating
        AppWindowPlacement.Maximized -> WindowPlacement.Maximized
        AppWindowPlacement.Fullscreen -> WindowPlacement.Fullscreen
    }

fun WindowState.toStoredData(previous: WindowStateStoredData = WindowStateStoredData.DEFAULT): WindowStateStoredData {
    val isFloating = placement == WindowPlacement.Floating
    return WindowStateStoredData(
        widthDp = if (isFloating && size.width.value > 0f) size.width.value else previous.widthDp,
        heightDp = if (isFloating && size.height.value > 0f) size.height.value else previous.heightDp,
        xDp = if (isFloating && position.isSpecified) position.x.value else previous.xDp,
        yDp = if (isFloating && position.isSpecified) position.y.value else previous.yDp,
        placement = placement.toAppWindowPlacement(),
    )
}
