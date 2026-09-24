version = 1

cimastream {
    setRepo("https://github.com/mehdigm4life/cimastream-arabic")
    buildBranch = "plugins"
    description = "مشاهدة وتحميل الأفلام والمسلسلات العربية والأجنبية والمترجمة."
    authors = listOf("MehdiGM")
    status = 1
    language = "ar"
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://raw.githubusercontent.com/mehdigm4life/cimastream-arabic/plugins/icon.png"
}

android {
    namespace = "com.mehdigm.cimastream4.arabic"
    compileSdk = 37
    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    val cimastream by configurations
    cimastream("com.github.mehdigm4life.cimastream:library-android:api-1.0.0")

    implementation("com.github.mehdigm4life:NiceHttp:v0.5.0")
    implementation("org.jsoup:jsoup:1.17.2")
}