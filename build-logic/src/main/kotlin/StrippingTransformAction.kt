import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.support.unzipTo
import org.gradle.kotlin.dsl.support.zipTo
import shadow.org.codehaus.plexus.util.SelectorUtils
import java.io.File
import java.nio.file.Files

abstract class StrippingTransformAction : TransformAction<StrippingTransformAction.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val excludes: SetProperty<String>
    }

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val output = outputs.file("stripped-" + input.name)
        input.copyTo(output)
        strip(output)
    }

    private fun strip(jar: File) {
        val dir = Files.createTempDirectory("strip-${System.currentTimeMillis()}").toFile()
        unzipTo(dir, jar)

        dir.walk()
            .forEach {
                val relative = it.relativeTo(dir)
                if (excluded(relative.path.replace('\\', '/'))) {
                    it.deleteRecursively()
                }
            }

        zipTo(jar, dir)

        dir.deleteRecursively()
    }

    private fun excluded(path: String): Boolean {
        for (exclude in parameters.excludes.get()) {
            if (SelectorUtils.matchPath(exclude, path, "/", true)) {
                return true
            }
        }
        return false
    }
}
