plugins {
    id("paper.common-conventions")
}

java {
    withJavadocJar()
}

configurePublication {
    from(components["java"])
}

publishing {
    repositories {
        maven {
            name = "releases"
            url = uri(property(Constants.Properties.RELEASES_REPO) as String)
        }
        maven {
            name = "snapshots"
            url = uri(property(Constants.Properties.SNAPSHOTS_REPO) as String)
        }
    }
}

tasks {
    javadoc {
        options {
            if (this !is StandardJavadocDocletOptions) return@options
            val adventureVersion = project.property(Constants.Properties.ADVENTURE_VERSION) as String
            val currentJava = JavaVersion.current().majorVersion.toInt()
            links(
                jdkApiDocs(currentJava),
                "https://guava.dev/releases/21.0/api/docs/",
                "https://javadoc.io/doc/org.yaml/snakeyaml/1.27/",
                "https://javadoc.io/doc/org.jetbrains/annotations-java5/20.1.0/",
                "https://javadoc.io/doc/net.md-5/bungeecord-chat/1.16-R0.4/",
                "https://jd.adventure.kyori.net/api/$adventureVersion/",
                "https://jd.adventure.kyori.net/text-serializer-gson/$adventureVersion/",
                "https://jd.adventure.kyori.net/text-serializer-legacy/$adventureVersion/",
                "https://jd.adventure.kyori.net/text-serializer-plain/$adventureVersion/"
            )
            if (currentJava in 9..11) {
                // Apply workaround for https://bugs.openjdk.java.net/browse/JDK-8215291
                // Fixes search links, but breaks external doc links which use modules. Fixed in JDK 12+.
                val noModuleDirectories = addBooleanOption("-no-module-directories")
                noModuleDirectories.value = true
            }
        }
    }
}

fun jdkApiDocs(javaVersion: Int): String = if (javaVersion >= 11) {
    "https://docs.oracle.com/en/java/javase/$javaVersion/docs/api"
} else {
    "https://docs.oracle.com/javase/$javaVersion/docs/api"
}
