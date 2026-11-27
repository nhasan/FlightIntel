import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application") version "9.5.3"
    id("com.google.gms.google-services") version "4.5.0"
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencies {
    implementation (fileTree(mapOf("include" to "*.jar", "dir" to "libs")))
    implementation (libs.androidx.preference.ktx)
    implementation (libs.androidx.activity.ktx)
    implementation (libs.androidx.fragment.ktx)
    implementation (libs.androidx.viewpager2)
    implementation (libs.material)
    implementation (libs.androidx.swiperefreshlayout)
    implementation (libs.androidx.coordinatorlayout)
    implementation (libs.androidx.drawerlayout)
    implementation (libs.play.services.location)
    implementation (libs.firebase.messaging.ktx)
    implementation (libs.androidx.core.ktx)
    implementation (libs.androidx.work.runtime.ktx)
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)
    implementation (libs.kotlinx.serialization.json)
    implementation (libs.gson)
    implementation (libs.osmdroid.android)
    coreLibraryDesugaring (libs.desugar.jdk.libs)
    testImplementation (libs.junit)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    namespace = "com.nadmm.airports"

    defaultConfig {
        applicationId = "com.nadmm.airports"
        minSdk = 30
        targetSdk = 37
        compileSdk {
            version = release(37) {
                minorApiLevel = 2
            }
        }
        versionName = "26.12.02"
        versionCode = 261202
        buildToolsVersion = "37.0.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        getByName("debug") {
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    buildFeatures {
        viewBinding = true
        resValues = true
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        // Sets Java compatibility to Java 25
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}

kotlin {
    jvmToolchain(25)
}

// In AGP 9.0 applicationVariants is removed. A simpler way to rename outputs is to change the archivesName
base.archivesName.set("flightintel-${android.defaultConfig.versionName}")
