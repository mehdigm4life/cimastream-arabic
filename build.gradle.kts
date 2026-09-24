buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
        classpath("com.github.mehdigm4life:cimastream-gradle:v1.1.0")
    }
}

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "com.mehdigm.cimastream4.gradle")

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}