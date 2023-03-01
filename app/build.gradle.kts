plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aliernfrog.lactoollegacy"
    compileSdk = 33
    //buildToolsVersion "30.0.3"

    defaultConfig {
        applicationId = "com.aliernfrog.lactoollegacy"
        minSdk = 18
        targetSdk = 31
        versionCode = 20
        versionName = "2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.github.aliernfrog:laclib:1.1.0")
    implementation("com.github.HBiSoft:PickiT:2.0.2")
}