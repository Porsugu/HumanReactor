plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.humanreactor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.humanreactor"
        minSdk = 25
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.appcompat)
//    implementation(libs.material)
//    implementation(libs.androidx.activity)
//    implementation(libs.androidx.constraintlayout)
//    implementation(libs.androidx.camera.core)
//    implementation(libs.androidx.camera.lifecycle)
//    implementation(libs.pose.detection.common)
//    implementation(libs.pose.detection)
//    implementation(libs.androidx.camera.view)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // CameraX
    val cameraxVersion = "1.2.3"  // 固定使用1.2.3版本，這個版本的API更穩定
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")


    // ML Kit for pose detection
    implementation("com.google.mlkit:pose-detection:18.0.0-beta3")

    //Smile
    implementation ("com.github.haifengl:smile-core:2.6.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.cardview)
    implementation(libs.litert.api)
    implementation(libs.litert)
    implementation(libs.litert.support.api)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.pose.detection.accurate)

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("tw.edu.ntu.csie:libsvm:3.24")

    //AI API
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("org.json:json:20210307")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")


}