import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val testAccounts = Properties().apply {
    val credentialsFile = rootProject.file("test-accounts.properties")
    if (credentialsFile.exists()) credentialsFile.inputStream().use { load(it) }
}

fun testAccountValue(name: String): String = testAccounts.getProperty(name, "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.example.medicare"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.medicare"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            listOf("ADMIN", "DOCTOR", "PATIENT").forEach { role ->
                buildConfigField("String", "TEST_${role}_EMAIL", "\"${testAccountValue("${role}_EMAIL")}\"")
                buildConfigField("String", "TEST_${role}_PASSWORD", "\"${testAccountValue("${role}_PASSWORD")}\"")
            }
        }
        release {
            listOf("ADMIN", "DOCTOR", "PATIENT").forEach { role ->
                buildConfigField("String", "TEST_${role}_EMAIL", "\"\"")
                buildConfigField("String", "TEST_${role}_PASSWORD", "\"\"")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.viewpager2)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
}
