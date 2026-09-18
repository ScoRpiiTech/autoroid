plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.autoroid.stub"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        aidl = true
    }
}
