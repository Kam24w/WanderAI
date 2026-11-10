import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

/**
 * Firma de release compartida por WanderAI (teléfono) y Vibra (reloj): la Data Layer
 * solo entrega mensajes entre apps con el mismo applicationId Y el mismo certificado.
 * Los secretos van en local.properties (no versionado) o en variables de entorno:
 * VIBRA_RELEASE_STORE_FILE, VIBRA_RELEASE_STORE_PASSWORD, VIBRA_RELEASE_KEY_ALIAS,
 * VIBRA_RELEASE_KEY_PASSWORD. Sin ellos, la build release queda sin firmar.
 */
fun releaseSecret(name: String): String? =
    (localProperties.getProperty(name) ?: System.getenv(name))?.takeIf { it.isNotBlank() }
val releaseStoreFile = releaseSecret("VIBRA_RELEASE_STORE_FILE")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vibra.wanderai"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.vibra.wanderai"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_KEY",
            "\"${localProperties.getProperty("SUPABASE_KEY", "")}\""
        )

        // Google Maps SDK: AndroidManifest lee ${MAPS_API_KEY} (meta-data com.google.android.geo.API_KEY).
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY", "")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("boolean", "HAS_MAPS_API_KEY", mapsApiKey.isNotBlank().toString())
        // Misma clave para Routes API y Places API (New) (cabeceras X-Android-Package/Cert).
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")

        // Groq (chat de viaje con IA).
        buildConfigField(
            "String",
            "GROQ_API_KEY",
            "\"${localProperties.getProperty("GROQ_API_KEY", "")}\""
        )
        // Opcional: modelo de Groq (vacío = GroqProtocol.DEFAULT_MODEL).
        buildConfigField(
            "String",
            "GROQ_MODEL",
            "\"${localProperties.getProperty("GROQ_MODEL", "")}\""
        )
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseSecret("VIBRA_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSecret("VIBRA_RELEASE_KEY_ALIAS")
                keyPassword = releaseSecret("VIBRA_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseStoreFile != null) signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
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

dependencies {
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.ktor)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Wear OS Data Layer — Vibra watch app
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)

    // Google Maps SDK (vista Explorar)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // CameraX — WanderAI Vision viewfinder
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.compose.material.icons.extended)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}