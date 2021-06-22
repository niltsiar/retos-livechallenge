plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android")
}

dependencies {
    implementation(project(":shared"))
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.activity:activity-ktx:1.2.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0-alpha02")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    implementation("androidx.compose.ui:ui:1.0.0-beta09")
    implementation("androidx.compose.material:material:1.0.0-beta09")
    implementation("androidx.compose.material:material-icons-extended:1.0.0-beta09")
    implementation("androidx.compose.ui:ui-tooling:1.0.0-beta09")
    implementation("androidx.activity:activity-compose:1.3.0-beta02")
    implementation("androidx.navigation:navigation-compose:2.4.0-alpha03")

    implementation("com.google.maps.android:maps-ktx:3.1.0")
    implementation("com.google.maps.android:maps-utils-ktx:3.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0-native-mt")

    implementation("io.ktor:ktor-client-core:1.6.0")
    implementation("io.ktor:ktor-client-okhttp:1.6.0")
    implementation("io.ktor:ktor-client-serialization:1.6.0")
    implementation("io.ktor:ktor-client-logging:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation("io.github.dellisd.spatialk:geojson:0.1.1")
}

android {
    compileSdk = 30
    defaultConfig {
        applicationId = "dev.niltsiar.kmptmbtest.android"
        minSdk = 23
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
        val GOOGLE_MAPS_KEY_KMP_TMB_TEST: String by project
        manifestPlaceholders["googleMapsKey"] = GOOGLE_MAPS_KEY_KMP_TMB_TEST
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.0.0-beta09"
        kotlinCompilerVersion = "1.5.10"
    }
}
