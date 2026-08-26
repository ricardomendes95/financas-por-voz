// Rota 1 (§5.2) — Android 16+, futuro-prova. Timebox de meio dia (§15):
// API alpha, instável, e o §0.3 já avisa para tratar como bônus, não base.
// Se o AppFunctions não indexar no S23, o resto do app funciona sozinho.
//
// Ligado ao :app desde que o projeto migrou para compileSdk 37 / AGP 9.x
// (decisão deliberada, discutida — `androidx.appfunctions` exige essa
// stack). As 4 funções (`addExpense`, `addIncome`, `querySpending`,
// `createCategory`) usam KDoc como contrato lido pelo agente, validado no
// build: o compiler gera o service concreto e o XML de metadata a partir
// do KDoc.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "br.com.financas.integration.appfunctions"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // O <service> do manifest referencia androidx.appfunctions.service.
        // PlatformAppFunctionService, que não é uma classe da dependência
        // Maven — é fornecida pelo próprio framework Android 16+ em
        // runtime (parte da plataforma, como android.app.Service). O lint
        // nunca vai encontrá-la no classpath de compilação, independente
        // do compileSdk configurado.
        disable += "MissingClass"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":feature:voice"))

    implementation(libs.appfunctions)
    ksp(libs.appfunctions.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
