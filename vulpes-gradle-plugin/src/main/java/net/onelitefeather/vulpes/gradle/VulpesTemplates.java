package net.onelitefeather.vulpes.gradle;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import net.onelitefeather.vulpes.codegen.CodegenTemplate;
import org.gradle.api.GradleException;

/**
 * Selects the templates by name.
 */
final class VulpesTemplates {

    /** The template of a build that does not set {@code vulpes.template}. */
    static final String DEFAULT = "minestom";

    private VulpesTemplates() {
    }

    /**
     * Returns the template of the given name.
     *
     * @throws GradleException if no template has this name; the message lists the known names
     */
    static CodegenTemplate select(String name) {
        try {
            return CodegenTemplate.fromName(name);
        } catch (IllegalArgumentException e) {
            String known = Arrays.stream(CodegenTemplate.values())
                    .map(template -> template.name().toLowerCase(Locale.ROOT))
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new GradleException("unknown vulpes.template '" + name + "', known templates: " + known, e);
        }
    }
}
