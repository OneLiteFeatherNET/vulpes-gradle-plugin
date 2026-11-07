package net.onelitefeather.vulpes.codegen;

import java.util.Objects;
import javax.lang.model.SourceVersion;

/**
 * The options of one generation.
 *
 * @param packageName the package of the generated classes, for example {@code net.example.content}
 * @param template    the template that selects the generated code
 * @throws IllegalArgumentException if the package name is not a valid Java package name
 */
public record CodegenOptions(String packageName, CodegenTemplate template) {

    public CodegenOptions {
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(template, "template");
        if (!SourceVersion.isName(packageName)) {
            throw new IllegalArgumentException("invalid package name '" + packageName + "'");
        }
    }
}
