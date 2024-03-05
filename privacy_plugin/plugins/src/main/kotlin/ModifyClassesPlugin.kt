import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class ModifyClassesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Registers a callback on the application of the Android Application plugin.
        // This allows the CustomPlugin to work whether it's applied before or after
        // the Android Application plugin.
        project.plugins.withType(AppPlugin::class.java) {

            // Queries for the extension set by the Android Application plugin.
            // This is the second of two entry points into the Android Gradle plugin
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            // Registers a callback to be called, when a new variant is configured
            androidComponents.onVariants { variant ->
                val taskProvider = project.tasks.register<ModifyClassesTask>("${variant.name}ModifyClasses")

                // Register modify classes task
                variant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
                    .use(taskProvider)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        ModifyClassesTask::allJars,
                        ModifyClassesTask::allDirectories,
                        ModifyClassesTask::output
                    )
            }
        }
    }

}