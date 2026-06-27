# Consumer ProGuard rules for asqio-sdk
# These rules are applied to any app that consumes this library.

# Preserve kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class io.asqio.sdk.**$$serializer { *; }
-keepclassmembers class io.asqio.sdk.** {
    *** Companion;
}
-keepclasseswithmembers class io.asqio.sdk.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserve public SDK API surface
-keep public class io.asqio.sdk.AsqioSupport { *; }
-keep public class io.asqio.sdk.AsqioConfiguration { *; }
-keep public class io.asqio.sdk.error.** { *; }
-keep public class io.asqio.sdk.model.** { *; }
