plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.korkoor.pardos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.korkoor.pardos"
        minSdk = 24
        targetSdk = 36 // Alineado con compileSdk
        versionCode = 6
        versionName = "2.0" // Sugerencia: Usa nomenclatura estándar (ej: 2.0)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    configurations.all {
        exclude(group = "com.google.ar") // Bloquea todo lo que venga de Google AR
        exclude(group = "com.google.ar.sceneform") // Bloquea Sceneform (motores 3D viejos)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        // ✅ MEJORA: Android Studio moderno y KSP funcionan mejor con Java 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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

    // Anuncios
    implementation(libs.play.services.ads.api)

    // Room Database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // 🔥 KSP ACTIVADO
    ksp("androidx.room:room-compiler:$room_version")

    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
configurations.all {
    exclude(group = "com.google.ar")
    exclude(group = "com.google.ar.sceneform")
    exclude(group = "com.google.ar.sceneform.ux")
}