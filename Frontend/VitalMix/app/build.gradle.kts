plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.vitalmix"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.vitalmix"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.retrofit) // Retrofit
    implementation(libs.converter.gson) // Gson converter
    implementation(libs.okhttp) // OkHttp core library
    implementation(libs.okhttp.logging) // Logging Interceptor
    implementation(libs.mpandroidchart) // For Charts
}