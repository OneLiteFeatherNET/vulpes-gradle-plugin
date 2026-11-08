package net.onelitefeather.vulpes.gradle;

import java.nio.file.Path;
import java.util.Map;
import net.onelitefeather.vulpes.format.Release;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

/**
 * Prints what the local content merges over the published data JARs, warns about undeclared directories and enforces
 * {@code vulpes.local.forbid}. Runs before generation on every build, so that a local override is never silent.
 */
@UntrackedTask(because = "Prints the local override notice on every build; it reads the data JARs and two small files per content project.")
public abstract class VulpesLocalContentTask extends DefaultTask {

    /** The Vulpes data JARs of the declared content projects. */
    @Classpath
    public abstract ConfigurableFileCollection getDataJars();

    /** Artifact id by data JAR file name. */
    @Input
    public abstract MapProperty<String, String> getArtifactIdsByFileName();

    /** The local content directory, {@code <projectDir>/<localContentDirectory>}. */
    @Internal
    public abstract DirectoryProperty getLocalContentRoot();

    /** Declared coordinate {@code group:module:version} by declared content project. Its keys are the declared projects. */
    @Input
    public abstract MapProperty<String, String> getDeclaredCoordinatesByArtifactId();

    /** Reason by declared content project whose data JAR did not resolve. */
    @Input
    public abstract MapProperty<String, String> getUnresolvedReasons();

    /** Whether a local override of a declared content project fails the build, from {@code vulpes.local.forbid}. */
    @Input
    public abstract Property<Boolean> getForbid();

    @TaskAction
    public void notice() {
        Path root = getLocalContentRoot().get().getAsFile().toPath();
        DataJarReader.Content content = DataJarReader.readAll(root, getDataJars(), getArtifactIdsByFileName().get(),
                getDeclaredCoordinatesByArtifactId().get(), getUnresolvedReasons().get(), getForbid().get());
        for (DataJarReader.Override override : content.overrides()) {
            ContentMerge.Merged merged = override.merged();
            getLogger().lifecycle("vulpes local content: {} {} is merged over published {} from {}: {} replaced, {} added; {}",
                    override.project(), merged.content().release().version(), override.publishedVersion(),
                    override.directory(), merged.replaced(), merged.added(), base(merged.content().release().base()));
        }
        for (LocalContent.Served served : content.localOnly()) {
            getLogger().lifecycle("vulpes local content: {} {}: no published release used, local content only; could not resolve {}: {}; {}",
                    served.project(), served.content().release().version(), served.coordinate(),
                    served.unresolvedReason(), base(served.content().release().base()));
        }
        for (ContentMerge.MissingAsset missing : content.missingAssets()) {
            getLogger().warn("vulpes local content: {}", missing.message());
        }
        for (String ignored : content.ignored()) {
            getLogger().warn("vulpes local content: ignoring {}, no vulpes(...) dependency declares this content project",
                    ignored);
        }
    }

    private static String base(Release.Base base) {
        if (base == null) {
            return "no base recorded";
        }
        return "base " + base.repository() + " branch " + base.branch() + " commit " + base.commit();
    }
}
