// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP 9 trae su propio Kotlin integrado. Maps Compose usa Kotlin 2.4, así que
// declaramos aquí el Kotlin Gradle Plugin de esa versión para que el compilador coincida.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
