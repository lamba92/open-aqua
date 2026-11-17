# Preserve stacktraces and debugging metadata
-dontobfuscate
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Prevent ProGuard from narrowing return types (method specialization), which breaks the JVM bytecode verifier for Compose Text / Paragraph
-optimizations !method/specialization/*
-keep class androidx.compose.ui.text.** { *; }
-keep interface androidx.compose.ui.text.** { *; }

# Ignore warnings from internal dynamic reflection in dependencies
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-dontwarn com.typesafe.config.**
-dontwarn org.slf4j.**
-dontwarn org.fusesource.jansi.**
-dontwarn org.apache.logging.**
-dontwarn org.json.**
-dontwarn com.github.ajalt.clikt.**
-dontwarn com.github.ajalt.mordant.**
-dontwarn dev.nucleusframework.composenativetray.**

# Suppress informational notes from dependencies and serializers
-dontnote

# Kermit & Coroutines reflection
-keep class co.touchlab.kermit.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Compose resources and serialization
-keep class org.jetbrains.compose.resources.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }

# Clikt & Mordant CLI
-keep class com.github.ajalt.clikt.** { *; }
-keep class com.github.ajalt.mordant.** { *; }
-keep interface com.github.ajalt.mordant.** { *; }

# Native Tray & JNI bindings
-keep class dev.nucleusframework.composenativetray.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# ServiceLoader and Ktor Serialization
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider { *; }
-keep class * implements org.slf4j.spi.SLF4JServiceProvider { *; }
-keep class io.github.lamba92.openaqua.app.logging.KermitSlf4jServiceProvider { *; }
