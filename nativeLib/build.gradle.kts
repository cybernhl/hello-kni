import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import com.codingfeline.buildkonfig.compiler.FieldSpec
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("com.codingfeline.buildkonfig")
}

val jniLibDir = File(project.buildDir, arrayOf("generated", "jniLibs").joinToString(File.separator))

val sharedLib_name_prefix = "knlib"

kotlin {
    android()

    val nativeConfigure: KotlinNativeTarget.() -> Unit = {
        binaries {
            sharedLib(sharedLib_name_prefix) {
                linkTask.doLast {
                    copy {
                        from(outputFile)

                        val typeName = if (buildType == NativeBuildType.DEBUG) "Debug" else "Release"
                        val abiDirName = when(target.konanTarget) {
                            ANDROID_ARM32 -> "armeabi-v7a"
                            ANDROID_ARM64 -> "arm64-v8a"
                            ANDROID_X86 -> "x86"
                            ANDROID_X64 -> "x86_64"
                            else -> "unknown"
                        }

                        into(file("$jniLibDir/$typeName/$abiDirName"))
                    }
                }

                afterEvaluate {
                    val preBuild by tasks.getting
                    preBuild.dependsOn(linkTask)
                }
            }
        }
    }

    androidNativeArm32(configure = nativeConfigure)
    androidNativeArm64(configure = nativeConfigure)
    androidNativeX86(configure = nativeConfigure)
    androidNativeX64(configure = nativeConfigure)

    sourceSets {
        val androidNativeArm32Main by getting
        val androidNativeArm64Main by getting
        val androidNativeX86Main by getting
        val androidNativeX64Main by getting

        val nativeMain by creating {
            androidNativeArm32Main.dependsOn(this)
            androidNativeArm64Main.dependsOn(this)
            androidNativeX86Main.dependsOn(this)
            androidNativeX64Main.dependsOn(this)
        }
    }
}

buildkonfig {
    packageName = "com.example.hellojni.HelloJni"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "C_NAME_PREFIX_KCONFIG", "Java_"+packageName.toString().replace(".","_"))
    }
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 23
        targetSdk = 33
        buildConfigField(
            "String",
            "JNI_SHARED_LIB_NAME_PREFIX",
            "\"$sharedLib_name_prefix\""
        )
    }

    sourceSets {
        getByName("debug").jniLibs.srcDirs("$jniLibDir/Debug")
        getByName("release").jniLibs.srcDirs("$jniLibDir/Release")
    }
}
