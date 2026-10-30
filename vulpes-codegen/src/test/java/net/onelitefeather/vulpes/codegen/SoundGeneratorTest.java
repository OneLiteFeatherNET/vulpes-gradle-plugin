package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SoundGeneratorTest {

    @Test
    void buildsSoundWithDefaultAttenuation() {
        String source = SoundGenerator.generate(TestContents.CONTEXT, List.of(TestContents.sound("cygnus:ui.click"))).source();

        assertTrue(source.contains("public static final CustomSound UI_CLICK = CustomSound.of(CygnusSoundKeys.UI_CLICK, null);"),
                "sound constant:\n" + source);
    }

    @Test
    void aliasIsDeprecatedAndPointsToCurrentConstant() {
        String source = SoundGenerator.generate(TestContents.CONTEXT, List.of(TestContents.sound("cygnus:ui.click",
                "cygnus:ui.old_click"))).source();

        assertTrue(source.contains("public static final CustomSound UI_OLD_CLICK = UI_CLICK;"), "alias:\n" + source);
    }

    @Test
    void emptyTypeHasEmptyList() {
        String source = SoundGenerator.generate(TestContents.CONTEXT, List.of()).source();

        assertTrue(source.contains("public static final List<CustomSound> ALL = List.of();"), "empty ALL:\n" + source);
    }

    @Test
    void classIsNamedAfterProjectAndType() {
        assertEquals("CygnusSounds", SoundGenerator.generate(TestContents.CONTEXT, List.of()).className(), "class name");
    }
}
