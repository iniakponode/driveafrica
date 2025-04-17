plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id ("kotlin-android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.uoa.sensor"
    compileSdk = 34

    defaultConfig {
//        applicationId = "com.uoa.sensor"
        minSdk = 26

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures { // Enables Jetpack Compose for this module
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion="1.5.8"
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


    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.compose.compiler)
    implementation(libs.androidx.lifecycle.lifecycle.scope)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.licycle.viewmodel.ktx)
    implementation(libs.androidx.work.ktx)

//    implementation(libs.play.services.activity)

    implementation(libs.map.osmdroid)

    implementation(libs.guava)

    implementation(libs.request.permisions)

    implementation(libs.play.services.location)

    implementation(libs.gson)
    
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.common)
    implementation(libs.hilt.ext.work)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.navigation.compose)
    implementation(project(":ml"))
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.room.runtime)
    implementation(libs.firebase.dataconnect)
    implementation(project(":driverprofile"))
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

    testImplementation(libs.mokito.kotlin)
    testImplementation(libs.mokito.inline)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit5.engine)
    testImplementation(libs.mockito.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.architecture.testing)
    testImplementation(libs.junit)

    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}