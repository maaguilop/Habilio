plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.habilio.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.habilio.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
        // BOM de Compose (gestiona las versiones compatibles automáticamente)
        val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
        implementation(composeBom)
        androidTestImplementation(composeBom)

        // Core y ciclo de vida
        implementation("androidx.core:core-ktx:1.13.1")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
        implementation("androidx.activity:activity-compose:1.9.2")

        // Jetpack Compose UI y Material 3
        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.ui:ui-graphics")
        implementation("androidx.compose.ui:ui-tooling-preview")
        implementation("androidx.compose.material3:material3")

        // OBLIGATORIA para este código: Íconos extendidos (Handyman, Plumbing, Carpenter, Verified, etc.)
        implementation("androidx.compose.material:material-icons-extended")

        // Opcional recomendada: Navegación nativa y carga de imágenes (si luego conectas URLs o API)
        implementation("androidx.navigation:navigation-compose:2.8.2")
        implementation("io.coil-kt:coil-compose:2.7.0")

        // Herramientas de Debug / Preview
        debugImplementation("androidx.compose.ui:ui-tooling")
        debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.compose.material:material-icons-extended:1.7.4")
}