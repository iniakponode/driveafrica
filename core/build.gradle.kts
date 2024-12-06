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

        // Get the API key from local.properties
        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { load(it) }
            }
        }
        buildConfigField("String", "CHAT_GPT_API_KEY", "\"${localProperties["CHAT_GPT_API_KEY"]}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties["GEMINI_API_KEY"]}\"")
    }

    buildTypes {
        release {
//            buildConfigField("String", "DRIVE_AFRICA_BASE_URL", "\"https://api.yourproductionurl.com/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("String", "DRIVE_AFRICA_BASE_URL", "\"http://localhost:8000/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures { // Enables Jetpack Compose for this module
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion="1.5.8"
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
//    implementation(libs.androidx.pagination)
//    implementation(libs.androidx.ui.desktop)
    implementation(libs.navigation.compose)
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