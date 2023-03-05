import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    compileSdk = 33
    ndkVersion = "21.3.6528147"

    defaultConfig {
        applicationId = "name.lmj0011.holdup"
        minSdk = 28
        targetSdk = 33
        versionCode = 64
        versionName = "0.3.0-beta.4"

        vectorDrawables {
            useSupportLibrary = true
        }

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ref: https://stackoverflow.com/a/48674264/2445763
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            ndk {
                debugSymbolLevel = "FULL"
            }
        }

        named("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }

        /**
         * Debug with shrinking, obfuscation, and optimization applied
         *
         * Set "isDebuggable = false" to produce this build as
         * if it were a "release" build.
         */
        create("debugR8") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    buildTypes.forEach {
        if(it.name == "release") {
            // reddit app credentials (release)
            it.resValue("string", "reddit_app_clientId", "${project.findProperty("reddit.app.clientId")}")
            it.resValue("string", "reddit_app_redirectUri", "${project.findProperty("reddit.app.redirectUri")}")
        } else {
            // reddit app credentials (for any other build type)
            it.resValue("string", "reddit_app_clientId", "BBDkZM1qASiJTNpmQlMyYA")
            it.resValue("string", "reddit_app_redirectUri", "https://localhost:8080/my_callback")
        }

        it.resValue("string", "git_commit_count", getCommitCount())
        it.resValue("string", "git_commit_sha", getGitSha())
        it.resValue("string", "app_buildtime", getBuildTime())
    }

    flavorDimensions.add("default")

    productFlavors {
        /**
         *  The "main" flavor
         */
        create("core") {
            dimension = "default"
            resValue("string", "app_name", "Holdup")
        }

        /**
         *  A flavor (core + experimental features) intended for app testing distribution
         */
        create("preview") {
            dimension = "default"
            applicationIdSuffix = ".preview"
            versionNameSuffix = "+${getGitSha().take(7)}"
            resValue("string", "app_name", "Holdup (preview)")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    val fTree = fileTree("lib")
    fTree.include("*.jar")

    implementation(fTree)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${findProperty("kotlin.version")}")

    implementation("org.jetbrains.kotlin:kotlin-reflect:${findProperty("kotlin.version")}")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.2")

    implementation ("com.google.android.material:material:1.3.0-alpha04") {
        exclude(group = "androidx.recyclerview",  module = "recyclerview")
        exclude(group = "androidx.recyclerview",   module = "recyclerview-selection")
    }

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    val lifecycleVersion = "2.5.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")

    implementation(project(path = ":auth"))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")


    // Room dependencies
    val roomVersion = "2.2.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    val workVersion = "2.7.1"
    implementation("androidx.work:work-runtime-ktx:$workVersion")

    // Coroutines
    val coroutinesVersion = "1.4.1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    // androidx.preference
    implementation("androidx.preference:preference-ktx:1.2.0")

    // dependency injection
    implementation("org.kodein.di:kodein-di:7.7.0")

    // Timber Logger
    implementation("com.jakewharton.timber:timber:4.7.1")

    val moshiVersion = "1.13.0"
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")


    val okhttpVersion = "4.9.0"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

    val glideVersion = "4.11.0"
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    kapt("com.github.bumptech.glide:compiler:$glideVersion")

    val exoPlayerVersion = "2.14.1"
    implementation("com.google.android.exoplayer:exoplayer-core:$exoPlayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-dash:$exoPlayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exoPlayerVersion")

    val ioSocketVersion = "2.0.1"
    implementation("io.socket:socket.io-client:$ioSocketVersion")

    implementation(platform("com.google.firebase:firebase-bom:30.2.0"))
    implementation ("com.google.firebase:firebase-crashlytics-ktx")
    implementation ("com.google.firebase:firebase-analytics-ktx")

    implementation("org.jsoup:jsoup:1.13.1")
    implementation("com.kroegerama:bottomsheet-imagepicker:1.1.2")
    implementation("com.github.javafaker:javafaker:1.0.2")
}

// Git is needed in your system PATH for these commands to work.
// If it's not installed, you can return a random value as a workaround
// ref: https://github.com/tachiyomiorg/tachiyomi/blob/master/app/build.gradle.kts
fun getCommitCount(): String {
    return runCommand("git rev-list --count HEAD")
}

fun getGitSha(): String {
    return runCommand("git rev-parse HEAD")
}

fun getBuildTime(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    df.timeZone = TimeZone.getTimeZone("UTC")
    return df.format(Date())
}

fun runCommand(command: String): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split(" ")
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}
