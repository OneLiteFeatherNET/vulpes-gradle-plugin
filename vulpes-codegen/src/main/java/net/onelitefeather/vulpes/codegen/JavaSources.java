package net.onelitefeather.vulpes.codegen;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

/**
 * Renders the generated types. One writer for every generator, so all sources share indentation and line endings.
 */
final class JavaSources {

    private static final String INDENT = "    ";

    private JavaSources() {
    }

    /**
     * Renders the type as a source file in the given package.
     */
    static GeneratedSource render(String packageName, TypeSpec type) {
        String source = JavaFile.builder(packageName, type).indent(INDENT).skipJavaLangImports(true).build().toString();
        return new GeneratedSource(packageName, type.name(), source);
    }

    /**
     * Javadoc text of a generated class: the summary, then the provenance line.
     */
    static String classJavadoc(GenerationContext context, String summary) {
        return summary + "\n\n<p>" + context.provenance();
    }

    /**
     * Escapes text for a javadoc comment, so it cannot end the comment or start a tag.
     */
    static String javadocText(String text) {
        return text.replace("*/", "*&#47;").replace("@", "&#64;");
    }
}
