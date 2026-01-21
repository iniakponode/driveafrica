import java.util.Properties
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id ("kotlin-android")
    alias(libs.plugins.ksp)
//    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.secrets)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.uoa.nlgengine"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "com.uoa.nlgengine.CustomTestRunner"
        consumerProguardFiles("consumer-rules.pro")

        // Load API keys from local.properties or environment variables
        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { load(it) }
            }
        }

        val chatGptKey = (localProperties["CHAT_GPT_KEY"] ?: System.getenv("CHAT_GPT_KEY")) as String? ?: ""
        val geminiKey = (localProperties["GEMINI_API_KEY"] ?: System.getenv("GEMINI_API_KEY")) as String? ?: ""

        buildConfigField("String", "CHAT_GPT_KEY", "\"$chatGptKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    packaging {
        // Pick the MockMaker implementation from mockito-inline and ignore the other
        resources.pickFirsts.add("mockito-extensions/org.mockito.plugins.MockMaker")
    }
}

composeCompiler {
    featureFlags.set(setOf(ComposeFeatureFlag.StrongSkipping))

    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

secrets {
    // Fall back to repo-shipped defaults so CI and clean environments don't fail
    // when a developer has not yet created a personal local.properties file.
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.test.ext)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.metrics)
//    implementation(libs.androidx.ui.desktop)
    implementation(libs.androidx.runtime.livedata)

    implementation(libs.androidx.compose.material.iconsExtended)


    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.compose.compiler)
    implementation(libs.androidx.lifecycle.lifecycle.scope)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.licycle.viewmodel.ktx)
    implementation(libs.map.osmdroid)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.gson)
    implementation(libs.retrofit.gson)

    implementation(libs.kotlinx.datetime)


    implementation(libs.okhttp.logging)

    implementation(libs.request.permisions)

    implementation(libs.play.services.location)

    implementation(libs.gson)

//    implementation(libs.anychart)

    implementation(libs.androidx.hilt.common)
    implementation(libs.hilt.ext.work)
    implementation(project(":core"))
    implementation(libs.generative.ai)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(project(":dbda"))
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.hilt.android)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.room.runtime)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.testManifest)
}
