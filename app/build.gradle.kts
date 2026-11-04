import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing: create keystore.properties (see README). Not committed to source control.
val keystoreFile = rootProject.file("keystore.properties")

// The Android Studio signing wizard injects its own keystore instead of using keystore.properties.
val hasInjectedSigning = providers.gradleProperty("android.injected.signing.store.file").isPresent

android {
    namespace = "com.list_it.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.list_it.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "1.4.0"

        buildConfigField("String", "SUPABASE_URL", "\"${property("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${property("SUPABASE_KEY")}\"")
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            val keys = Properties().apply { keystoreFile.inputStream().use(::load) }
            create("release") {
                storeFile = rootProject.file(keys.getProperty("storeFile"))
                storePassword = keys.getProperty("storePassword")
                keyAlias = keys.getProperty("keyAlias")
                keyPassword = keys.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time is only in the platform from API 26; the app ships to API 24.
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("roborazzi.test.record", "true")
            it.maxHeapSize = "3g"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Without a signing config, release packaging quietly succeeds and produces an unsigned artifact
// that Play rejects on upload. Fail at the point of packaging instead of shipping the surprise.
if (!keystoreFile.exists() && !hasInjectedSigning) {
    tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
        doFirst {
            throw GradleException(
                "No release signing config. Copy keystore.properties.example to keystore.properties " +
                    "and point storeFile at the upload keystore (see README). " +
                    "Unsigned release artifacts cannot be installed or uploaded to Play."
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.markdown.m3)
    implementation(libs.lottie.compose)
    implementation(libs.mlkit.genai.prompt)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.compose.ui.test.manifest)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
