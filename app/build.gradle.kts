import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    id("kotlin-kapt")
}

// Load API keys from local.properties (gitignored) or environment variables
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

fun getApiKey(key: String, defaultValue: String = ""): String {
    return localProperties.getProperty(key)
        ?: System.getenv(key)
        ?: defaultValue
}

android {
    namespace = "com.petsafety.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.petsafety.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Inject Google Maps API key from local.properties or environment variable
        // For local development: add GOOGLE_MAPS_API_KEY=your_key to local.properties
        // For CI/CD: set GOOGLE_MAPS_API_KEY environment variable
        val mapsApiKey = getApiKey("GOOGLE_MAPS_API_KEY", "")
        if (mapsApiKey.isNotEmpty()) {
            resValue("string", "google_maps_key", mapsApiKey)
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // Use production API (same as iOS) for OTP to work
            buildConfigField("String", "API_BASE_URL", "\"https://pet-er.app/api/\"")
            buildConfigField("String", "SSE_BASE_URL", "\"https://pet-er.app/api/sse/events\"")
        }
        create("staging") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            isDebuggable = true
            buildConfigField("String", "API_BASE_URL", "\"https://staging.pet-er.app/api/\"")
            buildConfigField("String", "SSE_BASE_URL", "\"https://staging.pet-er.app/api/sse/events\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "API_BASE_URL", "\"https://pet-er.app/api/\"")
            buildConfigField("String", "SSE_BASE_URL", "\"https://pet-er.app/api/sse/events\"")
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
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kapt {
    correctErrorTypes = true
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    implementation(libs.coil.compose)

    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)

    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.config)
    implementation(libs.sentry.android)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.datastore.preferences)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.work:work-testing:2.9.1")
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
