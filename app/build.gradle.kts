plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Optional release signing. The keystore is never committed: CI decodes it from a repository secret
// into a temporary file and passes its location and passwords through these environment variables.
val releaseStorePath: String? = System.getenv("RELEASE_KEYSTORE_PATH")
val hasReleaseSigning = !releaseStorePath.isNullOrBlank() && file(releaseStorePath).exists()

android {
    namespace = "com.rememberflow.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rememberflow.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("RF_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("RF_VERSION_NAME") ?: "1.0.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // With real release secrets the APK is signed with the owner's key (stable across builds, so it
            // updates in place). Without them it falls back to the auto-generated debug key: still
            // installable, but each CI run signs with a different key, so installing a newer build over an
            // older one requires uninstalling first.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    testImplementation(libs.junit)
}
