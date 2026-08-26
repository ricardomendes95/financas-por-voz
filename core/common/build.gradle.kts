// Kotlin puro: Clock, formatadores e helpers de data/moeda usados por
// repositories e ViewModels. Sem dependência de Android — java.time e
// java.text já cobrem tudo, disponíveis nativamente desde minSdk 26.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
