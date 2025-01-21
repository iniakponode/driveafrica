// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.secrets) apply false
//    alias(libs.plugins.room) apply false
    alias(libs.plugins.hilt) apply false
//    alias(libs.plugins.darger.hilt) apply false
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler) apply false
}
allprojects {
//    repositories {
//        google()
//        mavenCentral()
//        maven { url "https://jitpack.io" }
//    }

    gradle.projectsEvaluated {
        tasks.withType(JavaCompile::class.java) {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-Xlint:deprecation")
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "11"
            }
        }

    }
}

