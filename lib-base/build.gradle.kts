import me.lingci.gradle.localBuildConfigString

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.parcelize)
    id("kotlinx-serialization")
}

val aesKey = project.localBuildConfigString("AES_KEY")
val aesIv = project.localBuildConfigString("AES_IV")

android {
    namespace = "me.lingci.lib.base"

    //noinspection GradleDependency
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        // AES 加密密钥，从 local.properties 读取，未配置时使用空字符串
        buildConfigField("String", "AES_KEY", aesKey)
        buildConfigField("String", "AES_IV", aesIv)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}



dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.androidx.annotation)
    api(libs.androidx.core) {
        exclude(group = "androidx.annotation")
        exclude(group = "androidx.lifecycle")
    }
    api(libs.androidx.activity){
        exclude(group = "androidx.annotation")
        exclude(group = "androidx.lifecycle")
    }
    api(libs.androidx.appcompat){
        exclude(group = "androidx.core")
        exclude(group = "androidx.activity")
        exclude(group = "androidx.fragment")
    }
    api(libs.androidx.coordinatorlayout)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.fragment)
    api(libs.androidx.recyclerview)
    api(libs.androidx.window)
    api(libs.android.material)
    // flow
    api(libs.android.flexbox)
    api(libs.bundles.okhttp)
    api(libs.glide)
    api(libs.jsoup)
    api(libs.lifecycle.livedata.ktx)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.jcifs.ng)

    testImplementation(libs.test.junit)
}
