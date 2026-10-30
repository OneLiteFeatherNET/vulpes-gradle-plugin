package net.onelitefeather.vulpes.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import java.util.List;
import java.util.stream.Collectors;
import net.onelitefeather.vulpes.format.Item;

/**
 * Writes the class of the custom items: one {@code CustomItem} per entry, built from its material, amount and raw
 * vanilla components.
 */
final class ItemGenerator {

    private static final ClassName CUSTOM_ITEM = ClassName.get("net.onelitefeather.vulpes.item", "CustomItem");
    private static final ClassName ITEM_COMPONENTS = ClassName.get("net.onelitefeather.vulpes.item", "ItemComponents");
    private static final ClassName ITEM_STACK = ClassName.get("net.minestom.server.item", "ItemStack");
    private static final ClassName MATERIAL = ClassName.get("net.minestom.server.item", "Material");

    private ItemGenerator() {
    }

    /**
     * Generates {@code <Project>Items}.
     *
     * @throws CodegenException if two keys share a constant name or a key is malformed
     */
    static GeneratedSource generate(GenerationContext context, List<Item> items) {
        ConstantTable table = ConstantTable.of(context.namespace(), items.stream().map(EntryKeys::of).toList());
        ClassName keys = context.className(EntityType.ITEMS.keysSuffix());
        List<EntryWriter.Built> built = items.stream()
                .map(item -> new EntryWriter.Built(table.entry(item.key()).name(), CodeBlock.of("$T.of($T.$L, $L)",
                        CUSTOM_ITEM, keys, table.entry(item.key()).name(), expression(item))))
                .toList();
        return EntryWriter.write(context, EntityType.ITEMS, CUSTOM_ITEM, table, built);
    }

    /**
     * {@code ItemComponents.apply(ItemStack.builder(Material.fromKey(...)).amount(n), "id", "json", ...).build()}; the
     * components are passed in the order of their component id.
     */
    private static CodeBlock expression(Item item) {
        String components = item.components().entrySet().stream()
                .map(component -> ", " + JavaLiterals.string(component.getKey())
                        + ", " + JavaLiterals.string(component.getValue()))
                .collect(Collectors.joining());
        return CodeBlock.of("$T.apply($T.builder($T.fromKey($L)).amount($L)$L).build()", ITEM_COMPONENTS, ITEM_STACK,
                MATERIAL, JavaLiterals.string(item.material()), item.amount(), components);
    }
}
