plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
//    alias(libs.plugins.darger.hilt)
    alias(libs.plugins.ksp)
    id ("kotlin-android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.uoa.safedriveafrica"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.uoa.safedriveafrica"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "1.11"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }

        ndkVersion="29.0.13599879"

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources=true
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true

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

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.common)
    implementation(libs.hilt.ext.work)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlinx.datetime)

    implementation(libs.request.permisions)
//    implementation(libs.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(project(":dbda"))
    implementation(project(":driverprofile"))
    implementation(project(":nlgengine"))
    implementation(project(":alcoholquestionnaire"))
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.ext.compiler)

    implementation(libs.coil.kt.compose)

    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.compose.compiler)
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
    implementation(libs.androidx.metrics)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.testManifest)
//    androidTestImplementation(libs.androidx.test.espresso.core)
}