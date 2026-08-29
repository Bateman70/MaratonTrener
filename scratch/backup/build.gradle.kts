plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.jostein.maratontrener"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lillevika.android"
        minSdk = 23
        targetSdk = 35

        versionCode = 493
        versionName = "4.93"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

tasks.register<Copy>("prepareBuddyDownload") {
    dependsOn("assembleDebug")
    val desktop = File(System.getProperty("user.home"), "Desktop")
    val oneDriveDesktop = File(System.getProperty("user.home"), "OneDrive/Desktop")
    val targetDir = if (oneDriveDesktop.exists()) oneDriveDesktop else desktop
    
    from("${layout.buildDirectory.get()}/outputs/apk/debug/app-debug.apk")
    into(targetDir)
    rename { "THE_RUM_RUNNER_BETA.apk" }
    
    doLast {
        println("🚀 SUCCESS! Your buddy file is ready here: ${targetDir.absolutePath}/THE_RUM_RUNNER_BETA.apk")
    }
}

tasks.register<Copy>("prepareGoogleUpload") {
    dependsOn("bundleRelease")
    val desktop = File(System.getProperty("user.home"), "Desktop")
    val oneDriveDesktop = File(System.getProperty("user.home"), "OneDrive/Desktop")
    val targetDir = if (oneDriveDesktop.exists()) oneDriveDesktop else desktop

    from("${layout.buildDirectory.get()}/outputs/bundle/release/app-release.aab")
    into(targetDir)
    rename { "READY_FOR_GOOGLE.aab" }
}

dependencies {
    // We are using direct strings here to avoid "Unresolved reference" errors
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-analytics")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}