enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("ca.stellardrift.polyglot-version-catalogs") version "5.0.0"
}

rootProject.name = "paper-parent"

setupSubproject("paper") {
    projectDir = file("Paper-Server")
    buildFileName = "../server.gradle.kts"
}
setupSubproject("paper-api") {
    projectDir = file("Paper-API")
    buildFileName = "../api.gradle.kts"
}
setupSubproject("paper-mojangapi") {
    projectDir = file("Paper-MojangAPI")
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
