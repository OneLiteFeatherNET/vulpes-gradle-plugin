package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class AttributeGeneratorTest {

    @Test
    void buildsAttributeWithMinimumDefaultMaximumOrder() {
        String source = AttributeGenerator.generate(TestContents.CONTEXT, List.of(TestContents.attribute(
                "cygnus:crit_chance", "Krit-Chance", null, 0.0, 0.0, 1.0, true))).source();

        assertTrue(source.contains("public static final CustomAttribute CRIT_CHANCE = CustomAttribute.of("
                + "CygnusAttributeKeys.CRIT_CHANCE, \"Krit-Chance\", null, 0.0, 0.0, 1.0, true);"), "attribute:\n" + source);
    }

    @Test
    void passesTranslationKeyAndNonUnicodeDisplayNameEscaped() {
        String source = AttributeGenerator.generate(TestContents.CONTEXT, List.of(TestContents.attribute(
                "cygnus:strength", "Stärke", "attribute.cygnus.strength", 0.0, 1.0, 20.0, false))).source();

        assertTrue(source.contains("\"St\\u00e4rke\", \"attribute.cygnus.strength\", 0.0, 1.0, 20.0, false)"),
                "display name escaped, translation key passed:\n" + source);
    }

    @Test
    void aliasIsDeprecatedAndPointsToCurrentConstant() {
        String source = AttributeGenerator.generate(TestContents.CONTEXT, List.of(TestContents.attribute(
                "cygnus:strength", "Stärke", null, 0.0, 1.0, 20.0, false, "cygnus:power"))).source();

        assertTrue(source.contains("public static final CustomAttribute POWER = STRENGTH;"), "alias target:\n" + source);
    }

    @Test
    void emptyTypeHasEmptyList() {
        String source = AttributeGenerator.generate(TestContents.CONTEXT, List.of()).source();

        assertTrue(source.contains("public static final List<CustomAttribute> ALL = List.of();"), "empty ALL:\n" + source);
    }

    @Test
    void classIsNamedAfterProjectAndType() {
        assertEquals("CygnusAttributes", AttributeGenerator.generate(TestContents.CONTEXT, List.of()).className(),
                "class name");
    }
}
