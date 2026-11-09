plugins {
    openaqua.convention.jvm
    id(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kaml)
    api(libs.xmlutil.serialization)
    api(libs.kotlinx.serialization.json)
}
