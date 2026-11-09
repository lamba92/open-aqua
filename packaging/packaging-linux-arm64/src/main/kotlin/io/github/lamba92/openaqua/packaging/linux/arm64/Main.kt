package io.github.lamba92.openaqua.packaging.linux.arm64

import io.github.lamba92.openaqua.packaging.linuxStoresMetadata
import io.github.lamba92.openaqua.packaging.withMetadata

fun main() = withMetadata { linuxStoresMetadata(it) }
