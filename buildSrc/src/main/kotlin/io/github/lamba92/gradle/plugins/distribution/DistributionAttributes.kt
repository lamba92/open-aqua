package io.github.lamba92.gradle.plugins.distribution

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

enum class DistributionType(
    private val id: String,
) : Named {
    APP_DIR("app-dir"),
    PACKAGES("packages"),
    ;

    override fun getName(): String = id
}

val DISTRIBUTION_TYPE_ATTRIBUTE: Attribute<DistributionType> =
    Attribute.of("io.github.lamba92.openaqua.distribution.type", DistributionType::class.java)
