plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //   id("com.google.devtools.ksp")
    id("kotlin-kapt")
}

android {
    namespace = "com.lion.shopmanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lion.shopmanager"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding { enable = true }
}

dependencies {
    implementation(libs.glide)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

//    ksp(libs.androidx.room.compiler.v250)
//    ksp(libs.androidx.room.compiler.v250)
//    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.room.compiler)
//    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.room.ktx)

    // Room
    implementation("androidx.room:room-runtime:2.4.3")
    implementation("androidx.room:room-ktx:2.4.3")
    kapt("androidx.room:room-compiler:2.4.3")

    // gson
    implementation ("com.google.code.gson:gson:2.8.9")

    // Glide
    implementation ("com.github.bumptech.glide:glide:4.15.1")
}