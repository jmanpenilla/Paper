plugins {
    id("paper.api-conventions")
}

dependencies {
    api("com.mojang:brigadier:1.0.17")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.hamcrest:hamcrest-library:1.3")
    testImplementation("org.ow2.asm:asm-tree:7.3.1")
    compileOnly(projects.paperApi)
    compileOnly("it.unimi.dsi:fastutil:8.2.2")
    compileOnly("org.jetbrains:annotations:18.0.0")
}
