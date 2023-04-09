import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow
import java.nio.file.Paths
import kotlin.io.path.copyTo

buildscript {
    extra.apply {
        set("sdk_version", "v0.6.2")
        set("kotlin_version", "1.7.0")
    }
    repositories {
        google()
        mavenCentral()
        maven {
            url = java.net.URI("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.extra.get("kotlin_version")}")
        classpath("org.mozilla.rust-android-gradle:plugin:0.9.3")
    }
}

tasks.create("clean", type = Delete::class) {
    delete(rootProject.buildDir)
    subprojects.forEach {
        it.afterEvaluate {
            it.tasks.findByName("clean").let { subCleanTask ->
                if (subCleanTask != null) {
                    dependsOn(subCleanTask)
                }
            }
        }
    }
}

val archs = listOf(
    "x86_64-linux-android",
    "i686-linux-android",
    "aarch64-linux-android",
    "armv7-linux-androideabi"
)

val root = project.rootProject.rootDir.toPath()
val stage = root.resolve("build").resolve("release")
archs.forEach { arch ->
    // TODO: abstract cargo build
    tasks.create("build-uplink-$arch") {
        description = "build exe for $arch"
        doLast {
            val pb = ProcessBuilder()
                .command(
                    "cargo",
                    "ndk",
                    "--platform",
                    "23",
                    "--target",
                    arch,
                    "build",
                    "--release",
                    "--bin",
                    "uplink"
                )
                .directory(File("uplink"))
            pb.environment().let {
                it["ANDROID_NDK_HOME"] = "${it["HOME"]}/Android/Sdk/ndk/25.0.8775105"
            }

            val process = pb.start()
            println(String(process.inputStream.readAllBytes()))
            println(String(process.errorStream.readAllBytes()))

            if (process.waitFor() != 0) {
                throw Exception("cargo build failed")
            }
        }
    }

    tasks.create("build-utilities-$arch") {
        description = "build utilities for $arch"
        doLast {
            val pb = ProcessBuilder()
                .command(
                    "cargo",
                    "ndk",
                    "--platform",
                    "23",
                    "--target",
                    arch,
                    "build",
                    "--release",
                )
                .directory(root.resolve("utilities").toFile())
            pb.environment().let {
                it["ANDROID_NDK_HOME"] = "${it["HOME"]}/Android/Sdk/ndk/25.0.8775105"
            }

            val process = pb.start()
            println(String(process.inputStream.readAllBytes()))
            println(String(process.errorStream.readAllBytes()))

            if (process.waitFor() != 0) {
                throw Exception("cargo build failed")
            }
        }
    }

    task("template-$arch", type = Copy::class) {
        description = "copy module template for $arch"

        from(root.resolve("module_template"))
        into(stage.resolve(arch))
    }

    task("copy-uplink-$arch", type = Copy::class) {
        from(Paths.get("uplink/target/$arch/release/uplink"))
        into(stage.resolve(arch).resolve("bin"))
    }

    task("copy-utilities-$arch", type = Copy::class) {
        arrayOf("logrotate", "uplink_watchdog").forEach { utility ->
            from(root.resolve(root.resolve(Paths.get("utilities", "target", arch, "release", utility))))
        }
        into(stage.resolve(arch).resolve("bin"))
    }

    task("module-dir-$arch") {
        description = "create module for $arch"
        dependsOn(
            "build-uplink-$arch",
            "build-utilities-$arch",
            "template-$arch",
            "copy-uplink-$arch",
            "copy-utilities-$arch"
        )
    }

    task("module-$arch") {
        description = "module archive for $arch"
        dependsOn("module-dir-$arch")
        doLast {
            // create tar gz of module directory
            val pb = ProcessBuilder()
                .command(
                    "tar",
                    "cf",
                    "$arch.tar.gz",
                    "-C",
                    arch,
                    ".",
                )
                .directory(stage.toFile())

            val process = pb.start()
            println(String(process.inputStream.readAllBytes()))
            println(String(process.errorStream.readAllBytes()))
//            stage.resolve(arch).toFile().deleteRecursivelyOrThrow()

            if (process.waitFor() != 0) {
                throw Exception("tar failed")
            }
        }
    }
}

tasks.create("copy-lib", type = Copy::class) {
    dependsOn("lib:assembleRelease")
    from(root.resolve(Paths.get("lib", "build", "outputs", "aar", "uplink-release.aar")))
    into(stage)
    rename("uplink-release.aar", "uplink_${project.extra.get("sdk_version")}.aar")
}

tasks.create("buildArtifacts") {
    dependsOn("clean")
    archs.forEach { arch ->
        dependsOn("module-$arch")
    }
    dependsOn("copy-lib")
}

fun<T> trace(value: T) : T {
    println(value)
    return value
}