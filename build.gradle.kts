import java.util.Properties
import java.io.FileInputStream
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

buildscript {
    dependencies {
        classpath ("com.android.tools.build:gradle:8.13.0")
        classpath ("com.google.gms:google-services:4.4.3")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    }
}

plugins {
    id ("com.android.application") version "8.13.0"
    id ("org.jetbrains.kotlin.android") version "2.2.20"
    id ("com.google.gms.google-services") version "4.4.3"
    kotlin ("plugin.parcelize") version "2.2.0"
    kotlin ("plugin.serialization") version "2.2.10"
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
    implementation ("androidx.preference:preference-ktx:1.2.1")
    implementation ("androidx.activity:activity-ktx:1.11.0")
    implementation ("androidx.fragment:fragment-ktx:1.8.9")
    implementation ("androidx.viewpager2:viewpager2:1.1.0")
    implementation ("com.google.android.material:material:1.13.0")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation ("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation ("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation ("com.google.firebase:firebase-messaging-ktx:24.1.2")
    implementation ("androidx.core:core-ktx:1.17.0")
    implementation ("androidx.work:work-runtime-ktx:2.10.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation ("com.google.code.gson:gson:2.13.2")
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:2.1.5")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    namespace = "com.nadmm.airports"

    defaultConfig {
        applicationId = "com.nadmm.airports"
        minSdk = 29
        targetSdk = 36
        compileSdk = 36
        versionName = "25.09.03"
        versionCode = 250903
        buildToolsVersion = "36.1.0"
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
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as BaseVariantOutputImpl
            output.outputFileName = "flightintel-${variant.name}-${variant.versionName}.apk"
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        // Sets Java compatibility to Java 21
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }
}
