import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.toothpick.shadow.ModifiedLog4j2PluginsCacheFileTransformer
import xyz.jpenilla.toothpick.shadow.ToothpickRelocator
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("paper.common-conventions")
    id("com.github.johnrengelman.shadow")
}

description = "Paper Minecraft Server"

fun paperVersion(): String {
    return "git-Paper-\"${rootProject.lastCommitHash()}\"" // todo
}

// All of this artifact transformation garbage is because shadow does not support filtering
// the contents of individual dependencies in the same way that maven-shade-plugin does.
val artifactType = Attribute.of("artifactType", String::class.java)
val stripped = Attribute.of("stripped", Boolean::class.javaObjectType)
dependencies {
    attributesSchema {
        attribute(stripped)
    }
    artifactTypes.getByName("jar") {
        attributes.attribute(stripped, false)
    }
    registerTransform(StrippingTransformAction::class) {
        from.attribute(stripped, false).attribute(artifactType, "jar")
        to.attribute(stripped, true).attribute(artifactType, "jar")
        parameters {
            excludes.addAll(
                "com/google/common/**",
                "com/google/gson/**",
                "com/google/thirdparty/**",
                "io/netty/**",
                "META-INF/native/libnetty*",
                "com/mojang/brigadier/**",
                "META-INF/MANIFEST.MF",
                "com/mojang/authlib/yggdrasil/YggdrasilGameProfileRepository.class",
                "com/mojang/datafixers/util/Either*",
                "org/apache/logging/log4j/**",
                "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"
            )
        }
    }
}

val minecraftServer: Configuration by configurations.creating {
    attributes.attribute(stripped, true)
}

dependencies {
    minecraftServer("io.papermc:minecraft-server:1.16.5-SNAPSHOT")
}

afterEvaluate {
    minecraftServer.files.forEach {
        dependencies {
            val stripped = files(it)
            compileOnly(stripped)
            runtimeOnly(stripped)
            testImplementation(stripped)
        }
    }
}

dependencies {
    implementation(projects.paperApi)
    implementation(projects.paperMojangapi)

    implementation("net.minecrell:terminalconsoleappender:1.2.0")

    val log4jVersion = "2.11.2"
    implementation(platform(setOf("org.apache.logging.log4j", "log4j-bom", log4jVersion).joinToString(":")))
    implementation("org.apache.logging.log4j:log4j-core")
    annotationProcessor(group = "org.apache.logging.log4j", name = "log4j-core", version = log4jVersion)
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-iostreams")

    implementation("org.ow2.asm:asm:9.1")
    implementation("co.aikar:cleaner:1.0-SNAPSHOT")
    implementation("io.netty:netty-all:4.1.50.Final")
    implementation("com.googlecode.json-simple:json-simple:1.1.1") {
        // This includes junit transitively for whatever reason
        isTransitive = false
    }

    runtimeOnly("org.jline:jline-terminal-jansi:3.12.1")
    runtimeOnly("com.lmax:disruptor:3.4.2")
    runtimeOnly("org.xerial:sqlite-jdbc:3.34.0")
    runtimeOnly("mysql:mysql-connector-java:8.0.23")
    runtimeOnly("org.apache.maven:maven-resolver-provider:3.8.1")
    runtimeOnly("org.apache.maven.resolver:maven-resolver-connector-basic:1.6.2")
    runtimeOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.6.2")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.hamcrest:hamcrest-library:1.3")
    testImplementation("io.github.classgraph:classgraph:4.8.47")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    test {
        exclude("org/bukkit/craftbukkit/inventory/ItemStack*Test.class")
    }
    val pom = project.buildDir.resolve("tmp/pom.xml")
    generatePomFileForMavenPublication {
        destination = pom
    }
    jar {
        archiveClassifier.set("unshaded")

        dependsOn(generatePomFileForMavenPublication)
        from(pom) {
            // for Paperclip install command
            into("META-INF/maven/io.papermc.paper/paper")
        }

        manifest.attributes(
            "Main-Class" to "org.bukkit.craftbukkit.Main",
            "Implementation-Title" to "CraftBukkit",
            "Implementation-Version" to paperVersion(),
            "Implementation-Vendor" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date()),
            "Specification-Title" to "Bukkit",
            "Specification-Version" to project.property(Constants.Properties.API_VERSION) as String,
            "Specification-Vendor" to "Bukkit Team",
            "Multi-Release" to true
        )
        sequenceOf("net", "com", "org").forEach { tld ->
            manifest.attributes(
                mapOf("Sealed" to true), "$tld/bukkit"
            )
        }
    }
    shadowJar {
        configurations.add(minecraftServer)
        archiveClassifier.set(null as String?)
        archiveFileName.set("${project.name}-${project.property(Constants.Properties.MINECRAFT_VERSION)}.jar")

        transform(ModifiedLog4j2PluginsCacheFileTransformer::class.java)
        mergeServiceFiles()

        exclude("META-INF/services/javax.annotation.processing.Processor")

        if (!project.hasProperty(Constants.Properties.NO_RELOCATE)) {
            configureRelocations()
        }
    }

    fun registerRunTask(name: String, block: JavaExec.() -> Unit): TaskProvider<JavaExec> =
        register<JavaExec>(name) {
            workingDir = rootProject.projectDir.resolve("run")
            doFirst {
                if (!workingDir.exists()) workingDir.mkdir()
            }
            standardInput = System.`in`
            args("--nogui")
            systemProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", true)
            block(this)
        }

    registerRunTask("runServer") {
        description = "Spin up a test server"
        dependsOn(shadowJar)
        classpath(shadowJar.map { it.archiveFile })
    }

    registerRunTask("runDevServer") {
        description = "Spin up a non-relocated test server"
        classpath = project.convention.getPlugin(JavaPluginConvention::class.java)
            .sourceSets.getByName("main").runtimeClasspath
        main = "org.bukkit.craftbukkit.Main"
        systemProperty("disable.watchdog", true)
    }
}

configurePublication {
    artifact(tasks.shadowJar)
    // todo: the sources jar is not exactly correct... https://github.com/johnrengelman/shadow/issues/41
    artifact(tasks.sourcesJar)
}

fun ShadowJar.configureRelocations() {
    val destination = "org.bukkit.craftbukkit.libs"
    fun relocate(pkg: String) {
        relocate(pkg, "$destination.$pkg")
    }
    relocate("jline")
    relocate("org.apache.commons.codec")
    relocate("org.apache.commons.io")
    relocate("org.apache.commons.lang3")
    relocate("org.apache.http")
    relocate("org.apache.maven")
    relocate("org.codehaus.plexus")
    relocate("org.eclipse.aether")
    relocate("org.eclipse.sisu")
    relocate("org.objectweb.asm")

    val nmsPackage = project.property(Constants.Properties.NMS_PACKAGE)
    relocate("org.bukkit.craftbukkit", "org.bukkit.craftbukkit.v$nmsPackage") {
        exclude("org.bukkit.craftbukkit.Main*")
    }
    val mojangMath = ToothpickRelocator(
        "com/mojang/math/(.+/)*(.*)",
        "net/minecraft/server/v$nmsPackage/\$2",
        true
    )
    relocate(mojangMath)
    val nms = ToothpickRelocator(
        "net/minecraft/(.+/)*(.*)",
        "net/minecraft/server/v$nmsPackage/\$2",
        true,
        excludes = listOf("net/minecraft/data/Main*")
    )
    relocate(nms)
}
