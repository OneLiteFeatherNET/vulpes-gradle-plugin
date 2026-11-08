package net.onelitefeather.vulpes.gradle;

import static net.onelitefeather.vulpes.format.fixtures.DataJarFixtures.releaseJson;
import static net.onelitefeather.vulpes.format.fixtures.DataJarFixtures.snapshotJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.function.Consumer;
import net.onelitefeather.vulpes.format.Asset;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.Expression;
import net.onelitefeather.vulpes.format.Item;
import net.onelitefeather.vulpes.format.fixtures.DataJarFixtures;
import org.junit.jupiter.api.Test;

/**
 * The merge of a local snapshot over the published snapshot of the same project. In-memory fixtures only.
 */
final class ContentMergeTest {

    private static final String PROJECT = "cygnus";
    private static final String PUBLISHED_SOURCE = "cygnus-1.4.0.jar";
    private static final String LOCAL_SOURCE = "vulpes/cygnus";
    private static final String IRON_SWORD = "12f6f240-843f-5833-8fba-a8ffbe31c173";
    private static final String RATION = "d2875ef7-ea80-56af-9955-1c8a4be7938d";
    private static final String ROOT = "43de1c8a-e912-520a-985d-60ec86891515";
    private static final String FIRST_KILL = "6b662b93-1b3a-58b9-ac9e-f6c3930b40d7";
    private static final String ARMOR_DAMAGE = "c91dab27-e52f-5b83-aa81-cb2b3d8a971e";
    private static final String IN_ARENA = "709b4d25-2545-5bde-a8bf-06d9fef28803";
    private static final String NEW_ID = "00000000-0000-0000-0000-0000000000a1";
    private static final String OTHER_ID = "00000000-0000-0000-0000-0000000000b2";
    private static final String OTHER_PROJECT_ID = "00000000-0000-0000-0000-0000000000c2";
    private static final String HUD_ASSET = "assets/cygnus/textures/font/hud_icons.png";
    private static final String BACKGROUND_ASSET = "assets/cygnus/textures/gui/advancements/backgrounds/cygnus.png";
    private static final String NEW_SHA1 = "1111111111111111111111111111111111111111";
    private static final String CLICK_ASSET = "assets/cygnus/sounds/ui/click.ogg";
    private static final String HUD_LOCATION = "cygnus:font/hud_icons.png";
    private static final String CLICK_LOCATION = "cygnus:ui/click";
    private static final String UI_LOCATION = "cygnus:font/ui.png";
    private static final String MISSING_LOCATION = "cygnus:font/missing.png";

    @Test
    void replacesPublishedEntryWithSameIdCompletely() {
        ContentRelease merged = merge(local(s -> {
            entry(s, "items", 0).put("material", "minecraft:gold_ingot");
            entry(s, "items", 0).putArray("previousKeys");
        }));

        Item sword = item(merged, IRON_SWORD);
        assertEquals("minecraft:gold_ingot", sword.material(), "the local material replaces the published one");
        assertEquals(List.of(), sword.previousKeys(), "the published previous keys are dropped, not merged");
    }

    @Test
    void renameThroughStableIdLeavesNoOldKey() {
        ContentRelease merged = merge(local(s -> entry(s, "items", 0).put("key", "cygnus:steel_sword")));

        List<String> keys = itemKeys(merged);
        assertEquals(1, keys.stream().filter("cygnus:steel_sword"::equals).count(), "the renamed item is present once");
        assertFalse(keys.contains("cygnus:iron_sword"), "the published key is replaced, not kept beside the new one: " + keys);
    }

    @Test
    void addsEntryWithNewIdNextToPublishedItems() {
        ContentMerge.Merged result = mergeResult(local(s -> {
            keepOnlyItems(s);
            appendCopyOf(s, "items", 1, c -> c.put("id", NEW_ID).put("key", "cygnus:apple"));
        }));

        assertTrue(itemKeys(result.content()).contains("cygnus:apple"), "the new item is added");
        assertEquals(1, result.added(), "one added entry");
        assertEquals(2, result.replaced(), "the two unchanged published items are replaced by their local copies");
    }

    @Test
    void sortsMergedItemsByKey() {
        ContentRelease merged = merge(local(s -> appendCopyOf(s, "items", 1,
                c -> c.put("id", NEW_ID).put("key", "cygnus:apple"))));

        assertEquals(List.of("cygnus:apple", "cygnus:iron_sword", "cygnus:ration"), itemKeys(merged),
                "merged items are sorted by key as the schema requires");
    }

    @Test
    void keepsPublishedEntryWithoutLocalCounterpart() {
        ContentMerge.Merged result = mergeResult(local(s -> {
            keepOnlyItems(s);
            onlyCopyOf(s, "items", 1, c -> c.put("id", NEW_ID).put("key", "cygnus:apple"));
        }));

        assertEquals(List.of("cygnus:apple", "cygnus:iron_sword", "cygnus:ration"), itemKeys(result.content()),
                "published items the local snapshot does not hold stay in the build");
        assertEquals(0, result.replaced(), "no local entry replaces a published one");
        assertEquals(1, result.added(), "the local entry is added");
    }

    @Test
    void matchesAssetsByPath() {
        ContentRelease merged = merge(local(s -> {
            entry(s, "assets", 1).put("sha1", NEW_SHA1);
            appendCopyOf(s, "assets", 0, c -> c.put("path", "assets/cygnus/sounds/ui/new.ogg"));
        }));

        List<Asset> assets = merged.snapshot().assets();
        assertEquals(List.of("assets/cygnus/sounds/ui/click.ogg", "assets/cygnus/sounds/ui/new.ogg", HUD_ASSET,
                        BACKGROUND_ASSET),
                assets.stream().map(Asset::path).toList(), "the new path is added and the shared path is kept once");
        assertEquals(NEW_SHA1, assets.stream().filter(a -> a.path().equals(HUD_ASSET)).findFirst().orElseThrow().sha1(),
                "the local asset replaces the published asset with the same path");
    }

    @Test
    void matchesExpressionsById() {
        ContentRelease merged = merge(local(s -> entry(s, "expressions", 0).put("name", "maxHealthRenamed")));

        assertEquals(List.of("maxHealthRenamed"), merged.snapshot().expressions().stream().map(Expression::name).toList(),
                "a renamed expression with the same id replaces the published one");
    }

    @Test
    void acceptsLocalReferenceToPublishedAsset() {
        ContentRelease merged = merge(local(s -> clear(s, "assets")));

        assertEquals(published().snapshot().assets().size(), merged.snapshot().assets().size(),
                "the font of the local snapshot uses the published asset");
    }

    @Test
    void takesReleaseAndPackFromLocalSide() {
        ContentRelease local = local(DataJarFixtures.modifyRelease(releaseJson(),
                        release -> release.put("version", "1.4.1-SNAPSHOT")),
                s -> ((ObjectNode) s.get("pack")).putArray("resolutions").add(16));

        ContentRelease merged = ContentMerge.merge(PROJECT, published(), local).content();

        assertEquals("1.4.1-SNAPSHOT", merged.release().version(), "the release describes the merged result");
        assertEquals(List.of(16), merged.snapshot().pack().resolutions(), "the pack comes from the local snapshot");
    }

    @Test
    void countsReplacedAndAddedEntriesOverAllLists() {
        ContentMerge.Merged result = mergeResult(local(s -> {
            clear(s, "fonts");
            clear(s, "sounds");
            clear(s, "advancements");
            clear(s, "expressionFunctions");
            clear(s, "expressions");
            clear(s, "assets");
            onlyCopyOf(s, "attributes", 0, c -> c.put("id", NEW_ID).put("key", "cygnus:agility"));
            onlyCopyOf(s, "items", 0, c -> c.put("material", "minecraft:gold_ingot"));
        }));

        assertEquals(1, result.replaced(), "the replaced item");
        assertEquals(1, result.added(), "the added attribute");
    }

    @Test
    void failsNamingBothProjectIds() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> merge(local(s -> s.put("projectId", OTHER_PROJECT_ID))));

        assertMessage(e, "5b0c7a52-3c1e-4d7a-9f2e-0a1b2c3d4e5f", OTHER_PROJECT_ID);
    }

    @Test
    void failsNamingBothNamespaces() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> merge(local(s -> s.put("namespace", "manis"))));

        assertMessage(e, "'manis'", "'cygnus'");
    }

    @Test
    void failsOnKeyConflictNamingBothIdsAndSides() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> merge(local(s -> onlyCopyOf(s, "items", 0, c -> c.put("id", NEW_ID)))));

        assertMessage(e, "cygnus:iron_sword", NEW_ID, "(local)", IRON_SWORD, "(published)");
    }

    @Test
    void failsOnPreviousKeyThatIsCurrentKeyOfSameList() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> merge(local(s -> entry(s, "items", 0).putArray("previousKeys").add("cygnus:ration"))));

        assertMessage(e, "previous key 'cygnus:ration'", IRON_SWORD);
    }

    @Test
    void failsOnNameClashBetweenExpressionAndFunctionNamingBothSides() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> merge(local(s -> {
                    clear(s, "expressionFunctions");
                    onlyCopyOf(s, "expressions", 0, c -> c.put("id", NEW_ID).put("name", "inArena"));
                })));

        assertMessage(e, "'inArena'", NEW_ID, "(local)", IN_ARENA, "(published)");
    }

    @Test
    void failsOnDanglingAdvancementParentNamingBothSides() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> merge(local(s -> {
                    onlyCopyOf(s, "advancements", 1, c -> c.put("key", "cygnus:new_root"));
                })));

        assertMessage(e, FIRST_KILL, "(published)", "'cygnus:root'", ROOT, "'cygnus:new_root'", "(local)");
    }

    @Test
    void failsOnAdvancementCycle() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> merge(local(s -> {
                    onlyCopyOf(s, "advancements", 1, c -> c.put("parent", "cygnus:first_kill"));
                })));

        assertMessage(e, "cycle", "cygnus:root");
    }

    @Test
    void reportsFontWithoutAssetOnLocalSideOfMergedContent() {
        ContentMerge.Merged result = mergeResult(local(s -> {
            clear(s, "assets");
            entry(s, "fonts", 0).put("file", MISSING_LOCATION);
        }));

        assertEquals(1, result.missingAssets().size(), "one missing asset: " + result.missingAssets());
        ContentMerge.MissingAsset font = result.missingAssets().get(0);
        assertEquals("local", font.side(), "the font comes from the local side");
        assertEquals(MISSING_LOCATION, font.location(), "the location the font uses");
        assertTrue(font.entry().contains("'cygnus:hud_icons'"), "the entry names the font: " + font.entry());
        assertTrue(font.message().contains("(local) uses resource location '" + MISSING_LOCATION
                        + "' that has no asset entry"), "warning text: " + font.message());
    }

    @Test
    void reportsPublishedSoundWithoutAssetOnPublishedSide() {
        ContentRelease published = publishedChanged(s -> removeAsset(s, CLICK_ASSET));
        ContentRelease local = local(s -> {
            clear(s, "assets");
            clear(s, "sounds");
        });

        List<ContentMerge.MissingAsset> missing = ContentMerge.merge(PROJECT, published, local).missingAssets();

        assertEquals(1, missing.size(), "one missing asset: " + missing);
        assertEquals("published", missing.get(0).side(), "the sound entry is kept from the published side");
        assertEquals(CLICK_LOCATION, missing.get(0).location(), "the location the sound uses");
        assertTrue(missing.get(0).entry().contains("'cygnus:ui.click'"), "the entry names the sound: " + missing);
    }

    @Test
    void reportsEachReferenceToMissingLocationOnce() {
        List<ContentMerge.MissingAsset> missing = ContentMerge.checkLocalOnly(PROJECT, local(s -> {
            removeAsset(s, HUD_ASSET);
            appendCopyOf(s, "fonts", 0, c -> c.put("id", NEW_ID).put("key", "cygnus:other_icons"));
        }));

        assertEquals(2, missing.size(), "one report per font that uses the location: " + missing);
        assertEquals(2, missing.stream().map(ContentMerge.MissingAsset::entry).distinct().count(),
                "the two fonts are reported as two entries: " + missing);
        assertTrue(missing.stream().allMatch(m -> m.location().equals(HUD_LOCATION)),
                "both reports name the shared location: " + missing);
    }

    @Test
    void failsOnAssetPathUsedTwice() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> merge(local(s -> appendCopyOf(s, "assets", 1, c -> c.put("sha1", NEW_SHA1)))));

        assertMessage(e, HUD_ASSET);
    }

    @Test
    void failsOnDuplicateKeyInLocalOnlySnapshot() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> ContentMerge.checkLocalOnly(PROJECT,
                        local(s -> appendCopyOf(s, "items", 0, c -> c.put("id", NEW_ID)))));

        assertMessage(e, "cygnus:iron_sword", IRON_SWORD, NEW_ID, "(local)");
        assertFalse(e.getMessage().contains("(published)"), "every entry of a local-only project comes from local:\n"
                + e.getMessage());
    }

    @Test
    void reportsFontWithoutAssetInLocalOnlySnapshotAndPasses() {
        List<ContentMerge.MissingAsset> missing = ContentMerge.checkLocalOnly(PROJECT, local(s -> {
            removeAsset(s, HUD_ASSET);
            entry(s, "fonts", 0).put("file", UI_LOCATION);
        }));

        assertEquals(1, missing.size(), "exactly the font is reported: " + missing);
        ContentMerge.MissingAsset font = missing.get(0);
        assertEquals(PROJECT, font.project(), "the project");
        assertEquals("local", font.side(), "every entry of a local-only snapshot is local");
        assertEquals(UI_LOCATION, font.location(), "the location the font uses");
        assertTrue(font.entry().contains("'cygnus:hud_icons'"), "the entry names the font: " + font.entry());
    }

    @Test
    void failsOnDuplicateKeyNextToMissingAsset() {
        LocalContentException e = assertThrows(LocalContentException.class,
                () -> ContentMerge.checkLocalOnly(PROJECT, local(s -> {
                    removeAsset(s, HUD_ASSET);
                    appendCopyOf(s, "items", 0, c -> c.put("id", NEW_ID));
                })));

        assertMessage(e, "key 'cygnus:iron_sword'");
    }

    @Test
    void reportsNothingWhenAssetsCoverEveryLocation() {
        List<ContentMerge.MissingAsset> missing = ContentMerge.checkLocalOnly(PROJECT, local(s -> { }));

        assertTrue(missing.isEmpty(), "a complete local snapshot with its advancement tree reports no missing asset: "
                + missing);
    }

    private static ContentRelease published() {
        return ContentReader.read(PUBLISHED_SOURCE, releaseJson(), snapshotJson());
    }

    private static ContentRelease local(Consumer<ObjectNode> change) {
        return local(releaseJson(), change);
    }

    private static ContentRelease local(byte[] releaseJson, Consumer<ObjectNode> change) {
        DataJarFixtures.Metadata metadata = DataJarFixtures.modifySnapshot(releaseJson, snapshotJson(), change);
        return ContentReader.readWithoutCrossEntryChecks(LOCAL_SOURCE, metadata.releaseJson(), metadata.snapshotJson());
    }

    private static ContentRelease merge(ContentRelease local) {
        return ContentMerge.merge(PROJECT, published(), local).content();
    }

    private static ContentMerge.Merged mergeResult(ContentRelease local) {
        return ContentMerge.merge(PROJECT, published(), local);
    }

    private static Item item(ContentRelease merged, String id) {
        return merged.snapshot().items().stream().filter(i -> i.id().toString().equals(id)).findFirst().orElseThrow();
    }

    private static List<String> itemKeys(ContentRelease merged) {
        return merged.snapshot().items().stream().map(Item::key).toList();
    }

    private static ObjectNode entry(ObjectNode snapshot, String list, int index) {
        return (ObjectNode) snapshot.get(list).get(index);
    }

    /** Replaces the list with the copy of one of its entries, changed as given. */
    private static void onlyCopyOf(ObjectNode snapshot, String list, int index, Consumer<ObjectNode> change) {
        ObjectNode copy = entry(snapshot, list, index).deepCopy();
        change.accept(copy);
        ArrayNode array = (ArrayNode) snapshot.get(list);
        array.removeAll();
        array.add(copy);
    }

    /** Appends a copy of one of the entries of the list, changed as given. */
    private static void appendCopyOf(ObjectNode snapshot, String list, int index, Consumer<ObjectNode> change) {
        ObjectNode copy = entry(snapshot, list, index).deepCopy();
        change.accept(copy);
        ((ArrayNode) snapshot.get(list)).add(copy);
    }

    /** Keeps only the items of the local snapshot, so that no other published entry counts as replaced. */
    private static void keepOnlyItems(ObjectNode snapshot) {
        for (String list : List.of("attributes", "fonts", "sounds", "advancements", "expressionFunctions",
                "expressions", "assets")) {
            clear(snapshot, list);
        }
    }

    private static void clear(ObjectNode snapshot, String list) {
        ((ArrayNode) snapshot.get(list)).removeAll();
    }

    /** The published release and snapshot with the given change, read without the cross-entry checks. */
    private static ContentRelease publishedChanged(Consumer<ObjectNode> change) {
        DataJarFixtures.Metadata metadata = DataJarFixtures.modifySnapshot(releaseJson(), snapshotJson(), change);
        return ContentReader.readWithoutCrossEntryChecks(PUBLISHED_SOURCE, metadata.releaseJson(),
                metadata.snapshotJson());
    }

    /** Removes the asset with the given path from the snapshot. */
    private static void removeAsset(ObjectNode snapshot, String path) {
        ArrayNode assets = (ArrayNode) snapshot.get("assets");
        for (int i = 0; i < assets.size(); i++) {
            if (path.equals(assets.get(i).get("path").asText())) {
                assets.remove(i);
                return;
            }
        }
        throw new IllegalArgumentException("no asset " + path + " in the snapshot");
    }

    private static void assertMessage(LocalContentException e, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(e.getMessage().contains(fragment),
                    "message must contain '" + fragment + "':\n" + e.getMessage());
        }
    }
}
