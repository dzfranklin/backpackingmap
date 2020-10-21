// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.gradle}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("android.arch.navigation:navigation-safe-args-gradle-plugin:${Versions.navigationSafeArgs}")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://kotlin.bintray.com/kotlinx/")
        maven(url = "https://dl.bintray.com/arrow-kt/arrow-kt/")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}