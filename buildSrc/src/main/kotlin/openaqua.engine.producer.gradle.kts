import io.github.lamba92.gradle.plugins.engine.ARCH_ATTRIBUTE
import io.github.lamba92.gradle.plugins.engine.EngineBinarySpec
import io.github.lamba92.gradle.plugins.engine.OS_ATTRIBUTE
import org.gradle.api.attributes.Usage


val binaries = container<EngineBinarySpec>()
extensions.add("engineBinaries", binaries)

val prepareEngineBinariesAll =
    tasks.register<Sync>("prepareEngineBinariesAll") {
        group = "engine"
        description = "Collects all engine executables"
        into(layout.buildDirectory.dir("engine-binaries/all"))
    }

configurations.create("engineBinariesAll") {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("all-engine-binaries"))
    }
    outgoing.artifact(prepareEngineBinariesAll)
}

afterEvaluate {
    binaries.forEach { spec ->
        val configSuffix = spec.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val prepareTargetTask =
            tasks.register<Sync>("prepareEngineBinary$configSuffix") {
                group = "engine"
                description = "Prepares ${spec.name} binary for distribution"
                into(layout.buildDirectory.dir("engine-binaries/${spec.name}"))
                from(spec.binaryFile) {
                    rename { spec.canonicalName.get() }
                }
            }

        prepareEngineBinariesAll.configure {
            from(spec.binaryFile) {
                rename { spec.canonicalName.get() }
            }
        }

        configurations.create("engineBinary$configSuffix") {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("engine-binaries"))
                attribute(OS_ATTRIBUTE, spec.os.get())
                attribute(ARCH_ATTRIBUTE, spec.arch.get())
            }
            outgoing.artifact(prepareTargetTask)
        }
    }
}
