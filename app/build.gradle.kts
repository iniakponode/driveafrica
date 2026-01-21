import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import javax.inject.Inject

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
    ndkVersion = "29.0.13599879"

    defaultConfig {
        applicationId = "com.uoa.safedriveafrica"
        minSdk = 29
        targetSdk = 34
        versionCode = 12
        versionName = "1.12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    val isReleaseTask = gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("Release", ignoreCase = true)
    }

    // Signing configuration
    signingConfigs {
        create("release") {
            // Load keystore properties from keystore.properties file if it exists
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (!keystorePropertiesFile.exists()) {
                if (isReleaseTask) {
                    throw GradleException("Missing keystore.properties for release signing.")
                } else {
                    logger.warn("keystore.properties not found; release builds will fail.")
                }
            } else {
                val keystoreProperties = Properties()
                FileInputStream(keystorePropertiesFile).use { stream ->
                    keystoreProperties.load(stream)
                }

                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Enable minification and shrinking using the non-optimized ProGuard config
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
    featureFlags.set(setOf(ComposeFeatureFlag.StrongSkipping))

    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

val bundletool by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {

    add(bundletool.name, "com.android.tools.build:bundletool:1.18.1")

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.common)
    implementation(libs.hilt.ext.work)
    implementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.request.permisions)
//    implementation(libs.compose.material)
    implementation(libs.androidx.compose.material3)
    implementation(project(":dbda"))
    implementation(project(":driverprofile"))
    implementation(project(":ml"))
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
    implementation(libs.androidx.hilt.navigation.compose)
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
    androidTestImplementation(project(":core"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.testManifest)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    kspAndroidTest(libs.hilt.ext.compiler)
//    androidTestImplementation(libs.androidx.test.espresso.core)
}

abstract class Check16kAlignment @Inject constructor() : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aabFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localPropertiesFile: RegularFileProperty

    @get:Input
    abstract val sdkDirFallback: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bundletoolClasspath: ConfigurableFileCollection

    @TaskAction
    fun run() {
        val aabFile = aabFile.get().asFile
        val apkFile = apkFile.get().asFile

        if (!aabFile.exists()) {
            error("Missing AAB at ${aabFile.absolutePath}. Run :app:bundleRelease first.")
        }
        if (!apkFile.exists()) {
            error("Missing APK at ${apkFile.absolutePath}. Run :app:assembleRelease first.")
        }

        val localProps = Properties().apply {
            val propsFile = localPropertiesFile.orNull?.asFile
            if (propsFile != null && propsFile.exists()) {
                propsFile.inputStream().use { load(it) }
            }
        }

        val sdkDirPath = localProps.getProperty("sdk.dir")
            ?: sdkDirFallback.get().takeIf { it.isNotBlank() }
            ?: error("Set sdk.dir in local.properties or ANDROID_SDK_ROOT/ANDROID_HOME.")
        val sdkDir = File(sdkDirPath)
        val buildToolsDir = File(sdkDir, "build-tools")
        val buildToolsVersionDir = buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }
            ?: error("No build-tools found under ${buildToolsDir.absolutePath}.")
        val zipalignName = if (System.getProperty("os.name").lowercase().contains("win")) {
            "zipalign.exe"
        } else {
            "zipalign"
        }
        val zipalign = File(buildToolsVersionDir, zipalignName)
        if (!zipalign.exists()) {
            error("zipalign not found at ${zipalign.absolutePath}.")
        }

        val bundletoolClasspath = bundletoolClasspath.files
            .joinToString(File.pathSeparator) { it.absolutePath }
        if (bundletoolClasspath.isBlank()) {
            error("bundletool classpath could not be resolved.")
        }

        val bundletoolOutput = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(
                "java",
                "-cp",
                bundletoolClasspath,
                "com.android.tools.build.bundletool.BundleToolMain",
                "dump",
                "config",
                "--bundle=${aabFile.absolutePath}"
            )
            standardOutput = bundletoolOutput
            errorOutput = bundletoolOutput
        }
        val alignmentLines = bundletoolOutput.toString()
            .lineSequence()
            .filter { it.contains("alignment", ignoreCase = true) }
            .toList()
        if (alignmentLines.isEmpty()) {
            logger.lifecycle(bundletoolOutput.toString().trim())
        } else {
            alignmentLines.forEach { logger.lifecycle(it) }
        }

        execOperations.exec {
            commandLine(
                zipalign.absolutePath,
                "-c",
                "-P",
                "16",
                "4",
                apkFile.absolutePath
            )
        }
    }
}

tasks.register<Check16kAlignment>("check16kAlignment") {
    group = "verification"
    description = "Checks AAB/APK zip alignment for 16 KB page sizes."
    dependsOn("bundleRelease", "assembleRelease")
    aabFile.set(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
    apkFile.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    localPropertiesFile.set(rootProject.layout.projectDirectory.file("local.properties"))
    sdkDirFallback.set(
        System.getenv("ANDROID_SDK_ROOT") ?: System.getenv("ANDROID_HOME") ?: ""
    )
    bundletoolClasspath.from(bundletool)
}
