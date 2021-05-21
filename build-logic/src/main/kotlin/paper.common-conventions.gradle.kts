plugins {
    `java-library`
    `maven-publish`
}

group = property(Constants.Properties.GROUP) as String
version = property(Constants.Properties.API_VERSION) as String

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://libraries.minecraft.net")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.md-5.net/content/repositories/releases/")
    mavenLocal() {
        content { includeModule("io.papermc", "minecraft-server") }
    }
}

java {
    val javaTarget = property(Constants.Properties.JAVA_TARGET).toString().toInt()
    targetCompatibility = JavaVersion.toVersion(javaTarget)
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    withSourcesJar()
}

publishing.publications.create<MavenPublication>("maven") {
    pom {
        name.set(project.name)
        description.set(project.description)
        url.set(property(Constants.Properties.WEBSITE) as String)
    }
}

tasks {
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}
