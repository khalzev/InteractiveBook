pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Позволяет Gradle самому скачать JDK нужной версии (17), если подходящего
    // локального JDK не найдено — иначе toolchain download нужно настраивать вручную.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "InteractiveBook"
include(":app")
