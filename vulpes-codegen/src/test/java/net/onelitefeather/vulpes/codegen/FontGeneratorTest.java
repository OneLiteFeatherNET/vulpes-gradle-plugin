package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class FontGeneratorTest {

    @Test
    void buildsFontSymbolWithFontKeyAndEscapedChars() {
        String source = FontGenerator.generate(TestContents.CONTEXT, List.of(TestContents.font("cygnus:hud_icons",
                "cygnus:hud", "cygnus:font/hud_icons.png", 7, 8, List.of("\uE000")))).source();

        assertTrue(source.contains("public static final CustomFontSymbol HUD_ICONS = CustomFontSymbol.of("
                + "CygnusFontSymbolKeys.HUD_ICONS, Key.key(\"cygnus\", \"hud\"), new FontSymbol("
                + "\"cygnus:font/hud_icons.png\", \"bitmap\", 7, 8, List.of(\"\\ue000\")));"), "font symbol:\n" + source);
    }

    @Test
    void aliasIsDeprecatedAndPointsToCurrentConstant() {
        String source = FontGenerator.generate(TestContents.CONTEXT, List.of(TestContents.font("cygnus:hud_icons",
                "cygnus:hud", "cygnus:font/hud_icons.png", 7, 8, List.of("a"), "cygnus:old_icons"))).source();

        assertTrue(source.matches("(?s).*@Deprecated\\(\\s*forRemoval = true\\s*\\).*"), "alias deprecated:\n" + source);
        assertTrue(source.contains("public static final CustomFontSymbol OLD_ICONS = HUD_ICONS;"), "alias target:\n" + source);
    }

    @Test
    void emptyTypeHasEmptyList() {
        String source = FontGenerator.generate(TestContents.CONTEXT, List.of()).source();

        assertTrue(source.contains("public static final List<CustomFontSymbol> ALL = List.of();"), "empty ALL:\n" + source);
    }

    @Test
    void classIsNamedAfterProjectAndType() {
        assertEquals("CygnusFontSymbols", FontGenerator.generate(TestContents.CONTEXT, List.of()).className(),
                "class name");
    }
}
