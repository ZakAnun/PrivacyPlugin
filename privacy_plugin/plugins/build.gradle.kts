plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(gradleKotlinDsl())
    compileOnly(libs.android.gradlePlugin.api)
    compileOnly(libs.asm.commons.api)
    compileOnly(libs.asm.tree.api)
}

gradlePlugin {
    plugins {
        create("privacyApiSettings") {
            id = "com.zakanun.privacy_plugin"
            implementationClass = "ModifyClassesPlugin"
        }
    }
}
