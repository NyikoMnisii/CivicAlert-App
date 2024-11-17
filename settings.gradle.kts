pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Add Mapbox Maven repository here
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = System.getenv("MAPBOX_DOWNLOADS_TOKEN") // Your Mapbox token
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add Mapbox Maven repository here as well
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = System.getenv("MAPBOX_DOWNLOADS_TOKEN") // Your Mapbox token
            }
        }
    }
}

rootProject.name = "CivicAlertOriginal"
include(":app")
