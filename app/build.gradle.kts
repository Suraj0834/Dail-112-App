// =============================================================================
// Dial-112 Emergency Response System - Android App Build Configuration
// =============================================================================
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.dial112"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dial112"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API base URL from build config
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:5001/\"")
        buildConfigField("String", "AI_BASE_URL", "\"http://10.0.2.2:8000/\"")
        buildConfigField("String", "SOCKET_URL", "\"http://10.0.2.2:5001\"")
        buildConfigField("String", "MAPS_API_KEY", "\"AIzaSyBweXhdwxPGjfLXOdQLpnglIskuSB3K9P0\"")

        // Manifest placeholder for Maps API key
        manifestPlaceholders["MAPS_API_KEY"] = "AIzaSyBweXhdwxPGjfLXOdQLpnglIskuSB3K9P0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:5001/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.dial112.gov.in/\"")
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
        viewBinding = true
        buildConfig = true
    }

    // Allow multiple dex files (needed for some large dependencies)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // -------------------------------------------------------------------------
    // Core Android & Kotlin
    // -------------------------------------------------------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // -------------------------------------------------------------------------
    // Material Design 3
    // -------------------------------------------------------------------------
    implementation(libs.material)

    // -------------------------------------------------------------------------
    // Lifecycle & ViewModel (MVVM)
    // -------------------------------------------------------------------------
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // -------------------------------------------------------------------------
    // Navigation Component
    // -------------------------------------------------------------------------
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // -------------------------------------------------------------------------
    // Hilt Dependency Injection
    // -------------------------------------------------------------------------
    implementation(libs.hilt.android)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    kapt(libs.hilt.compiler)

    // -------------------------------------------------------------------------
    // Retrofit + OkHttp (Networking)
    // -------------------------------------------------------------------------
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // -------------------------------------------------------------------------
    // Room Database (Offline caching)
    // -------------------------------------------------------------------------
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // -------------------------------------------------------------------------
    // Coroutines
    // -------------------------------------------------------------------------
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // -------------------------------------------------------------------------
    // Google Maps + Location
    // -------------------------------------------------------------------------
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.utils)

    // -------------------------------------------------------------------------
    // CameraX
    // -------------------------------------------------------------------------
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // -------------------------------------------------------------------------
    // Socket.IO
    // -------------------------------------------------------------------------
    implementation(libs.socket.io) { exclude(group = "org.json", module = "json") }

    // -------------------------------------------------------------------------
    // Image Loading (Glide)
    // -------------------------------------------------------------------------
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // -------------------------------------------------------------------------
    // Lottie Animations
    // -------------------------------------------------------------------------
    implementation(libs.lottie)

    // -------------------------------------------------------------------------
    // BlurView (Glassmorphism)
    // -------------------------------------------------------------------------
    // implementation removed

    // -------------------------------------------------------------------------
    // SwipeRefreshLayout
    // -------------------------------------------------------------------------
    implementation(libs.androidx.swiperefreshlayout)

    // -------------------------------------------------------------------------
    // Constraint Layout & Motion Layout
    // -------------------------------------------------------------------------
    implementation(libs.androidx.constraintlayout)

    // -------------------------------------------------------------------------
    // DataStore (Preferences)
    // -------------------------------------------------------------------------
    implementation(libs.androidx.datastore.preferences)

    // -------------------------------------------------------------------------
    // WorkManager (Background tasks)
    // -------------------------------------------------------------------------
    implementation(libs.androidx.work.runtime.ktx)

    // -------------------------------------------------------------------------
    // Testing
    // -------------------------------------------------------------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Allow kapt to use generated stubs
kapt {
    correctErrorTypes = true
}