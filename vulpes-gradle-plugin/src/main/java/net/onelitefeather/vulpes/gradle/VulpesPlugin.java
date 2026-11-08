package net.onelitefeather.vulpes.gradle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.onelitefeather.vulpes.codegen.VulpesCodegen;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactCollection;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginManager;
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
    static final String BASE_SCOPE = "vulpesDiffBaseScope";
    static final String BASE_CONFIGURATION = "vulpesDiffBase";
    static final String DIFF_TASK_NAME = "vulpesDiff";
    static final String DIFF_FROM_PROPERTY = "vulpes.diff.from";
    static final String DIFF_PROJECT_PROPERTY = "vulpes.diff.project";
    static final String REPORT_DIRECTORY = "reports/vulpes/diff";
    static final String LOCAL_TASK_NAME = "vulpesLocalContent";
    static final String LOCAL_FORBID_PROPERTY = "vulpes.local.forbid";
    static final String DEFAULT_LOCAL_CONTENT_DIRECTORY = "vulpes";

    @Override
    public void apply(Project project) {
        VulpesExtension extension = project.getExtensions().create(EXTENSION_NAME, VulpesExtension.class);
        extension.getTemplate().convention(VulpesTemplates.DEFAULT);
        extension.getLocalContentDirectory().convention(DEFAULT_LOCAL_CONTENT_DIRECTORY);
        Configuration vulpes = project.getConfigurations().dependencyScope(DEPENDENCY_SCOPE).get();
        Configuration vulpesData = project.getConfigurations().resolvable(DATA_CONFIGURATION, data -> {
            data.extendsFrom(vulpes);
            data.setTransitive(false);
            dataAttributes(project, data);
        }).get();
        // Lenient: a declared project whose data JAR does not resolve yields no file instead of failing the build.
        ArtifactView lenientData = vulpesData.getIncoming().artifactView(view -> view.lenient(true));
        ArtifactCollection lenientArtifacts = lenientData.getArtifacts();
        FileCollection lenientFiles = lenientData.getFiles();

        Provider<String> diffFrom = project.getProviders().gradleProperty(DIFF_FROM_PROPERTY);
        Provider<String> diffProject = project.getProviders().gradleProperty(DIFF_PROJECT_PROPERTY);
        Configuration vulpesDiffScope = project.getConfigurations().dependencyScope(BASE_SCOPE).get();
        vulpesDiffScope.withDependencies(dependencies -> {
            String from = diffFrom.getOrNull();
            String only = diffProject.getOrNull();
            if (from == null) {
                return;
            }
            for (Dependency declared : vulpes.getDependencies()) {
                if (only == null || only.equals(declared.getName())) {
                    dependencies.add(project.getDependencies()
                            .create(declared.getGroup() + ":" + declared.getName() + ":" + from));
                }
            }
        });
        Configuration vulpesDiffBase = project.getConfigurations().resolvable(BASE_CONFIGURATION, base -> {
            base.extendsFrom(vulpesDiffScope);
            base.setTransitive(false);
            dataAttributes(project, base);
        }).get();

        Provider<Directory> localRoot = project.getLayout().getProjectDirectory()
                .dir(extension.getLocalContentDirectory());
        Provider<Boolean> localForbid = project.getProviders().gradleProperty(LOCAL_FORBID_PROPERTY)
                .map(Boolean::parseBoolean).orElse(false);
        Provider<Map<String, String>> declaredCoordinates = declaredCoordinatesByArtifactId(vulpesData);
        Provider<Map<String, String>> unresolvedReasons = unresolvedReasonsByArtifactId(vulpesData);
        FileTree localFiles = project.fileTree(localRoot,
                tree -> tree.include("*/" + LocalContent.RELEASE_FILE, "*/" + LocalContent.SNAPSHOT_FILE));

        TaskProvider<VulpesLocalContentTask> localContent = project.getTasks().register(LOCAL_TASK_NAME,
                VulpesLocalContentTask.class, task -> {
                    task.setGroup("vulpes");
                    task.setDescription("Prints the local content that is merged over the published data JARs.");
                    task.getDataJars().from(lenientFiles);
                    task.getArtifactIdsByFileName().set(artifactIdsByFileName(lenientArtifacts));
                    task.getLocalContentRoot().set(localRoot);
                    task.getDeclaredCoordinatesByArtifactId().set(declaredCoordinates);
                    task.getUnresolvedReasons().set(unresolvedReasons);
                    task.getForbid().set(localForbid);
                });

        TaskProvider<VulpesGenerateTask> generate = project.getTasks().register(TASK_NAME, VulpesGenerateTask.class,
                task -> {
                    task.setGroup("vulpes");
                    task.setDescription("Generates Java sources from the Vulpes content data JARs.");
                    task.dependsOn(localContent);
                    task.getDataJars().from(lenientFiles);
                    task.getArtifactIdsByFileName().set(artifactIdsByFileName(lenientArtifacts));
                    task.getLocalContentRoot().set(localRoot);
                    task.getLocalContentFiles().from(localFiles);
                    task.getDeclaredCoordinatesByArtifactId().set(declaredCoordinates);
                    task.getUnresolvedReasons().set(unresolvedReasons);
                    task.getForbid().set(localForbid);
                    task.getTemplate().set(extension.getTemplate());
                    task.getPackageName().set(extension.getPackageName());
                    task.getPackages().set(project.provider(() -> packageOverrides(extension)));
                    task.getPluginVersion().set(pluginVersion());
                    task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir(GENERATED_DIRECTORY));
                });

        project.getTasks().register(DIFF_TASK_NAME, VulpesDiffTask.class, task -> {
            task.setGroup("vulpes");
            task.setDescription("Compares the content data JARs with -Pvulpes.diff.from=<version>.");
            task.getDataJars().from(lenientFiles);
            task.getArtifactIdsByFileName().set(artifactIdsByFileName(lenientArtifacts));
            task.getBaseDataJars().from(vulpesDiffBase);
            task.getBaseArtifactIdsByFileName().set(artifactIdsByFileName(vulpesDiffBase.getIncoming().getArtifacts()));
            task.getLocalContentRoot().set(localRoot);
            task.getLocalContentFiles().from(localFiles);
            task.getDeclaredCoordinatesByArtifactId().set(declaredCoordinates);
            task.getUnresolvedReasons().set(unresolvedReasons);
            task.getForbid().set(localForbid);
            task.getBaseVersion().set(diffFrom);
            task.getProjectFilter().set(diffProject);
            task.getReportDirectory().set(project.getLayout().getBuildDirectory().dir(REPORT_DIRECTORY));
        });

        project.getPluginManager().withPlugin("java", javaPlugin -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava()
                    .srcDir(generate.flatMap(VulpesGenerateTask::getOutputDirectory));

            // The runtime library of the selected template, resolved lazily so that the template can be set later.
            // With java-library it goes to api instead, see below. The plugins may be applied in any order, so the
            // check is made when the provider is realized, not here.
            DependencyHandler dependencies = project.getDependencies();
            PluginManager plugins = project.getPluginManager();
            Configuration implementation = project.getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
            implementation.getDependencies().addAllLater(extension.getTemplate().map(name -> plugins.hasPlugin("java-library")
                    ? List.<Dependency>of()
                    : runtimeLibrary(dependencies, name)));
        });

        // The api configuration exists only with java-library. Consumers of the module compile against the generated
        // types, so they need the runtime library too.
        project.getPluginManager().withPlugin("java-library", libraryPlugin -> {
            DependencyHandler dependencies = project.getDependencies();
            Configuration api = project.getConfigurations().getByName(JavaPlugin.API_CONFIGURATION_NAME);
            api.getDependencies().addAllLater(extension.getTemplate().map(name -> runtimeLibrary(dependencies, name)));
        });

        project.afterEvaluate(evaluated -> {
            if (!evaluated.getPlugins().hasPlugin(JavaPlugin.class)) {
                throw new GradleException("The Vulpes plugin requires the 'java' plugin; "
                        + "apply 'java' or 'java-library'.");
            }
        });
    }

    /** The runtime library of the template with the version the plugin was built for. */
    private static List<Dependency> runtimeLibrary(DependencyHandler dependencies, String template) {
        return VulpesCodegen.runtimeDependencies(VulpesTemplates.select(template)).stream()
                .map(dependency -> dependencies.create(dependency.group() + ":" + dependency.name() + ":"
                        + dependency.version()))
                .toList();
    }

    private static void dataAttributes(Project project, Configuration configuration) {
        configuration.getAttributes().attribute(Usage.USAGE_ATTRIBUTE,
                project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
        configuration.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE,
                project.getObjects().named(Category.class, Category.LIBRARY));
    }

    private static Provider<Map<String, String>> artifactIdsByFileName(ArtifactCollection artifacts) {
        return artifacts.getResolvedArtifacts().map(resolved -> {
            Map<String, String> artifactIds = new TreeMap<>();
            for (ResolvedArtifactResult artifact : resolved) {
                if (artifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier module) {
                    artifactIds.put(artifact.getFile().getName(), module.getModule());
                }
            }
            return artifactIds;
        });
    }

    /**
     * The declared content projects of the {@code vulpes} scope, read from the direct dependencies of the root
     * component, so that a declared project is known even when its data JAR does not resolve. A resolved project has
     * the version it resolved to, an unresolved one the version it was declared with.
     */
    private static Provider<Map<String, String>> declaredCoordinatesByArtifactId(Configuration vulpesData) {
        return vulpesData.getIncoming().getResolutionResult().getRootComponent().map(root -> {
            Map<String, String> coordinates = new TreeMap<>();
            for (DependencyResult dependency : root.getDependencies()) {
                if (dependency instanceof ResolvedDependencyResult resolved
                        && resolved.getSelected().getId() instanceof ModuleComponentIdentifier module) {
                    coordinates.put(module.getModule(),
                            module.getGroup() + ":" + module.getModule() + ":" + module.getVersion());
                } else if (dependency instanceof UnresolvedDependencyResult unresolved
                        && unresolved.getAttempted() instanceof ModuleComponentSelector module) {
                    coordinates.put(module.getModule(),
                            module.getGroup() + ":" + module.getModule() + ":" + module.getVersion());
                }
            }
            return coordinates;
        });
    }

    /** The reason by declared content project whose data JAR did not resolve, shortened to one line. */
    private static Provider<Map<String, String>> unresolvedReasonsByArtifactId(Configuration vulpesData) {
        return vulpesData.getIncoming().getResolutionResult().getRootComponent().map(root -> {
            Map<String, String> reasons = new TreeMap<>();
            for (DependencyResult dependency : root.getDependencies()) {
                if (dependency instanceof UnresolvedDependencyResult unresolved
                        && unresolved.getAttempted() instanceof ModuleComponentSelector module) {
                    Throwable failure = unresolved.getFailure();
                    reasons.put(module.getModule(),
                            LocalContent.shortReason(failure == null ? null : failure.getMessage()));
                }
            }
            return reasons;
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
