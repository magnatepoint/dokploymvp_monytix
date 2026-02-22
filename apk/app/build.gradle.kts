import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.monytix"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.monytix"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localPropsFile = project.rootProject.file("local.properties")
        val props = if (localPropsFile.exists()) {
            Properties().apply { load(localPropsFile.inputStream()) }
        } else null
        val supabaseUrl = props?.getProperty("SUPABASE_URL")
            ?: if (project.hasProperty("ciRelease")) null
            else "https://vwagtikpxbhjrffolrqn.supabase.co"
        val supabaseKey = props?.getProperty("SUPABASE_ANON_KEY") ?: props?.getProperty("SUPABASE_PUBLISHABLE_KEY")
            ?: if (project.hasProperty("ciRelease")) null
            else "your-key"
        val backendUrl = props?.getProperty("BACKEND_URL")
            ?: if (project.hasProperty("ciRelease")) null
            else "http://34.14.136.76:8001"
        if (project.hasProperty("ciRelease") && (supabaseUrl.isNullOrBlank() || supabaseKey.isNullOrBlank() || backendUrl.isNullOrBlank())) {
            throw GradleException(
                "CI release build requires SUPABASE_URL, SUPABASE_ANON_KEY, BACKEND_URL in local.properties or env. " +
                "Do not commit secrets. Use CI secrets for production builds."
            )
        }
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}