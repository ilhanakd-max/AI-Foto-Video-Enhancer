plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.ilhan.ai_enhancer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ilhan.ai_enhancer"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources.excludes += "/META-INF/*"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.mlkit:vision-common:17.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
