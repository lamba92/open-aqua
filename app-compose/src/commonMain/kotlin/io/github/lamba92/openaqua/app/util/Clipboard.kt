package io.github.lamba92.openaqua.app.util

import androidx.compose.ui.platform.ClipEntry

fun String.toClipEntry(): ClipEntry = ClipEntry.withPlainText(this)
