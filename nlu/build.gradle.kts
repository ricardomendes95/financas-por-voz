// Módulo Kotlin PURO. Nenhuma dependência de Android pode entrar aqui —
// é justamente isso que permite rodar o corpus de testes em milissegundos
// na JVM, sem emulador e sem Robolectric.

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
