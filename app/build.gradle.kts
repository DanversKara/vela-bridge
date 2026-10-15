plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "app.vela.bridge"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // Two install variants of the same code:
    //  - normal:    package app.vela.bridge — appears as its own entry wherever
    //               the system/Uber lists navigation handlers. No adb needed.
    //  - mapsSpoof: package com.google.android.apps.maps — makes Uber's hardcoded
    //               "is Google Maps installed?" check pass. Requires Maps to be
    //               fully uninstalled first (see README); only works if Maps is a
    //               *user* app, not a system app (signature check).
    flavorDimensions += "mode"
    productFlavors {
        create("normal") {
            dimension = "mode"
            applicationId = "app.vela.bridge"
        }
        create("mapsSpoof") {
            dimension = "mode"
            applicationId = "com.google.android.apps.maps"
        }
        create("wazeSpoof") {
            dimension = "mode"
            applicationId = "com.waze"
            // Honest launcher label — Uber shows its own "Waze" label.
            resValue("string", "app_name", "Vela Bridge (Waze)")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// No dependencies — pure Android framework. Nothing to download besides the
// Android Gradle Plugin itself.
