plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "co.pranavlabs.quietzone"
    compileSdk = 34

    defaultConfig {
        applicationId = "co.pranavlabs.quietzone"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            isDebuggable = false
            renderscriptOptimLevel = 3
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
    implementation (libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.maps)
}
