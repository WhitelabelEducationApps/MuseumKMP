import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.museum.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.museum.android"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }

        val mapsApiKey = properties.getProperty("maps.apiKey") ?: ""
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        resValue("string", "maps_api_key", mapsApiKey)
    }

    buildTypes {
        release {
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.androidx.palette.ktx)
}
