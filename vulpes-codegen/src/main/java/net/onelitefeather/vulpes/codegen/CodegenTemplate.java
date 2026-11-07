package net.onelitefeather.vulpes.codegen;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The templates of the generator, by name. {@link #MINESTOM} generates the full code against vulpes-base,
 * {@link #KEYS} generates the key constants only and needs adventure-key.
 */
public enum CodegenTemplate {

    /** The full code, built against vulpes-base. Named {@code minestom}. */
    MINESTOM,

    /** The key constants only, built against adventure-key. Named {@code keys}. */
    KEYS;

    /**
     * Returns the template of the given name, for example {@code minestom}.
     *
     * @throws IllegalArgumentException if no template has this name; the message names it and lists the known names
     */
    public static CodegenTemplate fromName(String name) {
        Objects.requireNonNull(name, "name");
        for (CodegenTemplate template : values()) {
            if (template.templateName().equals(name)) {
                return template;
            }
        }
        String known = Arrays.stream(values()).map(CodegenTemplate::templateName).sorted()
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("unknown template '" + name + "', known templates: " + known);
    }

    private String templateName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
