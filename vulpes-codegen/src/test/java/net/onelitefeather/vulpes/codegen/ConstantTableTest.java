package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ConstantTableTest {

    @Test
    void entryNameDropsProjectNamespace() {
        ConstantTable table = ConstantTable.of("cygnus", List.of(new EntryKeys("cygnus:iron_sword", List.of())));

        assertEquals("IRON_SWORD", table.entry("cygnus:iron_sword").name(), "name without the project namespace");
    }

    @Test
    void entryNameKeepsForeignNamespaceAsPrefix() {
        ConstantTable table = ConstantTable.of("cygnus", List.of(new EntryKeys("other:thing", List.of())));

        assertEquals("OTHER_THING", table.entry("other:thing").name(), "foreign namespace as prefix");
    }

    @Test
    void aliasTakesPartInClaimsOfTheSameTable() {
        ConstantTable table = ConstantTable.of("cygnus", List.of(new EntryKeys("cygnus:iron_sword", List.of("cygnus:old_sword"))));

        assertEquals("OLD_SWORD", table.aliases().get(0).name(), "alias name from its key");
        assertEquals("IRON_SWORD", table.aliases().get(0).target().name(), "alias target is the current entry");
    }

    @Test
    void collidingEntryNamesFailNamingBothKeys() {
        CodegenException exception = assertThrows(CodegenException.class, () -> ConstantTable.of("cygnus", List.of(
                new EntryKeys("cygnus:iron-sword", List.of()), new EntryKeys("cygnus:iron_sword", List.of()))),
                "two entries with one constant name must fail");

        assertTrue(exception.getMessage().contains("cygnus:iron-sword"), "message names the first key: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("cygnus:iron_sword"), "message names the second key: " + exception.getMessage());
    }

    @Test
    void aliasWithNameOfOtherEntryFails() {
        assertThrows(CodegenException.class, () -> ConstantTable.of("cygnus", List.of(
                new EntryKeys("cygnus:iron_sword", List.of()),
                new EntryKeys("cygnus:sword", List.of("cygnus:iron-sword")))),
                "an alias that collides with an entry name must fail");
    }

    @Test
    void aliasEqualToEntryKeyFails() {
        assertThrows(CodegenException.class, () -> ConstantTable.of("cygnus", List.of(
                new EntryKeys("cygnus:iron_sword", List.of("cygnus:iron_sword")))),
                "an alias that is the key of an entry must fail");
    }

    @Test
    void sameKeyTwiceFails() {
        assertThrows(CodegenException.class, () -> ConstantTable.of("cygnus", List.of(
                new EntryKeys("cygnus:iron_sword", List.of()), new EntryKeys("cygnus:iron_sword", List.of()))),
                "a key that occurs twice must fail");
    }

    @Test
    void aliasDeclaredByTwoEntriesFails() {
        assertThrows(CodegenException.class, () -> ConstantTable.of("cygnus", List.of(
                new EntryKeys("cygnus:a", List.of("cygnus:old")), new EntryKeys("cygnus:b", List.of("cygnus:old")))),
                "an alias that two entries claim must fail");
    }

    @Test
    void aliasesAreSortedByAliasKey() {
        ConstantTable table = ConstantTable.of("cygnus", List.of(
                new EntryKeys("cygnus:a", List.of("cygnus:zeta", "cygnus:alpha"))));

        assertEquals("cygnus:alpha", table.aliases().get(0).key(), "aliases sorted by key");
        assertEquals("cygnus:zeta", table.aliases().get(1).key(), "aliases sorted by key");
    }
}
