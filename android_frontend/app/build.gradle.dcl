androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))

        // UI + Material Components (classic Views)
        implementation("com.google.android.material:material:1.12.0")

        // AndroidX core UI building blocks
        implementation("androidx.core:core-ktx:1.13.1")
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")

        // Lifecycle / ViewModel (retain cart state across configuration changes)
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    }
}
