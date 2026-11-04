# kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.list_it.app.**$$serializer { *; }
-keepclassmembers class com.list_it.app.** { *** Companion; }
-keepclasseswithmembers class com.list_it.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor / Supabase optional JVM-only references
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**
