package net.onelitefeather.vulpes.format.diff;

import static net.onelitefeather.vulpes.format.diff.DiffFixtures.CRIT;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.IN_ARENA;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.IRON_SWORD;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.OLD_SWORD;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.STEEL_SWORD;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.SWORD;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.armorDamage;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.attribute;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.content;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.inArena;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.item;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.pack;
import static net.onelitefeather.vulpes.format.diff.DiffFixtures.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.ExpressionFunction;
import net.onelitefeather.vulpes.format.ExpressionParameter;
import net.onelitefeather.vulpes.format.ExpressionType;
import net.onelitefeather.vulpes.format.Item;
import net.onelitefeather.vulpes.format.Release;
import net.onelitefeather.vulpes.format.Snapshot;
import org.junit.jupiter.api.Test;

final class ContentDiffTest {

    @Test
    void reportsAddedItemByKey() {
        DiffReport report = compare(snapshot(List.of(), List.of(), List.of()),
                snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()));

        assertEquals(List.of(IRON_SWORD), report.items().added(), "added items");
        assertEquals(List.of(), report.items().removed(), "removed items");
    }

    @Test
    void reportsRemovedItemByKey() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(), List.of(), List.of()));

        assertEquals(List.of(IRON_SWORD), report.items().removed(), "removed items");
        assertEquals(List.of(), report.items().added(), "added items");
    }

    @Test
    void reportsRenamedItemWithOldKeyInPreviousKeysAsAliased() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, OLD_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(item(SWORD, STEEL_SWORD, List.of(OLD_SWORD))), List.of(), List.of()));

        assertEquals(List.of(new Rename(OLD_SWORD, STEEL_SWORD, true)), report.items().renamed(),
                "renamed items keep the id and report the aliased old key");
        assertEquals(List.of(), report.items().removed(), "a rename is not a removal");
        assertEquals(List.of(), report.items().added(), "a rename is not an addition");
    }

    @Test
    void reportsRenamedItemWithoutOldKeyInPreviousKeysAsNotAliased() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, OLD_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(item(SWORD, STEEL_SWORD, List.of())), List.of(), List.of()));

        assertEquals(List.of(new Rename(OLD_SWORD, STEEL_SWORD, false)), report.items().renamed(),
                "renamed item without alias");
    }

    @Test
    void reportsChangedFieldsOfItemSortedByName() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(new Item(SWORD, List.of(), IRON_SWORD, "minecraft:diamond_sword", 2, Map.of())),
                        List.of(), List.of()));

        assertEquals(List.of(new Change(IRON_SWORD, List.of("amount", "material"), false)), report.items().changed(),
                "changed fields of the item, sorted by name");
    }

    @Test
    void ignoresPreviousKeysAndIdWhenComparingFields() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(item(SWORD, IRON_SWORD, List.of(OLD_SWORD))), List.of(), List.of()));

        assertFalse(report.items().hasChanges(), "previousKeys alone must not count as a change");
    }

    @Test
    void reportsChangedAttributeValueByField() {
        DiffReport report = compare(snapshot(List.of(), List.of(attribute(CRIT, "cygnus:crit_chance", 1.0)), List.of()),
                snapshot(List.of(), List.of(attribute(CRIT, "cygnus:crit_chance", 2.0)), List.of()));

        assertEquals(List.of(new Change("cygnus:crit_chance", List.of("maximumValue"), false)),
                report.attributes().changed(), "changed attribute");
    }

    @Test
    void reportsPackAddedAndRemovedById() {
        DiffReport report = compare(content("1.4.0", snapshot(List.of(), List.of(), List.of()), List.of(pack("16x", "a1"))),
                content("1.5.0", snapshot(List.of(), List.of(), List.of()), List.of(pack("32x", "b2"))));

        assertEquals(List.of("32x"), report.packs().added(), "added packs");
        assertEquals(List.of("16x"), report.packs().removed(), "removed packs");
    }

    @Test
    void reportsPackWithOtherSha1AsChanged() {
        DiffReport report = compare(content("1.4.0", snapshot(List.of(), List.of(), List.of()), List.of(pack("16x", "a1"))),
                content("1.5.0", snapshot(List.of(), List.of(), List.of()), List.of(pack("16x", "c3"))));

        assertEquals(List.of("16x"), report.packs().changed(), "pack whose sha1 changed");
    }

    @Test
    void reportsInterfaceFunctionWithChangedParametersAsSignatureChange() {
        DiffReport report = compare(
                snapshot(List.of(), List.of(), List.of(inArena(List.of(new ExpressionParameter("arena", ExpressionType.STRING))))),
                snapshot(List.of(), List.of(), List.of(inArena(List.of(new ExpressionParameter("arena", ExpressionType.NUMBER))))));

        assertEquals(List.of(new Change("inArena", List.of("parameters"), true)), report.expressionFunctions().changed(),
                "changed interface function signature");
    }

    @Test
    void reportsRenamedExpressionFunctionByNameWithPreviousNamesAsAliased() {
        ExpressionFunction before = armorDamage("base");
        ExpressionFunction renamed = new ExpressionFunction(DiffFixtures.ARMOR_DAMAGE, List.of("armorDamage"),
                "armorReduction", before.kind(), before.description(), before.parameters(), before.returnType(),
                before.expression());
        DiffReport report = compare(snapshot(List.of(), List.of(), List.of(before)),
                snapshot(List.of(), List.of(), List.of(renamed)));

        assertEquals(List.of(new Rename("armorDamage", "armorReduction", true)),
                report.expressionFunctions().renamed(), "renamed function with previous name");
    }

    @Test
    void reportsNoChangesForEqualReleases() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()));

        assertFalse(report.hasChanges(), "equal releases must report no changes");
    }

    @Test
    void refusesToCompareReleasesOfDifferentProjects() {
        ContentRelease cygnus = content("1.4.0", snapshot(List.of(), List.of(), List.of()));
        ContentRelease manis = new ContentRelease(
                new Release(1, "manis", DiffFixtures.PROJECT_ID, "1.5.0", "sha256:x", List.of(),
                        cygnus.release().provenance()),
                cygnus.snapshot());

        assertThrows(IllegalArgumentException.class, () -> ContentDiff.compare(cygnus, manis),
                "releases of different projects must not be compared");
    }

    @Test
    void refusesItemsWithDuplicateIds() {
        Snapshot before = snapshot(List.of(item(SWORD, IRON_SWORD, List.of()), item(SWORD, STEEL_SWORD, List.of())),
                List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> compare(before, snapshot(List.of(), List.of(), List.of())),
                "two items with the same id must not be compared");
    }

    @Test
    void noChangesRequiresNoBump() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()));

        assertEquals(VersionBump.NONE, report.requiredBump(), "bump without changes");
    }

    @Test
    void valueChangeRequiresPatchOnly() {
        DiffReport report = compare(snapshot(List.of(), List.of(attribute(CRIT, "cygnus:crit_chance", 1.0)), List.of()),
                snapshot(List.of(), List.of(attribute(CRIT, "cygnus:crit_chance", 2.0)), List.of()));

        assertEquals(VersionBump.PATCH, report.requiredBump(), "bump for a changed value");
    }

    @Test
    void packChangeRequiresPatchOnly() {
        DiffReport report = compare(content("1.4.0", snapshot(List.of(), List.of(), List.of()), List.of(pack("16x", "a1"))),
                content("1.5.0", snapshot(List.of(), List.of(), List.of()), List.of(pack("16x", "c3"))));

        assertEquals(VersionBump.PATCH, report.requiredBump(), "bump for a changed pack");
    }

    @Test
    void addedKeyRequiresMinor() {
        DiffReport report = compare(snapshot(List.of(), List.of(), List.of()),
                snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()));

        assertEquals(VersionBump.MINOR, report.requiredBump(), "bump for an added key");
    }

    @Test
    void removedKeyRequiresMajor() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(), List.of(), List.of()));

        assertEquals(VersionBump.MAJOR, report.requiredBump(), "bump for a removed key");
    }

    @Test
    void renameWithAliasRequiresMinor() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, OLD_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(item(SWORD, STEEL_SWORD, List.of(OLD_SWORD))), List.of(), List.of()));

        assertEquals(VersionBump.MINOR, report.requiredBump(), "bump for a rename that keeps the alias");
    }

    @Test
    void renameWithoutAliasRequiresMajor() {
        DiffReport report = compare(snapshot(List.of(item(SWORD, OLD_SWORD, List.of())), List.of(), List.of()),
                snapshot(List.of(item(SWORD, STEEL_SWORD, List.of())), List.of(), List.of()));

        assertEquals(VersionBump.MAJOR, report.requiredBump(), "bump for a rename without alias");
    }

    @Test
    void interfaceSignatureChangeRequiresMajor() {
        DiffReport report = compare(
                snapshot(List.of(), List.of(), List.of(inArena(List.of()))),
                snapshot(List.of(), List.of(), List.of(inArena(List.of(new ExpressionParameter("arena", ExpressionType.STRING))))));

        assertEquals(VersionBump.MAJOR, report.requiredBump(), "bump for a changed interface signature");
    }

    @Test
    void formulaChangeRequiresPatchEvenWithChangedParameters() {
        ExpressionFunction withOneParameter = armorDamage("base");
        ExpressionFunction withTwoParameters = new ExpressionFunction(withOneParameter.id(), List.of(),
                withOneParameter.name(), withOneParameter.kind(), withOneParameter.description(),
                List.of(new ExpressionParameter("base", ExpressionType.NUMBER),
                        new ExpressionParameter("armor", ExpressionType.NUMBER)),
                withOneParameter.returnType(), withOneParameter.expression());
        DiffReport report = compare(snapshot(List.of(), List.of(), List.of(withOneParameter)),
                snapshot(List.of(), List.of(), List.of(withTwoParameters)));

        assertEquals(VersionBump.PATCH, report.requiredBump(), "bump for a formula function with new parameters");
        assertEquals(List.of(new Change("armorDamage", List.of("parameters"), true)),
                report.expressionFunctions().changed(), "signature change is still listed");
    }

    @Test
    void highestBumpOfAllChangesWins() {
        DiffReport report = compare(snapshot(List.of(), List.of(attribute(CRIT, "cygnus:crit_chance", 1.0)), List.of()),
                snapshot(List.of(item(SWORD, IRON_SWORD, List.of())), List.of(attribute(CRIT, "cygnus:crit_chance", 2.0)),
                        List.of()));

        assertEquals(VersionBump.MINOR, report.requiredBump(), "an added key (minor) outweighs a changed value (patch)");
    }

    private static DiffReport compare(Snapshot before,
                                      Snapshot after) {
        return ContentDiff.compare(content("1.4.0", before), content("1.5.0", after));
    }

    private static DiffReport compare(ContentRelease before, ContentRelease after) {
        return ContentDiff.compare(before, after);
    }
}
