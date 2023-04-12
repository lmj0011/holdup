plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    compileSdk = 33
    // TODO - commit note: declare vars directly in app's build.gradle to enable
    // importing this library module to another project without  build error
    // https://developer.android.com/studio/projects/android-library#AddDependency

    defaultConfig {
        minSdk = 19
        targetSdk = 33
    }

    sourceSets {
        map { it.java.srcDir("src/${it.name}/kotlin") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildTypes {
        create("debugR8") {}
    }
}

kapt {
    useBuildCache = true
}

dependencies {
    val fTree = fileTree("lib")
    fTree.include("*.jar")

    implementation(fTree)

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${findProperty("kotlin.version")}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${findProperty("kotlin.version")}")

    val okhttpVersion = "4.9.0"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

    val retrofitVersion = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")

    val moshiVersion = "1.13.0"
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    kapt           ("com.squareup.moshi:moshi-kotlin-codegen:$moshiVersion")
}