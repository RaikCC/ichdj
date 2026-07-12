plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Lokale Builds: Ausgaben aus dem OneDrive-Ordner heraushalten (OneDrive
// sperrt sonst Zwischendateien → AccessDeniedException). CI setzt die
// Variable nicht und baut normal unter app/build.
System.getenv("ICHDJ_BUILD_DIR")?.let { layout.buildDirectory.set(File(it)) }

android {
    namespace = "de.ichdj.jukebox"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.ichdj.jukebox"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.1.4"
    }

    // Sideload-Keystore: wird beim ersten CI-Lauf automatisch erzeugt und committet.
    // Er dient nur der Update-Kontinuität beim Sideloading, nicht der Geheimhaltung.
    val keystoreFile = rootProject.file("signing/ichdj-release.jks")
    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                storePassword = "ichdj-sideload"
                keyAlias = "ichdj"
                keyPassword = "ichdj-sideload"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true // für die Versionsanzeige im Veranstaltermenü
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.androidx.browser)
}
