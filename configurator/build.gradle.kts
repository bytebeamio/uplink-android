import org.eclipse.jgit.api.Git
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
        create("release") {
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
            isMinifyEnabled = false

            if (localProps.getProperty("RELEASE_STORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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

dependencies {
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation(project(":lib"))
}

val uplinkRepo = project.rootProject.rootDir.toPath().resolve(localProps.getProperty("uplink.dir")).toAbsolutePath()

tasks.create("checkoutUplink") {
    doLast {
        val repo = Git.open(File(uplinkRepo.toString()))
        val status = repo.status().call()
        if (status.hasUncommittedChanges()) {
            throw IllegalStateException("uplink repository has uncommitted changes, aborting")
        }
        repo.checkout()
            .setName("android_exe")
            .call()
    }
}

data class ArchConfig(
    val rustTriplet: String,
    val androidId: String,
)

val archConfigs = mapOf(
    "x86" to ArchConfig(
        "i686-linux-android",
        "x86"
    ),
//    "x64" to ArchConfig(
//        "x86_64-linux-android",
//        "x86_64"
//    ),
//    "arm32" to ArchConfig(
//        "armv7-linux-androideabi",
//        "armeabi-v7a"
//    ),
//    "arm64" to ArchConfig(
//        "aarch64-linux-android",
//        "arm64-v8a"
//    )
)

archConfigs.forEach { (id, config) ->
    val idSuffix = id.capitalize()

    tasks.create(name = "cargoBuild$idSuffix", type = Exec::class) {
        dependsOn("checkoutUplink")

        inputs.files("$uplinkRepo/Cargo.lock", "$uplinkRepo/Cargo.toml", "$uplinkRepo/uplink")
        outputs.file("$uplinkRepo/target/${config.rustTriplet}/debug/uplink")

        workingDir = uplinkRepo.toFile()
        environment("ANDROID_NDK_HOME", android.ndkDirectory)
        commandLine(
            "cargo", "ndk", "--target=${config.rustTriplet}", "--platform=23", "build", "--bin", "uplink"
        )
    }

    tasks.create(name = "packageUplinkExe$idSuffix", type = Copy::class) {
        dependsOn("cargoBuild$idSuffix")

        from("$uplinkRepo/target/${config.rustTriplet}/debug/uplink")
        into("src/main/assets/executables/${config.androidId}")
    }

    tasks.withType(JavaCompile::class) {
        dependsOn("packageUplinkExe$idSuffix")
    }

    tasks.create(name = "clean$idSuffix", type = Delete::class) {
        delete("src/main/assets/executables/${config.androidId}/uplink")
    }

    tasks.findByName("clean")!!.dependsOn("clean$idSuffix")
}