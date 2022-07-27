plugins {
    id "com.android.application"
    id "kotlin-android"
}

Properties localProps = new Properties()
localProps.load(project.rootProject.file("local.properties").newDataInputStream())

android {
    compileSdk 32
    ndkVersion "25.0.8775105"

    defaultConfig {
        applicationId "io.bytebeam.uplink.configurator"
        minSdk 23
        targetSdk 32
        versionCode 1
        versionName sdk_version

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        release {
            if (localProps.getProperty("RELEASE_STORE_FILE") != null) {
                storeFile file("../" + localProps.getProperty("RELEASE_STORE_FILE"))
                storePassword localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            minifyEnabled false

            if (localProps.getProperty("RELEASE_STORE_FILE") != null) {
                signingConfig signingConfigs.release
            }

            proguardFiles getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation "androidx.core:core-ktx:1.8.0"
    implementation "androidx.appcompat:appcompat:1.4.2"
    implementation "com.google.android.material:material:1.6.1"
    implementation "androidx.constraintlayout:constraintlayout:2.1.4"

    implementation project(path: ":lib")
}

def rustBasePath = localProps.getProperty("uplink.dir")
def archTriplets = [
//        "armeabi-v7a": "armv7-linux-androideabi",
//        "arm64-v8a"  : "aarch64-linux-android",
"x86"        : "i686-linux-android",
//        "x86_64"     : "x86_64-linux-android",
]

tasks.create(name: "checkout-repo", description: "checkout android_exe branch in uplink") {
    doLast {
    }
}

archTriplets.each { arch, target ->

    // Build with cargo
    tasks.create(name: "cargo-build-${arch}", type: Exec, description: "Building core for ${arch}", dependsOn: "checkout-repo") {
        inputs.files "${rustBasePath}/Cargo.toml", "${rustBasePath}/Cargo.lock", "${rustBasePath}/uplink"
        outputs.files "${rustBasePath}/target/${target}/debug/uplink"

        workingDir rustBasePath
        environment ANDROID_NDK_HOME: android.ndkDirectory
        commandLine "cargo", "ndk", "--target=${target}", "--platform=23", "build", "--bin", "uplink"
    }

    // Copy exe into this module"s assets directory
    tasks.create(name: "rust-deploy-${arch}", type: Copy, dependsOn: "sync-rust-deps-${arch}", description: "Copy rust libs for (${arch}) to jniLibs") {
        from "${rustBasePath}/target/${target}/debug"
        include "uplink"
        into "src/main/res/executables/${arch}"
    }

    // Hook up tasks to execute before building java
    tasks.withType(JavaCompile) {
        compileTask -> compileTask.dependsOn "rust-deploy-${arch}"
    }
    preBuild.dependsOn "rust-deploy-${arch}"

    // Hook up clean tasks
    tasks.create(name: "clean-${arch}", type: Delete, description: "Deleting built libs for ${arch}") {
        delete fileTree("src/main/jniLibs/${arch}") {
            include "*.so"
        }
    }

    clean.dependsOn "clean-${arch}"
}

tasks.whenTaskAdded { task ->
    if ((task.name == "javaPreCompileDebug" || task.name == "javaPreCompileRelease")) {
        task.dependsOn "cargoBuild"
    }
}