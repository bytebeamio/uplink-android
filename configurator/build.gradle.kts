import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
}

val localProps = Properties()
localProps.load(project.rootProject.file("local.properties").inputStream())

android {
    compileSdk = 32
    ndkVersion = "25.0.8775105"

    defaultConfig {
        applicationId = "io.bytebeam.uplink.configurator"
        minSdk = 23
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        release {
            if (localProps.getProperty("RELEASE_STORE_FILE") != null) {
                storeFile = file("../" + localProps.getProperty("RELEASE_STORE_FILE"))
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            minifyEnabled = false

            if (localProps.getProperty("RELEASE_STORE_FILE") != null) {
                signingConfig = signingConfigs.release
            }

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
