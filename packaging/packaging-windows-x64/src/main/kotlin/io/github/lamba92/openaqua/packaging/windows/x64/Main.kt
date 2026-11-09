package io.github.lamba92.openaqua.packaging.windows.x64

import io.github.lamba92.openaqua.packaging.windowsStoresMetadata
import io.github.lamba92.openaqua.packaging.withMetadata

fun main() = withMetadata { windowsStoresMetadata(it) }
