plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.boyoffi9.scifikit"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")

    // Compose is compileOnly: View-only apps don't pull it in,
    // Compose apps already have it.
    compileOnly(platform("androidx.compose:compose-bom:2024.06.00"))
    compileOnly("androidx.compose.ui:ui")
    compileOnly("androidx.compose.foundation:foundation")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.Boy-Offi9"
                artifactId = "scifikit"
                version = "0.1.0"
            }
        }
    }
}
