plugins {
id("com.android.application")
id("org.jetbrains.kotlin.android")
}

android {
namespace = "com.example.helloworld"
compileSdk = 34

code
Code
download
content_copy
expand_less
defaultConfig {
    applicationId = "com.example.helloworld"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
    
    // Enable C++ for Vulkan NDK
    externalNativeBuild {
        cmake {
            cppFlags += "-std=c++17"
        }
    }
}

compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlinOptions {
    jvmTarget = "17"
}

// Link the CMake script
externalNativeBuild {
    cmake {
        path("src/main/cpp/CMakeLists.txt")
        version = "3.22.1"
    }
}

}

dependencies {
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
}