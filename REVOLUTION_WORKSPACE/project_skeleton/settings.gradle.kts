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
    }
}

rootProject.name = "gps-revolution"

// Application Modules
include(":app-sensor")
include(":app-monitor")

// Core Libraries
include(":core-shared")
include(":core-network")
include(":core-ui")
