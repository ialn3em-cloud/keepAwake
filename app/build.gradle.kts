plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing comes from GitHub Secrets (see README.txt).
// Every build must use the same key, otherwise phones refuse to update over the old version.
val keystoreFile = System.getenv("KEYSTORE_FILE")
val hasReleaseKey = !keystoreFile.isNullOrBlank() && file(keystoreFile).exists()

android {
    namespace = "com.awake.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.awake.app"
        minSdk = 26
        targetSdk = 34
        // grows with every GitHub build so each new APK installs over the previous one
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
