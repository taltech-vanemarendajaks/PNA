import java.util.Properties
import java.io.File

plugins {
    id("com.android.application")
}

val devProperties = Properties()
val devPropertiesFile: File = rootProject.file("dev.properties")
if (devPropertiesFile.exists()) {
    devProperties.load(devPropertiesFile.inputStream())
}

android {
    namespace = "com.example.callgrabber"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.callgrabber"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val DEFAULT_SERVER_URL = devProperties.getProperty("DEFAULT_SERVER_URL")
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"$DEFAULT_SERVER_URL\"")

        val GOOGLE_WEB_CLIENT_ID = devProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$GOOGLE_WEB_CLIENT_ID\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}