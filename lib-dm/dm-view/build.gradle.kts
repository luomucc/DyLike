plugins {
    id("com.android.library")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
}

android {
    namespace = "me.lingci.lib.dm.view"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}



dependencies {
    api(project(":lib-base"))
    implementation (project(":lib-common:comm-archive"))
    // 弹幕
    api(libs.danmaku.fix)
}

