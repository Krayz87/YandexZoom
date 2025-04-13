plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "sh.naeba.yandexzoom"
    compileSdk = 35

    defaultConfig {
        applicationId = "sh.naeba.yandexzoom"
        minSdk = 28
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    signingConfigs {
        create("myDebug") {  // Используем другое имя
            storeFile = file("C:\\AndroidTools\\platform.jks")
            storePassword = "platform"
            keyAlias = "platform"
            keyPassword = "platform"
            storeType = "JKS"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("myDebug")  // Используем нашу конфигурацию
            isDebuggable = true
        }
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
}