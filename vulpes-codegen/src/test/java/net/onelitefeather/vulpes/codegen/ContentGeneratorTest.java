package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class ContentGeneratorTest {

    @Test
    void implementsVulpesContentWithSingletonAndListsOfEachType() {
        String source = ContentGenerator.generate(TestContents.CONTEXT, TestContents.snapshot().build()).source();

        assertTrue(source.contains("public final class CygnusContent implements VulpesContent {"), "class:\n" + source);
        assertTrue(source.contains("public static final CygnusContent INSTANCE = new CygnusContent();"), "singleton:\n" + source);
        assertTrue(source.contains("return CygnusItems.ALL;"), "items:\n" + source);
        assertTrue(source.contains("return CygnusAttributes.ALL;"), "attributes:\n" + source);
        assertTrue(source.contains("return CygnusSounds.ALL;"), "sounds:\n" + source);
        assertTrue(source.contains("return CygnusFontSymbols.ALL;"), "font symbols:\n" + source);
        assertTrue(source.contains("return CygnusAdvancements.ALL;"), "advancements:\n" + source);
    }

    @Test
    void aliasesMapAliasKeyToCurrentKeyAcrossTypes() {
        var snapshot = TestContents.snapshot()
                .item(TestContents.item("cygnus:iron_sword", "minecraft:iron_sword", 1, Map.of(), "cygnus:old_sword"))
                .build();

        String source = ContentGenerator.generate(TestContents.CONTEXT, snapshot).source();

        assertTrue(source.contains("Map.entry(Key.key(\"cygnus\", \"old_sword\"), CygnusItemKeys.IRON_SWORD)"),
                "alias to current key:\n" + source);
    }

    @Test
    void withoutAliasesDeclaresEmptyMap() {
        String source = ContentGenerator.generate(TestContents.CONTEXT, TestContents.empty()).source();

        assertTrue(source.contains("return Map.ofEntries();"), "empty alias map:\n" + source);
    }
}
