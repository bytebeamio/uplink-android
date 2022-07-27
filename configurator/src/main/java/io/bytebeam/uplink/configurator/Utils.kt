package io.bytebeam.uplink.configurator

abstract class Architecture {
    abstract val assetId: String
}
object X86 : Architecture() {
    override val assetId = "x86"
}
object X64 : Architecture() {
    override val assetId = "x86_64"
}
object ARM : Architecture() {
    override val assetId = "armeabi-v7a"
}
object ARM64 : Architecture() {
    override val assetId = "arm64-v8a"
}

class UnknownArchitecture(val name: String) : Architecture() {
    override val assetId: String
        get() = throw IllegalStateException("can't get assetId for UnknownArchitecture")
}

private fun findArchitecture() : Architecture {
    Runtime.getRuntime().exec(arrayOf("uname", "-m")).let {
        it.waitFor()
        when (val name = it.inputStream.bufferedReader().readText()) {
            "x86_64" -> return X64
            "x86" -> return X86
            "arm64" -> return ARM64
            "arm" -> return ARM
            else -> return UnknownArchitecture(name)
        }
    }
}

val ourArchitecture = findArchitecture()