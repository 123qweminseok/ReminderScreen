plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}




android {
    namespace = "com.minseok.reminderscreen"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.minseok.reminderscreen"
        minSdk = 24
        targetSdk = 34
        versionCode = 10
        versionName = "1.0"
        buildConfigField("String", "WIT_AI_TOKEN", "\"TVAQWGNCT65HLHE7IVONUY7STCH33AJZ\"")

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true  // 이 줄 추가

    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation ("androidx.work:work-runtime-ktx:2.8.1")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.tv.material)
    kapt(libs.androidx.room.compiler)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    implementation ("com.github.skydoves:colorpickerview:2.2.4")  // 최신 버전을 사용하세요

    implementation("com.kizitonwose.calendar:compose:2.6.0")  // 최신 버전 사용
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")  // 최신 버전 사용
    implementation("com.kizitonwose.calendar:view:2.6.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")  // build.gradle.kts에 추가

    // Network

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // JSON
    implementation("org.json:json:20210307")

    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.google.android.material:material:1.9.0")

}