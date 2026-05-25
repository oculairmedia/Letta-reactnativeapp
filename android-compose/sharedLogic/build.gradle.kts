plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlinx.kover")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/detekt.yml")
    parallel = true
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
        )
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            }
        }
    }
}
