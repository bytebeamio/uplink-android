import java.nio.file.Files
import java.nio.file.Paths

extra.apply {
    set("sdk_version", "v0.2.5")
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.0")
        classpath("org.eclipse.jgit:org.eclipse.jgit:6.2.0.202206071550-r")
    }
}

tasks.create("clean", Delete::class) {
    delete = setOf(rootProject.buildDir)
    subprojects.forEach {
        it.afterEvaluate {
            val innerClean = it.tasks.findByName("clean")
            dependsOn(innerClean)
        }
    }
}

tasks.create("buildArtifacts") {
    description = "build all artifacts and move them to a directory for uploading"
    dependsOn(
        "configurator:assembleRelease",
        "example:assembleRelease",
        "lib:assembleRelease",
    )

    doLast {
        val stage = File("${System.getenv("HOME")}/stage/uplink")
        stage.mkdirs()
        for (file in stage.listFiles()!!) {
            file.delete()
        }
        val configurator =
            project.rootProject.rootDir.path + "/configurator/build/outputs/apk/release/configurator-release.apk"
        val example = project.rootProject.rootDir.path + "/example/build/outputs/apk/release/example-release.apk"
        val lib = project.rootProject.rootDir.path + "/lib/build/outputs/aar/uplink-release.aar"
        val sdkVersion = extra.get("sdk_version")
        Files.copy(Paths.get(configurator), Paths.get(stage.path, "configurator_${sdkVersion}.apk"))
        Files.copy(Paths.get(example), Paths.get(stage.path, "example_${sdkVersion}.apk"))
        Files.copy(Paths.get(lib), Paths.get(stage.path, "uplink_${sdkVersion}.aar"))
    }
}
