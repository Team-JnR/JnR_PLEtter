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
        // Pleos Connect SDK를 붙일 때 Pleos Playground Maven 저장소를 여기에 추가합니다.
    }
}

rootProject.name = "PLEtter"
include(":app")
