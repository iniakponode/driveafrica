import java.util.Properties
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id ("kotlin-android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.uoa.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // Load API keys from local.properties or environment variables
        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { load(it) }
            }
        }

        val chatGptKey = (localProperties["CHAT_GPT_API_KEY"] ?: System.getenv("CHAT_GPT_API_KEY")) as String? ?: ""
        val geminiKey = (localProperties["GEMINI_API_KEY"] ?: System.getenv("GEMINI_API_KEY")) as String? ?: ""

        buildConfigField("String", "CHAT_GPT_API_KEY", "\"$chatGptKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildTypes {
        release {
//            buildConfigField("String", "DRIVE_AFRICA_BASE_URL", "\"https://api.yourproductionurl.com/\"")
                buildConfigField("String", "DRIVE_AFRICA_BASE_URL", "\"https://api.safedriveafrica.com/\"")
                buildConfigField("String", "EMULATOR_BASE_URL", "\"http://10.0.2.2:8000/\"")
                buildConfigField("String", "NOMINATIM_BASE_URL", "\"https://nominatim.openstreetmap.org/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("String", "DRIVE_AFRICA_BASE_URL", "\"https://api.safedriveafrica.com/\"")
            buildConfigField("String", "EMULATOR_BASE_URL", "\"http://10.0.2.2:8000/\"")
            buildConfigField("String", "NOMINATIM_BASE_URL", "\"https://nominatim.openstreetmap.org/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { // Enables Jetpack Compose for this module
        compose = true
        buildConfig = true
    }
    // Sometimes this is also included
    packaging{
        resources.excludes.add("META-INF/LICENSE.md")
        resources.excludes.add("META-INF/LICENSE-notice.md")  // Sometimes this is also included
    }

    packaging {
        // Pick the MockMaker implementation from mockito-inline and ignore the other
        resources.pickFirsts.add("mockito-extensions/org.mockito.plugins.MockMaker")
    }
}

composeCompiler {
    enableStrongSkippingMode = true

    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

dependencies {
    implementation(libs.androidx.test.ext)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.metrics)

//    Moshi
    ksp(libs.moshi.codegen)
    implementation(libs.moshi.converter)
    implementation(libs.moshi.lib)
    implementation(libs.moshi.kotlin)

//    implementation(libs.androidx.pagination)
//    implementation(libs.androidx.ui.desktop)
    implementation(libs.navigation.compose)
    // Material icons extended for additional icon assets
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.firebase.dataconnect)
    implementation(libs.generative.ai)
    implementation(libs.pdfBox)
    implementation(libs.androidx.constraintlayout)
//    implementation(libs.compose.preview.renderer)
    ksp(libs.hilt.compiler)

//    AI Model runner
    implementation(libs.microsoft.onnx.runtime)
//    implementation(libs.ai.onnxruntime)


//    Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
//    annotationProcessor("androidx.room:room-compiler:$room_version")
    ksp(libs.room.compiler)

    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.compose.compiler)
    implementation(libs.androidx.lifecycle.lifecycle.scope)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.licycle.viewmodel.ktx)
    implementation(libs.androidx.work.ktx)
    implementation(libs.mokito.kotlin)
    implementation(libs.mokito.inline)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.gson)
    implementation(libs.retrofit.gson)



    implementation(libs.kotlinx.datetime)
    

    implementation(libs.okhttp.logging)

    implementation(libs.request.permisions)

    implementation(libs.gson)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.hilt.android)

    implementation(libs.mapper)

//    implementation(project(":core"))

//    implementation(libs.koin.android)
//    implementation(libs.koin.android.viewmodel)
    implementation(libs.play.services.location)
    implementation(libs.androidx.hilt.common)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.mokito.kotlin)
    testImplementation(libs.junit)
    testImplementation(libs.mokito.inline)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit5.engine)
    testImplementation(libs.mockito.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.architecture.testing)
   
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}