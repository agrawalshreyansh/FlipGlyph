plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.flipglyph"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.flipglyph"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-mvp"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Only the physical Phone (4a) Pro is supported, and it's arm64 — drop the other
        // ABI slices of every native lib pulled in transitively (Compose, DataStore, etc).
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        debug {
            // Debuggable + minify together silently disable R8's actual optimization pass —
            // there's no size win here, just a slower build. Keep debug fast; assembleRelease
            // (signed with the debug keystore below, so it's still directly sideloadable) is
            // the build that's actually shrunk.
            isMinifyEnabled = false
            manifestPlaceholders["nothingApiKey"] = "test"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Not distributed via Play Store — reuse the debug keystore so this stays a
            // plain sideloadable APK instead of requiring a dedicated release signing setup.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["nothingApiKey"] = ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Real Nothing Glyph Matrix Developer Kit AAR (vendor-distributed).
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
