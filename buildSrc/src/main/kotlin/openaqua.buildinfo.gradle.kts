import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.time.Instant

abstract class OpenAquaBuildInfoExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val packageName: Property<String> = objects.property<String>()
        val className: Property<String> = objects.property<String>().convention("OpenAquaBuildInfo")
        val version: Property<String> = objects.property<String>()
        val buildTimestamp: Property<String> = objects.property<String>()
        val outputDirectory: DirectoryProperty = objects.directoryProperty()
    }

val extension = extensions.create<OpenAquaBuildInfoExtension>("buildInfo")

extension.packageName.convention("io.github.lamba92.openaqua.app.build")
extension.className.convention("OpenAquaBuildInfo")
extension.version.convention(provider { version.toString() })
extension.outputDirectory.convention(
    layout.buildDirectory.dir(
        extension.packageName.map { "generated/kotlin/main/${it.replace('.', '/')}" },
    ),
)
extension.buildTimestamp.convention(provider { Instant.now().toString() })

private data class ResolvedCatalogLibrary(
    val alias: String,
    val group: String,
    val artifact: String,
    val version: String,
)

val generateOpenAquaBuildInfo by tasks.registering {
    val jvmVer = System.getProperty("java.version") ?: "unknown"
    val jvmVend = System.getProperty("java.vendor") ?: "unknown"
    val osName = System.getProperty("os.name") ?: "unknown"
    val osArch = System.getProperty("os.arch") ?: "unknown"
    val kotlinVer = libs.versions.kotlin.get()

    val versionCatalogs =
        project.extensions.findByType<VersionCatalogsExtension>()
            ?: project.rootProject.extensions.getByType<VersionCatalogsExtension>()
    val catalog = versionCatalogs.named("libs")
    val catalogLibraries =
        catalog.libraryAliases
            .mapNotNull { alias ->
                catalog.findLibrary(alias).orElse(null)?.get()?.let { dep ->
                    ResolvedCatalogLibrary(
                        alias = alias,
                        group = dep.module.group,
                        artifact = dep.module.name,
                        version = dep.versionConstraint.displayName.ifBlank { "unspecified" },
                    )
                }
            }.sortedBy { it.alias }

    inputs.property("packageName", extension.packageName)
    inputs.property("className", extension.className)
    inputs.property("version", extension.version)
    inputs.property("buildTimestamp", extension.buildTimestamp)
    inputs.property("gradleVersion", gradle.gradleVersion)
    inputs.property("jvmVersion", jvmVer)
    inputs.property("jvmVendor", jvmVend)
    inputs.property("osName", osName)
    inputs.property("osArch", osArch)
    inputs.property("kotlinVersion", kotlinVer)
    inputs.property("catalogLibraries", catalogLibraries.map { "${it.alias}=${it.group}:${it.artifact}:${it.version}" })
    outputs.dir(extension.outputDirectory)

    doLast {
        val targetDir = extension.outputDirectory.get().asFile
        targetDir.deleteRecursively()
        targetDir.mkdirs()

        val libraryClassName = ClassName(extension.packageName.get(), extension.className.get(), "Library")

        val libraryTypeSpec =
            TypeSpec
                .classBuilder("Library")
                .addModifiers(KModifier.DATA)
                .primaryConstructor(
                    FunSpec
                        .constructorBuilder()
                        .addParameter("alias", String::class)
                        .addParameter("group", String::class)
                        .addParameter("artifact", String::class)
                        .addParameter("version", String::class)
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("alias", String::class)
                        .initializer("alias")
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("group", String::class)
                        .initializer("group")
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("artifact", String::class)
                        .initializer("artifact")
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("version", String::class)
                        .initializer("version")
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("coordinates", String::class)
                        .getter(
                            FunSpec
                                .getterBuilder()
                                .addStatement("return \"\$group:\$artifact\"")
                                .build(),
                        ).build(),
                ).build()

        val listType = List::class.asTypeName().parameterizedBy(libraryClassName)
        val librariesCodeBlock =
            CodeBlock
                .builder()
                .add("listOf(\n")
                .indent()

        catalogLibraries.forEach { lib ->
            librariesCodeBlock.add(
                "%T(alias = %S, group = %S, artifact = %S, version = %S),\n",
                libraryClassName,
                lib.alias,
                lib.group,
                lib.artifact,
                lib.version,
            )
        }
        librariesCodeBlock.unindent().add(")")

        val typeSpec =
            TypeSpec
                .objectBuilder(extension.className.get())
                .addProperty(
                    PropertySpec
                        .builder("VERSION", String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", extension.version.get())
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("BUILD_TIMESTAMP", String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", extension.buildTimestamp.get())
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("GRADLE_VERSION", String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", gradle.gradleVersion)
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("KOTLIN_VERSION", String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", kotlinVer)
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("JVM_VERSION", String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", jvmVer)
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("JVM_VENDOR", String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", jvmVend)
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("OS_NAME", String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", osName)
                        .build(),
                ).addProperty(
                    PropertySpec
                        .builder("OS_ARCH", String::class)
                        .addModifiers(KModifier.CONST)
                        .initializer("%S", osArch)
                        .build(),
                ).addType(libraryTypeSpec)
                .addProperty(
                    PropertySpec
                        .builder("LIBRARIES", listType)
                        .initializer(librariesCodeBlock.build())
                        .build(),
                ).build()

        val fileSpec =
            FileSpec
                .builder(extension.packageName.get(), extension.className.get())
                .addType(typeSpec)
                .indent("    ")
                .build()

        targetDir.resolve("${extension.className.get()}.kt").writeText(fileSpec.toString())
    }
}

plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        sourceSets.named("main") {
            kotlin.srcDir(generateOpenAquaBuildInfo.flatMap { extension.outputDirectory }.get())
        }
    }
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension>("kotlin") {
        sourceSets.named("commonMain") {
            kotlin.srcDir(generateOpenAquaBuildInfo.flatMap { extension.outputDirectory }.get())
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateOpenAquaBuildInfo)
}

tasks.matching { it.name.startsWith("runKtlintCheckOver") }.configureEach {
    mustRunAfter(generateOpenAquaBuildInfo)
}
