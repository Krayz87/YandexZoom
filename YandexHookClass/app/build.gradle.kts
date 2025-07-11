plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "sh.naeba.yandexhookclass"
    compileSdk = 35

    defaultConfig {
        applicationId = "sh.naeba.yandexhookclass"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false;
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core)
    implementation("top.canyie.pine:core:0.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    compileOnly("com.yandex.android:maps.mobile:4.14.0-navikit")
    compileOnly("com.yandex.mapkit.styling:roadevents:4.14.0")
    compileOnly("com.yandex.mapkit.styling:automotivenavigation:4.14.0")
}