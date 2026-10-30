package net.onelitefeather.vulpes.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class JavaNamesTest {

    @Test
    void packageSegmentReplacesDashWithUnderscore() {
        assertEquals("my_game", JavaNames.packageSegment("my-game"), "package segment of my-game");
    }

    @Test
    void packageSegmentAppendsUnderscoreToJavaKeyword() {
        assertEquals("default_", JavaNames.packageSegment("default"), "package segment that is a keyword");
    }

    @Test
    void typeNameCapitalizesEverySegment() {
        assertEquals("MyGame", JavaNames.typeName("my-game"), "type name of my-game");
    }

    @Test
    void typeNameCapitalizesSingleSegment() {
        assertEquals("Cygnus", JavaNames.typeName("cygnus"), "type name of cygnus");
    }

    @Test
    void constantNameAddsPackPrefixAndUpperCases() {
        assertEquals("PACK_16X", JavaNames.constantName("16x"), "constant name of pack 16x");
    }

    @Test
    void constantNameReplacesDashWithUnderscore() {
        assertEquals("PACK_MY_PACK", JavaNames.constantName("my-pack"), "constant name of my-pack");
    }
}
