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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ialn3em.screenswitch"
        minSdk = 26
        targetSdk = 36
        // grows with every GitHub build so each new APK installs over the previous one
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
        // true only in the test build, so Pro can be tried before the app is on Google Play
        buildConfigField("boolean", "PRO_ALWAYS", "false")
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

    buildFeatures { buildConfig = true }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
        }
        // a separate app (own id and name) with Pro unlocked, for testing only; never upload it
        create("unlocked") {
            initWith(getByName("release"))
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            buildConfigField("boolean", "PRO_ALWAYS", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Google Play in-app purchase for the Pro upgrade
    implementation("com.android.billingclient:billing:8.0.0")
}
