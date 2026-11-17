package io.github.lamba92.openaqua.app.util

actual fun String.format(vararg args: Any?): String = java.lang.String.format(this, *args)
