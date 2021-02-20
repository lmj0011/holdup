plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    // TODO - commit note: declare vars directly in app's build.gradle to enable
    // importing this library module to another project without  build error
    // https://developer.android.com/studio/projects/android-library#AddDependency
    compileSdkVersion(30)
    buildToolsVersion("30.0.2")

    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
    }

    sourceSets {
        map { it.java.srcDir("src/${it.name}/kotlin") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        noReflect = true
    }
}

kapt {
    useBuildCache = true
}

dependencies {
    val fTree = fileTree("lib")
    fTree.include("*.jar")

    implementation(fTree)

    implementation("androidx.core:core-ktx:1.3.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${findProperty("kotlin.version")}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${findProperty("kotlin.version")}")

    val okhttpVersion = "4.9.0"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

    val retrofitVersion = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")

    val moshiVersion = "1.11.0"
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    kapt           ("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
}