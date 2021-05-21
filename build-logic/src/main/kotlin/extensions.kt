import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import java.io.ByteArrayOutputStream

fun Project.configurePublication(configurer: MavenPublication.() -> Unit) {
    extensions.configure<PublishingExtension> {
        publications.named<MavenPublication>("maven") {
            apply(configurer)
        }
    }
}

fun Project.lastCommitHash(): String = ByteArrayOutputStream().apply {
    exec {
        commandLine = listOf("git", "rev-parse", "HEAD")
        standardOutput = this@apply
    }
}.toString(Charsets.UTF_8.name()).trim().substring(0, 7)
