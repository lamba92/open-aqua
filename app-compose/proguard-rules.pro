# Preserve stacktraces and debugging metadata
-dontobfuscate
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Ignore warnings from internal dynamic reflection in dependencies
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-dontwarn com.typesafe.config.**
-dontwarn org.slf4j.**
-dontwarn org.fusesource.jansi.**
-dontwarn org.apache.logging.**
-dontwarn org.json.**

# Suppress informational notes from dependencies and serializers
-dontnote

# Kermit & Coroutines reflection
-keep class co.touchlab.kermit.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Compose resources and serialization
-keep class org.jetbrains.compose.resources.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }
