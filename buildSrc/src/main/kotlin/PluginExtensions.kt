import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependency
import org.gradle.plugin.use.PluginDependencySpec

fun PluginDependenciesSpec.id(plugin: Provider<PluginDependency>): PluginDependencySpec =
    id(plugin.get().pluginId)

fun PluginDependenciesSpec.id(plugin: PluginDependency): PluginDependencySpec =
    id(plugin.pluginId)
