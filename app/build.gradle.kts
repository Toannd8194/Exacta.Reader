import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.exacta.reader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.exacta.reader"
        minSdk = 24
        targetSdk = 34
        versionCode = 220
        versionName = "2.2"
    }

    // The app's own key, kept with the project (keystore/): every later APK must carry the same
    // signature to install over the last one without losing the reader's data.
    val keyProps = Properties().apply {
        rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    signingConfigs {
        create("exacta") {
            storeFile = rootProject.file(keyProps.getProperty("storeFile", "keystore/exacta-reader.jks"))
            storePassword = keyProps.getProperty("storePassword")
            keyAlias = keyProps.getProperty("keyAlias")
            keyPassword = keyProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Without the key (a fresh clone) the release is signed with the debug key.
            val own = rootProject.file(keyProps.getProperty("storeFile", "keystore/exacta-reader.jks")).exists()
            signingConfig = signingConfigs.getByName(if (own) "exacta" else "debug")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true  // required by Readium
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = false
        buildConfig = true
    }
    // Screens are drawn on the desktop at the Neo 3's size (Robolectric) to check the layouts.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    val readium = "3.0.3"
    implementation("org.readium.kotlin-toolkit:readium-shared:$readium")
    implementation("org.readium.kotlin-toolkit:readium-streamer:$readium")
    implementation("org.readium.kotlin-toolkit:readium-navigator:$readium")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
