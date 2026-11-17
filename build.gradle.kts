plugins {
    id("com.android.application") version "8.10.1" apply false
    id("com.android.library") version "8.10.1" apply false
    kotlin("jvm") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.0.21" apply false
    kotlin("plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("app.cash.paparazzi") version "1.3.5" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}

dependencies {
    kover(project(":core:music"))
    kover(project(":shared"))
    kover(project(":app"))
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        parallel = true
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        setSource(files("src/main/kotlin", "src/commonMain/kotlin", "src/androidMain/kotlin", "src/iosMain/kotlin"))
        include("**/*.kt")
        exclude("**/build/**")
        reports {
            html.required.set(true)
            xml.required.set(false)
            txt.required.set(true)
        }
    }
}
