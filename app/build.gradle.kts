plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
//    alias(libs.plugins.darger.hilt)
    alias(libs.plugins.ksp)
    id ("kotlin-android")
}

android {
    namespace = "com.uoa.driveafrica"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.uoa.driveafrica"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = true

    }
}

dependencies {

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.common)
    implementation(libs.hilt.ext.work)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.compose.ui)

    implementation(libs.request.permisions)
//    implementation(libs.compose.material)
    implementation(libs.androidx.compose.material3)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.ext.compiler)

    implementation(libs.coil.kt.compose)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.compiler)
    implementation(libs.androidx.lifecycle.lifecycle.scope)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.licycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(project(":core"))
    implementation(project(":sensor"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.work.ktx)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.kotlin.stdlib)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}