plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.angeluzt.miprimerempleo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.angeluzt.miprimerempleo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // La URL del backend que hace de proxy a OpenAI.
        // La API key de OpenAI vive SOLO en el backend, nunca aquí.
        buildConfigField(
            "String",
            "BACKEND_URL",
            "\"${project.findProperty("backendUrl") ?: "https://TU-PROYECTO.cloudfunctions.net"}\""
        )

        // AdMob. Por defecto van los identificadores PÚBLICOS DE PRUEBA de Google:
        // la app se puede probar hoy sin cuenta de AdMob, y sobre todo sin arriesgar
        // que Google suspenda una cuenta real por los clics de uno mismo probando.
        // Para publicar hay que pasar los propios:
        //   ./gradlew assembleRelease -PadmobAppId=ca-app-pub-XXX~YYY \
        //                             -PadmobRecompensado=ca-app-pub-XXX/ZZZ
        val admobAppId = project.findProperty("admobAppId")?.toString()
            ?: "ca-app-pub-3940256099942544~3347511713"
        val admobRecompensado = project.findProperty("admobRecompensado")?.toString()
            ?: "ca-app-pub-3940256099942544/5224354917"
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_RECOMPENSADO", "\"$admobRecompensado\"")
        buildConfigField(
            "boolean",
            "ADMOB_DE_PRUEBA",
            "${admobAppId.startsWith("ca-app-pub-3940256099942544")}",
        )
    }

    sourceSets["main"].assets.srcDirs(
        "src/main/assets",
        // Los prompts viven en /prompts y los comparten backend, script y app.
        "../prompts",
    )

    buildTypes {
        debug {
            // Play Billing no funciona en un APK instalado a mano, así que sin esto
            // no habría forma de probar el contenido de paga. Solo existe en debug.
            buildConfigField("boolean", "DESBLOQUEO_PRUEBA", "true")
            // Permite pegar una llave de OpenAI dentro de la app para probar en un
            // teléfono real. Solo en debug: en release la llave siempre vive en el backend.
            buildConfigField("boolean", "LLAVE_LOCAL_PERMITIDA", "true")
        }
        release {
            buildConfigField("boolean", "DESBLOQUEO_PRUEBA", "false")
            buildConfigField("boolean", "LLAVE_LOCAL_PERMITIDA", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // @JsonClassDiscriminator es experimental: lo usamos para que el mismo JSON
        // de contenido sirva a la app y más adelante al libro.
        freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Pagos: SOLO productos de pago único (INAPP). Esta app no usa suscripciones.
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // Publicidad: SOLO anuncios con recompensa, que la persona decide ver.
    // Nunca intersticiales sobre una acción que el usuario pidió.
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
}
