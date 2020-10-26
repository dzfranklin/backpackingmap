
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.collections.set

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdkVersion(30)
    buildToolsVersion("30.0.2")

    defaultConfig {
        applicationId = "com.backpackingmap.backpackingmap"
        minSdkVersion(24)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
            }
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", "\"http://localhost:5080/api/v1/\"")
        }

        getByName("release") {
            buildConfigField("String", "API_BASE_URL", "\"https://backpackingmap.com/api/v1/\"")

            isMinifyEnabled = false

            proguardFile(getDefaultProguardFile("proguard-android-optimize.txt"))
            proguardFile("proguard-rules.pro")
        }
    }

    kotlinOptions {
        // Added per <https://square.github.io/okhttp/upgrading_to_okhttp_4/>
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xinline-classes")
    }

    compileOptions {
        // Added per <https://square.github.io/okhttp/upgrading_to_okhttp_4/>
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
//    implementation(fileTree(dir = "libs", include = ["*.jar"]))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinxCoroutinesAndroid}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinxCoroutinesAndroid}")

    implementation("androidx.core:core-ktx:1.3.1")
    implementation("androidx.appcompat:appcompat:1.2.0")
    testImplementation("junit:junit:4.12")
    testImplementation("org.hamcrest:hamcrest:2.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")

    implementation("com.google.android.material:material:1.2.1")

    implementation("androidx.constraintlayout:constraintlayout:2.0.1")

    implementation("androidx.navigation:navigation-fragment-ktx:2.3.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.0")

    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    implementation("com.jakewharton.timber:timber:4.7.1")

    implementation("com.squareup.moshi:moshi:1.9.3")
    implementation("com.squareup.moshi:moshi-kotlin:1.9.3")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    // HTTP Debugger
    debugImplementation("com.github.chuckerteam.chucker:library:3.3.0")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:3.3.0")

    // Arrow
    implementation("io.arrow-kt:arrow-core:${Versions.arrowVersion}")
    implementation("io.arrow-kt:arrow-syntax:${Versions.arrowVersion}")
    kapt("io.arrow-kt:arrow-meta:${Versions.arrowVersion}")

    implementation("androidx.room:room-runtime:${Versions.roomVersion}")
    kapt("androidx.room:room-compiler:${Versions.roomVersion}")
    implementation("androidx.room:room-ktx:${Versions.roomVersion}")
    testImplementation("androidx.room:room-testing:${Versions.roomVersion}")

    // Geodesy
    implementation("org.locationtech.proj4j:proj4j:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")

    implementation("com.google.android.gms:play-services-location:17.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${Versions.kotlinxCoroutinesAndroid}")
}
