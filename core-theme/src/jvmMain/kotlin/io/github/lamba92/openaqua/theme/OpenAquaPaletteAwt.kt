package io.github.lamba92.openaqua.theme

import java.awt.Color as AwtColor

fun ThemeColor.toAwtColor(): AwtColor = AwtColor(red, green, blue, alpha)
