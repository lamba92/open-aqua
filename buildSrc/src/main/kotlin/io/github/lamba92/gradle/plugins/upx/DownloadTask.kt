package io.github.lamba92.gradle.plugins.upx

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

abstract class DownloadTask : DefaultTask() {

    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create(url.get()))
                .GET()
                .build()

        val client =
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        if (destination.exists()) {
            destination.delete()
        }
        destination.outputStream().use { out ->
            response.body().use { input ->
                input.copyTo(out)
            }
        }
    }
}
