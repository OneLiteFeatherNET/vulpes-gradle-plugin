package net.onelitefeather.vulpes.gradle;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.onelitefeather.vulpes.format.ContentFormatException;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;

/**
 * Reads the Vulpes data JARs of a task's classpath, in file name order.
 */
final class DataJarReader {

    private DataJarReader() {
    }

    /**
     * Reads every data JAR and checks that its release belongs to the artifact the JAR was resolved from.
     *
     * @param jars the data JARs
     * @param artifactIds artifact id by data JAR file name
     * @return the releases, sorted by the file name of their JAR
     * @throws GradleException if a JAR has no artifact id, cannot be read or does not match its artifact
     */
    static List<ContentRelease> read(FileCollection jars, Map<String, String> artifactIds) {
        List<ContentRelease> releases = new ArrayList<>();
        for (File jar : jars.getFiles().stream().sorted(Comparator.comparing(File::getName)).toList()) {
            String artifactId = artifactIds.get(jar.getName());
            if (artifactId == null) {
                throw new GradleException(jar.getName() + ": no artifact id is known for this data JAR");
            }
            ContentRelease content;
            try {
                content = ContentReader.read(jar.toPath());
            } catch (ContentFormatException e) {
                throw new GradleException(e.getMessage(), e);
            }
            String project = content.release().project();
            if (!project.equals(artifactId)) {
                throw new GradleException(jar.getName() + ": release.json project '" + project
                        + "' does not match artifact '" + artifactId + "'");
            }
            releases.add(content);
        }
        return releases;
    }
}
