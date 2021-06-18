plugins {
    id("com.android.application")
    kotlin("android")
}

dependencies {
    implementation(project(":shared"))
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.activity:activity-ktx:1.2.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0-alpha02")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0-native-mt")

    implementation("io.ktor:ktor-client-core:1.6.0")
    implementation("io.ktor:ktor-client-okhttp:1.6.0")
    implementation("io.ktor:ktor-client-serialization:1.6.0")
    implementation("io.ktor:ktor-client-logging:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
}

android {
    compileSdk = 30
    defaultConfig {
        applicationId = "dev.niltsiar.kmptmbtest.android"
        minSdk = 23
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}
