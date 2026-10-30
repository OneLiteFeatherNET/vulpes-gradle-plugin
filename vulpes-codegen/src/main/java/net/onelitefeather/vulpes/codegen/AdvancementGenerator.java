package net.onelitefeather.vulpes.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import java.util.List;
import net.onelitefeather.vulpes.format.Advancement;

/**
 * Writes the class of the custom advancements. Titles and descriptions are the component JSON of the contract, the
 * parent refers to the constant of an advancement of the same content, or to its key otherwise.
 */
final class AdvancementGenerator {

    private static final ClassName CUSTOM_ADVANCEMENT = ClassName.get("net.onelitefeather.vulpes.advancement",
            "CustomAdvancement");
    private static final ClassName ITEM_STACK = ClassName.get("net.minestom.server.item", "ItemStack");
    private static final ClassName MATERIAL = ClassName.get("net.minestom.server.item", "Material");
    private static final ClassName FRAME_TYPE = ClassName.get("net.minestom.server.advancements", "FrameType");
    private static final ClassName GSON = ClassName.get("net.kyori.adventure.text.serializer.gson",
            "GsonComponentSerializer");

    private AdvancementGenerator() {
    }

    /**
     * Generates {@code <Project>Advancements}.
     *
     * @throws CodegenException if two keys share a constant name, a key is malformed or a value is not finite
     */
    static GeneratedSource generate(GenerationContext context, List<Advancement> advancements) {
        ConstantTable table = ConstantTable.of(context.namespace(),
                advancements.stream().map(EntryKeys::of).toList());
        ClassName keys = context.className(EntityType.ADVANCEMENTS.keysSuffix());
        List<EntryWriter.Built> built = advancements.stream().map(advancement -> {
            String name = table.entry(advancement.key()).name();
            return new EntryWriter.Built(name, builder(advancement, keys, name, table));
        }).toList();
        return EntryWriter.write(context, EntityType.ADVANCEMENTS, CUSTOM_ADVANCEMENT, table, built);
    }

    private static CodeBlock builder(Advancement advancement, ClassName keys, String name, ConstantTable table) {
        CodeBlock.Builder code = CodeBlock.builder().add("$T.builder($T.$L)", CUSTOM_ADVANCEMENT, keys, name);
        if (advancement.parent() != null) {
            ConstantTable.Entry parent = table.entry(advancement.parent());
            CodeBlock parentKey = parent != null ? CodeBlock.of("$T.$L", keys, parent.name())
                    : KeysGenerator.keyInitializer(advancement.parent());
            code.add(".parent($L)", parentKey);
        }
        return code.add(".title($L)", component(advancement.title()))
                .add(".description($L)", component(advancement.description()))
                .add(".icon($T.of($T.fromKey($L)))", ITEM_STACK, MATERIAL, JavaLiterals.string(advancement.icon()))
                .add(".frame($T.$L)", FRAME_TYPE, advancement.frame().name())
                .add(".x($L).y($L)", JavaLiterals.floatValue(advancement.x()), JavaLiterals.floatValue(advancement.y()))
                .add(".showToast($L).hidden($L)", advancement.showToast(), advancement.hidden())
                .add(".background($L)", advancement.background() == null ? "null"
                        : JavaLiterals.string(advancement.background()))
                .add(".build()")
                .build();
    }

    private static CodeBlock component(String json) {
        return CodeBlock.of("$T.gson().deserialize($L)", GSON, JavaLiterals.string(json));
    }
}
