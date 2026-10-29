package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ItemGeneratorTest {

    @Test
    void buildsItemWithMaterialAmountAndRawComponents() {
        String source = ItemGenerator.generate(TestContents.CONTEXT, List.of(TestContents.item("cygnus:iron_sword",
                "minecraft:iron_sword", 1, Map.of("minecraft:lore", "[{\"text\":\"Zeile 1\"}]")))).source();

        assertTrue(source.contains("public static final CustomItem IRON_SWORD = CustomItem.of(CygnusItemKeys.IRON_SWORD, "
                + "ItemComponents.apply(ItemStack.builder(Material.fromKey(\"minecraft:iron_sword\")).amount(1), "
                + "\"minecraft:lore\", \"[{\\\"text\\\":\\\"Zeile 1\\\"}]\").build());"), "item constant:\n" + source);
    }

    @Test
    void passesComponentsSortedByComponentId() {
        String source = ItemGenerator.generate(TestContents.CONTEXT, List.of(TestContents.item("cygnus:sword",
                "minecraft:iron_sword", 1, Map.of("minecraft:lore", "[]", "minecraft:damage", "3")))).source();

        assertTrue(source.indexOf("minecraft:damage") < source.indexOf("minecraft:lore"),
                "components sorted by id:\n" + source);
    }

    @Test
    void aliasIsDeprecatedAndPointsToCurrentConstant() {
        String source = ItemGenerator.generate(TestContents.CONTEXT, List.of(TestContents.item("cygnus:iron_sword",
                "minecraft:iron_sword", 1, Map.of(), "cygnus:old_sword"))).source();

        assertTrue(source.matches("(?s).*@Deprecated\\(\\s*forRemoval = true\\s*\\).*"), "alias deprecated:\n" + source);
        assertTrue(source.contains("public static final CustomItem OLD_SWORD = IRON_SWORD;"), "alias target:\n" + source);
        assertTrue(source.contains("Renamed to {@link #IRON_SWORD}."), "alias javadoc:\n" + source);
    }

    @Test
    void listsEntriesInSnapshotOrderWithoutAliases() {
        String source = ItemGenerator.generate(TestContents.CONTEXT, List.of(
                TestContents.item("cygnus:ration", "minecraft:bread", 16, Map.of()),
                TestContents.item("cygnus:iron_sword", "minecraft:iron_sword", 1, Map.of(), "cygnus:old_sword"))).source();

        assertTrue(source.contains("public static final List<CustomItem> ALL = List.of(RATION, IRON_SWORD);"),
                "ALL in snapshot order without aliases:\n" + source);
    }

    @Test
    void emptyTypeHasEmptyList() {
        String source = ItemGenerator.generate(TestContents.CONTEXT, List.of()).source();

        assertTrue(source.contains("public static final List<CustomItem> ALL = List.of();"), "empty ALL:\n" + source);
        assertFalse(source.contains("CustomItem.of("), "no constants for an empty type:\n" + source);
    }

    @Test
    void classIsNamedAfterProjectAndType() {
        GeneratedSource source = ItemGenerator.generate(TestContents.CONTEXT, List.of());

        assertEquals("CygnusItems", source.className(), "class name");
    }
}
