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
    namespace = "com.uoa.ml"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true

    }
    composeOptions {
        kotlinCompilerExtensionVersion="1.5.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.test.ext)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.metrics)
//    implementation(libs.androidx.ui.desktop)
    implementation(libs.androidx.runtime.livedata)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.compose.compiler)
    implementation(libs.androidx.lifecycle.lifecycle.scope)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.licycle.viewmodel.ktx)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.gson)
    implementation(libs.retrofit.gson)

    implementation(libs.kotlinx.datetime)


    implementation(libs.okhttp.logging)

    implementation(libs.request.permisions)

    implementation(libs.play.services.location)

    implementation(libs.gson)

    implementation(libs.androidx.hilt.common)
    implementation(libs.hilt.ext.work)
    implementation(project(":core"))
    implementation(libs.generative.ai)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.hilt.android)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}