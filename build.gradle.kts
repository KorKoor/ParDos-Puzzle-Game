// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // 🔥 ACTUALIZADO: Usamos Kotlin 2.1.0 para coincidir con tus metadatos y KSP
    kotlin("jvm") version "2.1.0"

    // 🔥 NUEVO: Definimos el plugin KSP aquí para que el módulo 'app' pueda usarlo
    // (Esta versión 2.1.0-1.0.29 es específica para Kotlin 2.1.0)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}