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

enum class PackageFormat(
    private val id: String,
) : Named {
    APP_DIR("app-dir"),
    TAR_GZ("tar-gz"),
    ZIP("zip"),
    DEB("deb"),
    RPM("rpm"),
    MSI("msi"),
    MSIX("msix"),
    APP_IMAGE("app-image"),
    ;

    override fun getName(): String = id
}

val PACKAGE_FORMAT_ATTRIBUTE: Attribute<PackageFormat> =
    Attribute.of("io.github.lamba92.openaqua.package.format", PackageFormat::class.java)
