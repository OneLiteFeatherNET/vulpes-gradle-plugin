package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.onelitefeather.vulpes.format.AdvancementFrame;
import org.junit.jupiter.api.Test;

final class AdvancementGeneratorTest {

    @Test
    void buildsAdvancementWithParentTitleFrameAndPosition() {
        String source = AdvancementGenerator.generate(TestContents.CONTEXT, List.of(
                TestContents.advancement("cygnus:root", null, null, AdvancementFrame.TASK, 0.0, 0.0),
                TestContents.advancement("cygnus:first_kill", "cygnus:root", null, AdvancementFrame.GOAL, 1.0, 0.0)))
                .source();

        assertTrue(source.contains("public static final CustomAdvancement FIRST_KILL = CustomAdvancement.builder("
                + "CygnusAdvancementKeys.FIRST_KILL).parent(CygnusAdvancementKeys.ROOT)"), "parent of the entry:\n" + source);
        assertTrue(source.contains(".title(GsonComponentSerializer.gson().deserialize(\"{\\\"text\\\":\\\"Erster Kill\\\"}\"))"),
                "title as component JSON:\n" + source);
        assertTrue(source.contains(".icon(ItemStack.of(Material.fromKey(\"minecraft:iron_sword\")))"), "icon:\n" + source);
        assertTrue(source.contains(".frame(FrameType.GOAL)"), "frame:\n" + source);
        assertTrue(source.contains(".x(1.0f).y(0.0f)"), "position as float:\n" + source);
        assertTrue(source.contains(".showToast(true).hidden(false).background(null).build();"), "flags and build:\n" + source);
    }

    @Test
    void rootHasNoParentAndKeepsBackground() {
        String source = AdvancementGenerator.generate(TestContents.CONTEXT, List.of(TestContents.advancement(
                "cygnus:root", null, "cygnus:textures/gui/backgrounds/cygnus.png", AdvancementFrame.TASK, 0.0, 0.0)))
                .source();

        assertFalse(source.contains(".parent("), "a root has no parent:\n" + source);
        assertTrue(source.contains(".background(\"cygnus:textures/gui/backgrounds/cygnus.png\")"), "background:\n" + source);
    }

    @Test
    void parentOutsideOfTheProjectIsInlinedAsKey() {
        String source = AdvancementGenerator.generate(TestContents.CONTEXT, List.of(TestContents.advancement(
                "cygnus:child", "other:root", null, AdvancementFrame.TASK, 0.0, 0.0))).source();

        assertTrue(source.contains(".parent(Key.key(\"other\", \"root\"))"), "parent key inlined:\n" + source);
    }

    @Test
    void emptyTypeHasEmptyList() {
        String source = AdvancementGenerator.generate(TestContents.CONTEXT, List.of()).source();

        assertTrue(source.contains("public static final List<CustomAdvancement> ALL = List.of();"), "empty ALL:\n" + source);
    }
}
