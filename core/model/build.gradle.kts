// Kotlin puro, sem Android. Data classes de domínio usadas pela UI e pelos
// repositories — não são as @Entity do Room, para a UI não depender de
// anotações de persistência.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}
