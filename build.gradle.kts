plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}

// O94: `-PrerunTests` (passed by TESTS/run_tests.ps1) reruns every requested test task, and only those
// (not the compiles, unlike --rerun-tasks), so the script counts every test instead of only fresh modules.
val rerunTests = providers.gradleProperty("rerunTests").isPresent
subprojects {
    tasks.withType<Test>().configureEach {
        if (rerunTests) {
            outputs.upToDateWhen { false }
            outputs.cacheIf { false }
        }
    }
}
