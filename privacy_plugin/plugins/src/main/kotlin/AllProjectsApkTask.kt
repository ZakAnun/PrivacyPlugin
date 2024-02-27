import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class AllProjectsApkTask: DefaultTask() {
    @get:InputFiles
    abstract val inputDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val output = outputFile.get().asFile
        java.nio.file.Files.deleteIfExists(output.toPath())

        println("output.toPath == ${output.toPath()}")

        // inputDirectories contains a list of directories.
        //
        // Each directory contains the APKs for a particular module. We only
        // selected the RELEASE variant (see the CustomSettings.kt for the
        // variant selection).
        inputDirectories.get()
            .forEach {
                // Each directory contains one to many APKs in case of
                // multi-apk builds.
                // When AGP produces more than one file in a directory,
                // it also produces a small json file that indicates each
                // file particular attributes.
                // In this case, I am only interested at getting all the APKs
                // so I ignore this json file and store the APK file path
                // in my result file.
                it.asFile.walkTopDown()
                    .filter { it.name.endsWith(".apk") }
                    .forEach {
                        output.appendText("${it.absolutePath}\n")
                        output.appendText("append a output path, value == ${output.toPath()}")
                    }
            }
    }
}