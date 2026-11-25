import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

group = "io.github.lamba92.openaqua"
version = System.getenv("OPENAQUA_VERSION")
    ?: DateTimeFormatter
        .ofPattern("yyyy.MM.dd.HHmm")
        .format(ZonedDateTime.now(ZoneOffset.UTC))

tasks {
    if ("clean" !in names) {
        register("clean") {
            delete(layout.buildDirectory)
        }
    }
}
