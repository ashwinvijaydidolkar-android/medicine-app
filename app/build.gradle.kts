plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ragini.medreminder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ragini.medreminder"
        minSdk = 23          // Android 6.0 and every version after it
        targetSdk = 34
        // Every GitHub build gets a higher number than the one before, so it always installs as an
        // update over the previous version (same app, same signing key – medicines and history are kept).
        val run = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
        versionCode = if (run != null) 100 + run else 2
        versionName = if (run != null) "1.1.$run" else "1.1"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // A fixed key kept in the project, so every new version installs over the old one
        // without losing medicines or history. Keep this repository private.
        create("family") {
            storeFile = rootProject.file("keystore/ragini-release.jks")
            storePassword = "ragini-family"
            keyAlias = "ragini"
            keyPassword = "ragini-family"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("family")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("family")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.core:core-ktx:1.13.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
