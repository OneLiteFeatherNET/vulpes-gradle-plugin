package net.onelitefeather.vulpes.codegen;

import static com.palantir.javapoet.TypeSpec.classBuilder;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/**
 * Writes the class of the built entries of one type: a constant per entry, a deprecated constant per renamed key that
 * refers to the current constant, and {@code ALL} with the entries in snapshot order.
 */
final class EntryWriter {

    private EntryWriter() {
    }

    /**
     * One constant of the class.
     *
     * @param name       the constant name, the same as the key constant
     * @param expression the initializer that builds the entry
     */
    record Built(String name, CodeBlock expression) {
    }

    /**
     * Writes {@code <Project><Type>s}.
     *
     * @param entryType the type of the constants, for example {@code CustomItem}
     * @param table     the constant names of the entries and the aliases
     * @param built     the entries in snapshot order
     */
    static GeneratedSource write(GenerationContext context, EntityType type, ClassName entryType, ConstantTable table,
                                 List<Built> built) {
        TypeSpec.Builder entries = classBuilder(context.prefix() + type.entriesSuffix())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(JavaSources.classJavadoc(context, "Entries of type " + type.label()
                        + " of the Vulpes content project {@code " + context.project() + "}."));
        for (Built constant : built) {
            entries.addField(FieldSpec.builder(entryType, constant.name(), Modifier.PUBLIC, Modifier.STATIC,
                    Modifier.FINAL).initializer(constant.expression()).build());
        }
        for (ConstantTable.Alias alias : table.aliases()) {
            entries.addField(FieldSpec.builder(entryType, alias.name(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .addAnnotation(KeysGenerator.deprecatedForRemoval())
                    .addJavadoc("Renamed to {@link #$L}.", alias.target().name())
                    .initializer("$L", alias.target().name())
                    .build());
        }
        String names = built.stream().map(Built::name).collect(Collectors.joining(", "));
        CodeBlock all = built.isEmpty() ? CodeBlock.of("$T.of()", ClassName.get(List.class))
                : CodeBlock.of("$T.of($L)", ClassName.get(List.class), names);
        entries.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), entryType), "ALL",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer(all).build());
        return JavaSources.render(context.packageName(), entries.build());
    }
}
