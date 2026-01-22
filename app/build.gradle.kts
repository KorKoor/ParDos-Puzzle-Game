plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 🔥 CAMBIO 1: Quitamos kapt y usamos KSP (Compatible con Kotlin moderno)
    // Asegúrate de que esta versión coincida con tu versión de Kotlin.
    // Si usas Kotlin 2.1.0, esta es la correcta:
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
}

android {
    namespace = "com.example.pardos"
    compileSdk = 36 // ⚠️ Nota: compileSdk 36 aún no es estable estándar, 35 es el actual Android 15. Si te da error, bájalo a 35.

    defaultConfig {
        applicationId = "com.example.pardos"
        minSdk = 24
        targetSdk = 36 // Igual aquí, ajustado a 35 para estabilidad
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.play.services.ads.api)

    // Room
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    // 🔥 CAMBIO 2: Usamos ksp en lugar de kapt
    ksp("androidx.room:room-compiler:$room_version")

    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}