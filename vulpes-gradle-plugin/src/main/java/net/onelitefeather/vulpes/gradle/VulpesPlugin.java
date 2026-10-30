package net.onelitefeather.vulpes.gradle;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin that generates Java code from Vulpes content releases.
 */
public class VulpesPlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = "vulpes";
    static final String DEPENDENCY_SCOPE = "vulpes";
    static final String DATA_CONFIGURATION = "vulpesData";
    static final String TASK_NAME = "vulpesGenerate";
    static final String GENERATED_DIRECTORY = "generated/sources/vulpes/java/main";

    @Override
    public void apply(Project project) {
        VulpesExtension extension = project.getExtensions().create(EXTENSION_NAME, VulpesExtension.class);
        Configuration vulpes = project.getConfigurations().dependencyScope(DEPENDENCY_SCOPE).get();
        Configuration vulpesData = project.getConfigurations().resolvable(DATA_CONFIGURATION, data -> {
            data.extendsFrom(vulpes);
            data.setTransitive(false);
            data.getAttributes().attribute(Usage.USAGE_ATTRIBUTE,
                    project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
            data.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE,
                    project.getObjects().named(Category.class, Category.LIBRARY));
        }).get();

        TaskProvider<VulpesGenerateTask> generate = project.getTasks().register(TASK_NAME, VulpesGenerateTask.class,
                task -> {
                    task.setGroup("vulpes");
                    task.setDescription("Generates Java sources from the Vulpes content data JARs.");
                    task.getDataJars().from(vulpesData);
                    task.getArtifactIdsByFileName().set(artifactIdsByFileName(vulpesData));
                    task.getPackageName().set(extension.getPackageName());
                    task.getPackages().set(project.provider(() -> packageOverrides(extension)));
                    task.getPluginVersion().set(pluginVersion());
                    task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir(GENERATED_DIRECTORY));
                });

        project.getPluginManager().withPlugin("java", javaPlugin -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava()
                    .srcDir(generate.flatMap(VulpesGenerateTask::getOutputDirectory));
        });

        project.afterEvaluate(evaluated -> {
            if (!evaluated.getPlugins().hasPlugin(JavaPlugin.class)) {
                throw new GradleException("The Vulpes plugin requires the 'java' plugin; "
                        + "apply 'java' or 'java-library'.");
            }
        });
    }

    private static Provider<Map<String, String>> artifactIdsByFileName(Configuration vulpesData) {
        return vulpesData.getIncoming().getArtifacts().getResolvedArtifacts().map(artifacts -> {
            Map<String, String> artifactIds = new TreeMap<>();
            for (ResolvedArtifactResult artifact : artifacts) {
                if (artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier module) {
                    artifactIds.put(artifact.getFile().getName(), module.getModule());
                }
            }
            return artifactIds;
        });
    }

    private static Map<String, String> packageOverrides(VulpesExtension extension) {
        Map<String, String> packages = new TreeMap<>();
        for (VulpesProjectSettings settings : extension.getProjects()) {
            String packageName = settings.getPackageName().getOrNull();
            if (packageName != null) {
                packages.put(settings.getName(), packageName);
            }
        }
        return packages;
    }

    private static String pluginVersion() {
        return Optional.ofNullable(VulpesPlugin.class.getPackage())
                .map(Package::getImplementationVersion)
                .orElse("development");
    }
}
