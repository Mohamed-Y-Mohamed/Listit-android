# kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.list_it.app.**$$serializer { *; }
-keepclassmembers class com.list_it.app.** { *** Companion; }
-keepclasseswithmembers class com.list_it.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor / Supabase optional JVM-only references
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

# ML Kit GenAI (Gemini Nano via AICore). Its own consumer rules are minimal; if R8 strips or
# renames classes here, OnDeviceModel.isAvailable() fails and every AI entry point is hidden.
-keep class com.google.mlkit.genai.** { *; }
-keep class com.google.android.gms.internal.mlkit_genai_** { *; }
