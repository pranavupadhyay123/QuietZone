import org.apache.tools.ant.util.JavaEnvUtils.VERSION_1_8

plugins {
    alias(libs.plugins.androidApplication)
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("C:\\Users\\PRANAV\\AndroidStudioProjects\\QuietZone\\KeyStore.jks")
            storePassword = "pRANAv#8909"
            keyAlias = "key0"
            keyPassword = "pRANAv#8909"
        }
        create("release") {
            storeFile = file("C:\\Users\\PRANAV\\AndroidStudioProjects\\QuietZone\\KeyStore.jks")
            storePassword = "pRANAv#8909"
            keyPassword = "pRANAv#8909"
            keyAlias = "key0"
        }
    }
    namespace = "co.pranavlabs.quietzone"
    compileSdk = 34

    defaultConfig {
        applicationId = "co.pranavlabs.quietzone"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("release")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            isJniDebuggable = false
            isDebuggable = false
            renderscriptOptimLevel = 3
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    buildToolsVersion = "34.0.0"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation (libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.activity)
    implementation(libs.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.maps)
}
