package io.github.lamba92.openaqua.app.util

import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

actual fun String.toClipEntry(): ClipEntry = ClipEntry(StringSelection(this))
