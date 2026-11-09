package io.github.lamba92.openaqua.packaging.linux.x64

import io.github.lamba92.openaqua.packaging.linuxStoresMetadata
import io.github.lamba92.openaqua.packaging.withMetadata

fun main() = withMetadata { linuxStoresMetadata(it) }
