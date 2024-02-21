plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.android.gradlePlugin.api)
    implementation(gradleKotlinDsl())
}

gradlePlugin {
    plugins {
        create("privacyApiSettings") {
            id = "com.zakzone.privacy_plugin"
            implementationClass = "TempTest"
        }
    }
}