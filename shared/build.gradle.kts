import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }

    // iOS: la lógica del juego (motor, niveles, reglas) se entrega a la app SwiftUI como "Shared.xcframework".
    // Es estático para que quede dentro del ejecutable y la app no lleve frameworks sueltos que firmar.
    val xcframework = XCFramework("Shared")
    iosArm64 {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcframework.add(this)
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcframework.add(this)
        }
    }
    iosX64()

    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

android {
    namespace = "com.korkoor.pardos.shared"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
