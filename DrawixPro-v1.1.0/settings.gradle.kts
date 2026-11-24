pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io"); content { includeGroup("com.github.topjohnwu.libsu") } }
        maven { url = uri("https://api.xposed.info/"); content { includeGroup("de.robv.android.xposed") } }
    }
}
rootProject.name = "Drawix Pro"
include(":app")
