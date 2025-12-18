// Top-level build file

plugins {
    kotlin("android") version "1.9.10" apply false
    id("com.android.application") version "8.13.1" apply false
    id("com.android.library") version "8.13.1" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false

    // FIX: KSP version must match Kotlin version (1.9.10)
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false

}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.13.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
        classpath("com.google.gms:google-services:4.4.0")
    }
}