// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0-beta05")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${findProperty("kotlin.version")}")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.3")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url ="https://jitpack.io")
    }
}

tasks.create("clean") {
    delete(rootProject.buildDir)
}