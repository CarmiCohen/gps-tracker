pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}

rootProject.name = "gps-tracker"

// Application Modules
include(":app-sensor")
include(":app-monitor")

// Core Libraries
include(":core-shared")
include(":core-network")
include(":core-ui")
