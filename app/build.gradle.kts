plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "com.valtheron.horizon.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.valtheron.horizon.mobile"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
    }
    // Debug artifacts from CI use disposable signing keys. Keep the preview
    // package separate, so installing a new test APK cannot erase data in
    // the already installed Horizon Mobile app.
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".preview"
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.coroutines.android)
    implementation("androidx.webkit:webkit:1.13.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
