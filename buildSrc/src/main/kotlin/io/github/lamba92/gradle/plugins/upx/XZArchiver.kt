package io.github.lamba92.gradle.plugins.upx

import org.gradle.api.internal.file.archive.compression.URIBuilder
import org.gradle.api.internal.resources.DefaultResourceHandler
import org.gradle.api.internal.resources.ResourceResolver
import org.gradle.api.resources.ResourceHandler
import org.gradle.api.resources.internal.ReadableResourceInternal
import org.tukaani.xz.SeekableFileInputStream
import org.tukaani.xz.SeekableXZInputStream
import java.io.File
import java.io.InputStream
import java.net.URI

fun File.seekable(): SeekableFileInputStream = SeekableFileInputStream(this)

fun SeekableFileInputStream.lzma2(): SeekableXZInputStream = SeekableXZInputStream(this)

fun ResourceHandler.xz(path: Any): XZArchiver {
    val resourceResolverField = DefaultResourceHandler::class.java.getDeclaredField("resourceResolver")
    resourceResolverField.isAccessible = true
    val resourceResolver = resourceResolverField.get(this) as ResourceResolver
    resourceResolverField.isAccessible = false
    val resource = resourceResolver.resolveResource(path)
    return XZArchiver(resource)
}

class XZArchiver(
    private val resource: ReadableResourceInternal,
) : ReadableResourceInternal by resource {
    private val uri: URI = URIBuilder(resource.uri).schemePrefix("xz:").build()

    override fun getURI(): URI = uri

    override fun read(): InputStream = resource.backingFile.seekable().lzma2()
}
