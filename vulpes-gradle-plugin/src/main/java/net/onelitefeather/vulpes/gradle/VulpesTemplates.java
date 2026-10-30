package net.onelitefeather.vulpes.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import net.onelitefeather.vulpes.codegen.Template;
import net.onelitefeather.vulpes.codegen.Templates;
import org.gradle.api.GradleException;

/**
 * Selects the templates by name. The library versions come from the resource that the build of the plugin writes.
 */
final class VulpesTemplates {

    /** The template of a build that does not set {@code vulpes.template}. */
    static final String DEFAULT = "minestom";

    private static final String VERSIONS_RESOURCE = "versions.properties";

    private VulpesTemplates() {
    }

    /**
     * Returns the template of the given name.
     *
     * @throws GradleException if no template has this name; the message lists the known names
     */
    static Template select(String name) {
        Map<String, Template> templates = all();
        Template template = templates.get(name);
        if (template == null) {
            throw new GradleException("unknown vulpes.template '" + name + "', known templates: "
                    + String.join(", ", templates.keySet()));
        }
        return template;
    }

    /**
     * All templates by name, sorted by name.
     */
    static Map<String, Template> all() {
        Properties versions = versions();
        return Templates.all(require(versions, "vulpes-base"), require(versions, "adventure-key"));
    }

    private static Properties versions() {
        try (InputStream in = VulpesTemplates.class.getResourceAsStream(VERSIONS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("the Vulpes plugin has no " + VERSIONS_RESOURCE);
            }
            Properties versions = new Properties();
            versions.load(in);
            return versions;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + VERSIONS_RESOURCE, e);
        }
    }

    private static String require(Properties versions, String key) {
        return Objects.requireNonNull(versions.getProperty(key), () -> "versions.properties has no " + key);
    }
}
