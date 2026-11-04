import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

abstract class VerifyLocalizationConsistencyTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val composeResourcesDirectory: DirectoryProperty

    init {
        group = "verification"
        description = "Verifies that all localized strings.xml resource files define the same keys as the total keyset (union of all languages)."
    }

    @TaskAction
    fun verify() {
        val rootDir = composeResourcesDirectory.get().asFile
        if (!rootDir.exists()) {
            logger.info("Compose resources directory $rootDir does not exist, skipping localization check.")
            return
        }

        // Collect all directories containing strings.xml: values and all values-*
        val languageDirs = rootDir.listFiles { file ->
            file.isDirectory && (file.name == "values" || file.name.startsWith("values-"))
        }?.sortedBy { it.name } ?: emptyList()

        if (languageDirs.isEmpty()) {
            logger.info("No values or values-* directories found in $rootDir.")
            return
        }

        val languageKeys = mutableMapOf<String, Set<String>>()
        val totalKeySet = mutableSetOf<String>()

        for (dir in languageDirs) {
            val stringsFile = File(dir, "strings.xml")
            if (!stringsFile.exists()) {
                languageKeys[dir.name] = emptySet()
                continue
            }
            val keys = parseStringKeys(stringsFile)
            languageKeys[dir.name] = keys
            totalKeySet.addAll(keys)
        }

        val missingPerLanguage = mutableMapOf<String, List<String>>()
        for ((lang, keys) in languageKeys) {
            val missing = (totalKeySet - keys).sorted()
            if (missing.isNotEmpty()) {
                missingPerLanguage[lang] = missing
            }
        }

        if (missingPerLanguage.isNotEmpty()) {
            val message = buildString {
                appendLine("Localization consistency check failed!")
                appendLine("Total keys across all languages (union): ${totalKeySet.size}")
                appendLine()
                missingPerLanguage.forEach { (lang, missing) ->
                    appendLine("[$lang] Missing ${missing.size} key(s):")
                    missing.forEach { key ->
                        appendLine("  - $key")
                    }
                    appendLine()
                }
            }
            throw GradleException(message.trimEnd())
        }

        logger.lifecycle("Localization consistency check passed: all ${languageDirs.size} language(s) have all ${totalKeySet.size} keys.")
    }

    private fun parseStringKeys(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        val elements = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until elements.length) {
            val item = elements.item(i)
            val nameAttr = item.attributes?.getNamedItem("name")?.nodeValue
            if (!nameAttr.isNullOrBlank()) {
                keys.add(nameAttr)
            }
        }
        return keys
    }
}

val verifyLocalization = tasks.register<VerifyLocalizationConsistencyTask>("verifyLocalizationConsistency") {
    composeResourcesDirectory.convention(
        layout.projectDirectory.dir("src/main/composeResources"),
    )
}

tasks
    .matching { it.name == "check" }
    .configureEach { dependsOn(verifyLocalization) }
