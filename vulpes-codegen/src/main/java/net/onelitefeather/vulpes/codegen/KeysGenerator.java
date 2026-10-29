package net.onelitefeather.vulpes.codegen;

import static com.palantir.javapoet.TypeSpec.classBuilder;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;

/**
 * Writes the class of the key constants of one entity type: a {@code Key} per entry, and a deprecated constant per
 * renamed key.
 */
final class KeysGenerator {

    static final ClassName KEY = ClassName.get("net.kyori.adventure.key", "Key");

    private KeysGenerator() {
    }

    /**
     * Generates {@code <Project><Type>Keys} for the entries of one entity type.
     *
     * @throws CodegenException if the keys cannot be turned into constants
     */
    static GeneratedSource generate(GenerationContext context, EntityType type, List<EntryKeys> entries) {
        ConstantTable table = ConstantTable.of(context.namespace(), entries);
        TypeSpec.Builder keys = classBuilder(context.prefix() + type.keysSuffix())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(JavaSources.classJavadoc(context, "Keys of the " + type.label()
                        + " of the Vulpes content project {@code " + context.project() + "}."));
        for (ConstantTable.Entry entry : table.entries()) {
            keys.addField(FieldSpec.builder(KEY, entry.name(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer(keyInitializer(entry.key()))
                    .build());
        }
        for (ConstantTable.Alias alias : table.aliases()) {
            keys.addField(FieldSpec.builder(KEY, alias.name(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .addAnnotation(deprecatedForRemoval())
                    .addJavadoc("Renamed to {@link #$L}.", alias.target().name())
                    .initializer(keyInitializer(alias.key()))
                    .build());
        }
        keys.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        return JavaSources.render(context.packageName(), keys.build());
    }

    /**
     * The initializer {@code Key.key("namespace", "path")} of a key.
     */
    static CodeBlock keyInitializer(String key) {
        return CodeBlock.of("$T.key($L, $L)", KEY, JavaLiterals.string(ConstantTable.namespaceOf(key)),
                JavaLiterals.string(ConstantTable.pathOf(key)));
    }

    /**
     * The annotation {@code @Deprecated(forRemoval = true)}.
     */
    static AnnotationSpec deprecatedForRemoval() {
        return AnnotationSpec.builder(Deprecated.class).addMember("forRemoval", "$L", true).build();
    }
}
