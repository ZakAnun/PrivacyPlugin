plugins {
    id("java-gradle-plugin")
}

dependencies {
    gradleApi()
    compileOnly("com.android.tools.build:gradle:8.2.0")
    compileOnly("commons-io:commons-io:2.8.0")
    compileOnly("commons-codec:commons-codec:1.15")
    compileOnly("org.ow2.asm:asm-commons:9.5")
    compileOnly("org.ow2.asm:asm-tree:9.5")
}