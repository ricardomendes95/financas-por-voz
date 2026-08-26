pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "financas"

include(":app")
include(":nlu")
include(":core:model")
include(":core:database")
include(":core:common")
include(":core:data")
include(":core:designsystem")
include(":feature:dashboard")
include(":feature:transactions")
include(":feature:voice")
include(":feature:widget")
include(":feature:settings")
include(":feature:reports")
include(":feature:budgets")
include(":integration:appfunctions")
include(":integration:notifications")
