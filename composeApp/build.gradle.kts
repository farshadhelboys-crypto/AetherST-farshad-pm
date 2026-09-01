import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library") version "9.0.0"  // ← این خط رو عوض کن
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.serialization)
}

val isAndroidDisabled = providers.gradleProperty("skipAndroid").getOrElse("false") == "true"

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    
    if (!isAndroidDisabled) {
        androidTarget {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            }
        }
    }

    jvm("desktop")

    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalMultiplatform")
        }
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.okhttp)
            implementation(libs.moshi.kotlin)
        }
        
        if (!isAndroidDisabled) {
            androidMain.dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.androidsvg)
                
                // Firebase
                implementation("com.google.firebase:firebase-bom:33.7.0")
                implementation("com.google.firebase:firebase-firestore-ktx")
                implementation("com.google.firebase:firebase-auth-ktx")
                
                // Compose Icons
                implementation(libs.androidx.compose.material.icons.core)
                implementation(libs.androidx.compose.material.icons.extended)
            }
        }
        
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

android {
    namespace = "io.github.immaghzbad.aetherst.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

compose.desktop {
    application {
        mainClass = "io.github.immaghzbad.aetherst.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "AetherST-Tunnel"
            packageVersion = "1.1.0"
            vendor = "ImMaghzBad"
            description = "AetherST High-Performance Proxy Tunnel"

            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/desktopMain/resources"))

            windows {
                dirChooser = true
                menu = true
                shortcut = true
                upgradeUuid = "d7d4c82e-6f8b-4a5f-8c31-97a2e3f6d4d1"
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
            }

            buildTypes.release.proguard {
                isEnabled.set(true)
                optimize.set(true)
                obfuscate.set(true)
                configurationFiles.from(project.file("proguard-rules.pro"))
            }
        }
    }
}
