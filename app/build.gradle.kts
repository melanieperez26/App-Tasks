plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    // el plugin de google-services se aplicará más abajo si google-services.json existe en el módulo app/
}

// Si el archivo google-services.json está en la raíz del repo (por ejemplo C:/aplicacionmovil/UniTrack20/google-services.json)
// cópialo automáticamente al módulo app (app/google-services.json) para que el plugin de Google lo detecte.
val rootGs = rootProject.file("google-services.json")
val moduleGs = project.file("google-services.json")
if (!moduleGs.exists() && rootGs.exists()) {
    println("Copiando google-services.json desde raíz '${rootGs.path}' a módulo '${moduleGs.path}'")
    rootGs.copyTo(moduleGs, overwrite = true)
}

// Aplicar plugin google-services si después de la copia el archivo existe en el módulo app.
if (moduleGs.exists()) {
    println("google-services.json encontrado en: ${moduleGs.path}, aplicando plugin com.google.gms.google-services")
    apply(plugin = "com.google.gms.google-services")
} else {
    println("google-services.json NO encontrado en: ${moduleGs.path} - Firebase NO se inicializará. Añade el archivo en app/ para habilitar Firebase configurado por google-services.json")
}

android {
    namespace = "com.example.unitrack20"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.unitrack20"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


    buildFeatures {
        compose = true
    }
}

// Configurar jvmTarget en las tareas Kotlin usando el DSL moderno (compilerOptions)
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java).configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Material icons (extended) - resuelve Icons.Default.*
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase dependencias con versiones explícitas para evitar problemas de metadata
    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")

    // Coroutines helper para Tasks (await) - coordenada directa
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.3")

    // DataStore preferences
    implementation("androidx.datastore:datastore-preferences:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Storage
    implementation("com.google.firebase:firebase-storage-ktx")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
