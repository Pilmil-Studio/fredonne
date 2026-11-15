import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:music"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.materialIconsExtended)
            api(compose.ui)
        }
        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.13.1")
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
            implementation("androidx.camera:camera-core:1.4.1")
            implementation("androidx.camera:camera-camera2:1.4.1")
            implementation("androidx.camera:camera-lifecycle:1.4.1")
            implementation("androidx.camera:camera-view:1.4.1")
            implementation("com.google.zxing:core:3.5.3")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "fr.fredonne.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
