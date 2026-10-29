plugins {
    `java-library`
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
}

// Corrected: Strict enforcement of zero Android dependencies (Point 62)
configurations.all {
    exclude(group = "com.google.android", module = "android")
    exclude(group = "androidx.core")
}
