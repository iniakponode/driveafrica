plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id ("kotlin-android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.uoa.sensor"
    compileSdk = 34

    defaultConfig {
//        applicationId = "com.uoa.sensor"
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
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

    buildFeatures { // Enables Jetpack Compose for this module
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion="1.5.8"
    }

}

dependencies {


    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.compiler)
    implementation(libs.androidx.lifecycle.lifecycle.scope)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.licycle.viewmodel.ktx)
    implementation(libs.androidx.work.ktx)

    implementation(libs.request.permisions)

    implementation(libs.play.services.location)

    implementation(libs.gson)
    
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.common)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.hilt.android)

    implementation(libs.mapper)

    implementation(project(":core"))

//    implementation(libs.koin.android)
//    implementation(libs.koin.android.viewmodel)
    implementation(libs.play.services.location)
    implementation(libs.androidx.hilt.common)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}