package net.onelitefeather.vulpes.codegen;

import static com.palantir.javapoet.TypeSpec.classBuilder;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import net.onelitefeather.vulpes.format.Snapshot;

/**
 * Writes {@code <Project>Content}, the {@code VulpesContent} that hands the generated lists and the alias map to
 * {@code Vulpes.bootstrap}.
 */
final class ContentGenerator {

    private static final ClassName VULPES_CONTENT = ClassName.get("net.onelitefeather.vulpes", "VulpesContent");
    private static final ClassName CUSTOM_ITEM = ClassName.get("net.onelitefeather.vulpes.item", "CustomItem");
    private static final ClassName CUSTOM_ATTRIBUTE = ClassName.get("net.onelitefeather.vulpes.attribute", "CustomAttribute");
    private static final ClassName CUSTOM_SOUND = ClassName.get("net.onelitefeather.vulpes.sound", "CustomSound");
    private static final ClassName CUSTOM_FONT_SYMBOL = ClassName.get("net.onelitefeather.vulpes.font", "CustomFontSymbol");
    private static final ClassName CUSTOM_ADVANCEMENT = ClassName.get("net.onelitefeather.vulpes.advancement",
            "CustomAdvancement");
    private static final ClassName MAP = ClassName.get(Map.class);
    private static final ClassName LIST = ClassName.get(List.class);

    private ContentGenerator() {
    }

    /**
     * Generates {@code <Project>Content}. The alias map is sorted by the alias key.
     *
     * @throws CodegenException if the keys of one type cannot be turned into constants
     */
    static GeneratedSource generate(GenerationContext context, Snapshot snapshot) {
        ClassName content = context.className("Content");
        TypeSpec.Builder type = classBuilder(content.simpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(VULPES_CONTENT)
                .addJavadoc(JavaSources.classJavadoc(context, "Content of the Vulpes content project {@code "
                        + context.project() + "}, registered with {@code Vulpes.bootstrap}."))
                .addField(FieldSpec.builder(content, "INSTANCE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", content)
                        .build())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        list(context, type, "items", CUSTOM_ITEM, EntityType.ITEMS);
        list(context, type, "attributes", CUSTOM_ATTRIBUTE, EntityType.ATTRIBUTES);
        list(context, type, "sounds", CUSTOM_SOUND, EntityType.SOUNDS);
        list(context, type, "fontSymbols", CUSTOM_FONT_SYMBOL, EntityType.FONTS);
        list(context, type, "advancements", CUSTOM_ADVANCEMENT, EntityType.ADVANCEMENTS);
        type.addMethod(aliases(context, snapshot));
        return JavaSources.render(context.packageName(), type.build());
    }

    private static void list(GenerationContext context, TypeSpec.Builder type, String method, ClassName entryType,
                             EntityType entityType) {
        type.addMethod(MethodSpec.methodBuilder(method)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(LIST, entryType))
                .addStatement("return $T.ALL", context.className(entityType.entriesSuffix()))
                .build());
    }

    private static MethodSpec aliases(GenerationContext context, Snapshot snapshot) {
        List<Alias> aliases = new ArrayList<>();
        collect(context, snapshot, EntityType.ITEMS, snapshot.items().stream().map(EntryKeys::of).toList(), aliases);
        collect(context, snapshot, EntityType.ATTRIBUTES, snapshot.attributes().stream().map(EntryKeys::of).toList(),
                aliases);
        collect(context, snapshot, EntityType.FONTS, snapshot.fonts().stream().map(EntryKeys::of).toList(), aliases);
        collect(context, snapshot, EntityType.SOUNDS, snapshot.sounds().stream().map(EntryKeys::of).toList(), aliases);
        collect(context, snapshot, EntityType.ADVANCEMENTS,
                snapshot.advancements().stream().map(EntryKeys::of).toList(), aliases);
        aliases.sort(Comparator.comparing(alias -> alias.alias().key()));

        CodeBlock.Builder entries = CodeBlock.builder().add("return $T.ofEntries(", MAP);
        for (int i = 0; i < aliases.size(); i++) {
            Alias alias = aliases.get(i);
            if (i > 0) {
                entries.add(", ");
            }
            entries.add("$T.entry($L, $T.$L)", MAP, KeysGenerator.keyInitializer(alias.alias().key()), alias.keys(),
                    alias.alias().target().name());
        }
        return MethodSpec.methodBuilder("aliases")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(MAP, KeysGenerator.KEY, KeysGenerator.KEY))
                .addStatement("$L", entries.add(")").build())
                .build();
    }

    private static void collect(GenerationContext context, Snapshot snapshot, EntityType type,
                                List<EntryKeys> entries, List<Alias> target) {
        ConstantTable table = ConstantTable.of(snapshot.namespace(), entries);
        ClassName keys = context.className(type.keysSuffix());
        for (ConstantTable.Alias alias : table.aliases()) {
            target.add(new Alias(alias, keys));
        }
    }

    private record Alias(ConstantTable.Alias alias, ClassName keys) {
    }
}
