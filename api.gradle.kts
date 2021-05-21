plugins {
    id("paper.api-conventions")
    checkstyle
}

description = "Paper API"

dependencies {
    api(platform(setOf("net.kyori", "adventure-bom", project.property(Constants.Properties.ADVENTURE_VERSION) as String).joinToString(":")))
    api("net.kyori:adventure-api")
    api("net.kyori:adventure-text-serializer-gson")
    api("net.kyori:adventure-text-serializer-legacy")
    api("net.kyori:adventure-text-serializer-plain")

    api("commons-lang:commons-lang:2.6")
    api("com.google.code.findbugs:jsr305:1.3.9")
    api("com.googlecode.json-simple:json-simple:1.1.1")
    api("com.google.guava:guava:21.0")
    api("com.google.code.gson:gson:2.8.0")
    api("net.md-5:bungeecord-chat:1.16-R0.4")
    api("org.yaml:snakeyaml:1.27")
    api("org.slf4j:slf4j-api:1.7.25")
    api("org.ow2.asm:asm:9.0")
    api("org.ow2.asm:asm-commons:9.0")

    compileOnly(testImplementation("it.unimi.dsi:fastutil:8.2.2")!!)
    compileOnly(testImplementation("org.apache.maven:maven-resolver-provider:3.8.1")!!)
    compileOnly(testImplementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.6.2")!!)
    compileOnly(testImplementation("org.apache.maven.resolver:maven-resolver-transport-http:1.6.2")!!)
    compileOnly(testImplementation("org.jetbrains:annotations-java5:20.1.0")!!)

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.hamcrest:hamcrest-library:1.3")
    testImplementation("org.ow2.asm:asm-tree:9.1")

    checkstyle("com.puppycrawl.tools:checkstyle:8.39")
}

tasks {
    jar {
        val tempFile = buildDir.resolve("tmp/pom.properties")
        doFirst {
            tempFile.writeText("version=${project.version}")
        }
        from(tempFile) {
            into("META-INF/maven/${project.group}/${project.name}")
        }
        manifest.attributes("Automatic-Module-Name" to "org.bukkit")
    }
    withType<Checkstyle> {
        // there are a lot of checkstyle violations.
        onlyIf { false }
    }
}

checkstyle {
    configFile = file("checkstyle.xml")
    sourceSets = listOf(project.sourceSets.main.get(), project.sourceSets.test.get())
}
