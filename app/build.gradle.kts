import java.util.Properties
import java.io.File
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.autoroid.app"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.autoroid.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 17
        versionName = "1.2.16"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) {
            FileInputStream(keystorePropsFile).use { load(it) }
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("KEYSTORE_FILE")
                ?: keystoreProps.getProperty("storeFile")
                ?: "${rootDir}/autoroid-release.jks"
            val targetStoreFile = if (File(storeFilePath).isAbsolute) File(storeFilePath) else file(storeFilePath)
            val storePass = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProps.getProperty("storePassword")
            val kAlias = System.getenv("KEY_ALIAS")
                ?: keystoreProps.getProperty("keyAlias")
                ?: "key"
            val kPass = System.getenv("KEY_PASSWORD")
                ?: keystoreProps.getProperty("keyPassword")
                ?: storePass

            if (!storePass.isNullOrBlank() && targetStoreFile.exists()) {
                storeFile = targetStoreFile
                storePassword = storePass
                keyAlias = kAlias
                keyPassword = kPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Shizuku API & Hidden API bypass
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    compileOnly(project(":stub"))

    debugImplementation(libs.androidx.ui.tooling)
}

tasks.register<Copy>("copyChangelogToAssets") {
    from("${rootDir}/CHANGELOG.md")
    into("${projectDir}/src/main/assets")
}

tasks.named("preBuild") {
    dependsOn("copyChangelogToAssets")
}
