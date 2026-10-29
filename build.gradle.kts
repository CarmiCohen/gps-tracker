plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.google.protobuf") version "0.9.4" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
