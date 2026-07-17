pluginManagement {
    // Convention plugins live in the included build; every module applies them
    // by id (anima.*) instead of repeating AGP/Kotlin config.
    includeBuild("build-logic")
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
    }
}

dependencyResolutionManagement {
    // No module may declare its own repositories — one central list.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "anima-app"

// `projects.core.model` style accessors in module build files.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":core:model")
include(":core:body")
include(":core:creature")
include(":core:data")
include(":core:mind")
include(":core:ui")
include(":feature:onboarding")
include(":feature:home")
include(":feature:notifications")
include(":feature:soul")
include(":feature:settings")
include(":feature:widget")
include(":app")
include(":baselineprofile")
include(":core:model-delivery")
