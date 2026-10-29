package net.onelitefeather.vulpes.format;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.onelitefeather.vulpes.format.TestFixtures.bytes;
import static net.onelitefeather.vulpes.format.TestFixtures.sha256Hex;
import static net.onelitefeather.vulpes.format.TestFixtures.tree;
import static net.onelitefeather.vulpes.format.TestFixtures.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ContentReaderTest {

    private static final String SOURCE = "cygnus-1.4.0.jar";
    private static final String SNAPSHOT_FIXTURE = "/fixtures/snapshot.json";
    private static final String RELEASE_FIXTURE = "/fixtures/release.json";
    private static final String RELEASE_ENTRY = "META-INF/vulpes/release.json";
    private static final String SNAPSHOT_ENTRY = "META-INF/vulpes/snapshot.json";
    private static final String PROJECT_ID = "5b0c7a52-3c1e-4d7a-9f2e-0a1b2c3d4e5f";

    @Test
    void readsReleaseMetadataFromFixture() {
        Release release = readFixturePair().release();

        assertEquals(1, release.format(), "release format");
        assertEquals("cygnus", release.project(), "project");
        assertEquals(UUID.fromString(PROJECT_ID), release.projectId(), "projectId");
        assertEquals("1.4.0", release.version(), "version");
        assertEquals("sha256:046d3f84a66ba0940f6686d2679df8ac03d02c0350e0312e79feac5c9318b0b4", release.revision(), "revision");
        assertEquals(
                List.of(
                        new Pack("16x", 16,
                                "https://cdn.onelitefeather.net/packs/cygnus/3b1e5c0d9a8f7e6d5c4b3a291807f6e5d4c3b2a1.zip",
                                "3b1e5c0d9a8f7e6d5c4b3a291807f6e5d4c3b2a1", 123456L),
                        new Pack("32x", 32,
                                "https://cdn.onelitefeather.net/packs/cygnus/a07d1e2f3a4b5c6d7e8f90a1b2c3d4e5f6a7b8c9.zip",
                                "a07d1e2f3a4b5c6d7e8f90a1b2c3d4e5f6a7b8c9", 234567L)),
                release.packs(), "packs");
        assertEquals(new Provenance("2.0.0", "stelaris", Instant.parse("2026-10-29T10:00:00Z")),
                release.provenance(), "provenance");
    }

    @Test
    void readsItemsWithSortedComponentMap() {
        Item sword = readFixturePair().snapshot().items().get(0);

        assertEquals(new Item(UUID.fromString("12f6f240-843f-5833-8fba-a8ffbe31c173"), List.of("cygnus:old_sword"),
                "cygnus:iron_sword", "minecraft:iron_sword", 1,
                Map.of("minecraft:enchantments", "{\"minecraft:sharpness\":5}",
                        "minecraft:lore", "[{\"text\":\"Zeile 1\"}]")), sword, "first item");
    }

    @Test
    void keepsNullTranslationKeyOfAttribute() {
        Attribute crit = readFixturePair().snapshot().attributes().get(0);

        assertEquals(new Attribute(UUID.fromString("ff4741e6-d71f-5ef3-9fc3-d24e4d136193"), List.of(), "cygnus:crit_chance",
                "Krit-Chance", null, 0.0, 0.0, 1.0, true), crit, "attribute without translation key");
    }

    @Test
    void readsFontSymbolWithBitmapRows() {
        FontSymbol font = readFixturePair().snapshot().fonts().get(0);

        assertEquals(new FontSymbol(UUID.fromString("f49a3303-350b-5a6a-9494-03b290764a25"), List.of(), "cygnus:hud_icons",
                "cygnus:hud", "bitmap", "cygnus:font/hud_icons.png", 7, 8, List.of("\uE000\uE001")), font,
                "font symbol");
    }

    @Test
    void readsSoundEventWithFileEntry() {
        SoundEvent event = readFixturePair().snapshot().sounds().get(0);

        assertEquals(new SoundEvent(UUID.fromString("3b791b12-7153-5533-a127-62018a7fa823"), List.of(), "cygnus:ui.click",
                "Klick", false, List.of(new SoundEntry("cygnus:ui/click", SoundEntryType.FILE, 0.5, 1.0, 1, false, 16, false))),
                event, "sound event");
    }

    @Test
    void readsAdvancementWithParentAndRawTextComponents() {
        List<Advancement> advancements = readFixturePair().snapshot().advancements();

        assertEquals(new Advancement(UUID.fromString("6b662b93-1b3a-58b9-ac9e-f6c3930b40d7"), List.of(), "cygnus:first_kill",
                "cygnus:root", "minecraft:iron_sword", AdvancementFrame.GOAL, "{\"text\":\"Erster Kill\"}",
                "{\"text\":\"Besiege einen Gegner\"}", null, 1.0, 0.0, true, false, true), advancements.get(0),
                "child advancement");
        assertEquals(new Advancement(UUID.fromString("43de1c8a-e912-520a-985d-60ec86891515"), List.of(), "cygnus:root",
                null, "minecraft:compass", AdvancementFrame.TASK, "{\"text\":\"Cygnus\"}", "{\"text\":\"Betritt Cygnus\"}",
                "cygnus:textures/gui/advancements/backgrounds/cygnus.png", 0.0, 0.0, false, false, false), advancements.get(1),
                "root advancement");
    }

    @Test
    void readsInterfaceAndFormulaExpressionFunctions() {
        List<ExpressionFunction> functions = readFixturePair().snapshot().expressionFunctions();

        assertEquals(new ExpressionFunction(UUID.fromString("c91dab27-e52f-5b83-aa81-cb2b3d8a971e"), List.of(), "armorDamage",
                ExpressionFunctionKind.FORMULA, "Schaden nach Rüstung",
                List.of(new ExpressionParameter("base", ExpressionType.NUMBER), new ExpressionParameter("armor", ExpressionType.NUMBER)),
                ExpressionType.NUMBER, "base * (1 - armor / 100)"), functions.get(0), "formula function");
        assertEquals(new ExpressionFunction(UUID.fromString("709b4d25-2545-5bde-a8bf-06d9fef28803"), List.of(), "inArena",
                ExpressionFunctionKind.INTERFACE, "Ob der Spieler in einer Arena steht",
                List.of(new ExpressionParameter("arena", ExpressionType.STRING)), ExpressionType.BOOLEAN, null), functions.get(1),
                "interface function");
    }

    @Test
    void readsAssetsWithNullableTextureResolution() {
        List<Asset> assets = readFixturePair().snapshot().assets();

        assertEquals(new Asset("assets/cygnus/sounds/ui/click.ogg", "9a8b7c6d5e4f30211203f4e5d6c7b8a9f0e1d2c3", 4096L,
                AssetKind.SOUND, null), assets.get(0), "sound asset");
        assertEquals(new Asset("assets/cygnus/textures/font/hud_icons.png", "2f1e0c6a9b3d4e5f60718293a4b5c6d7e8f90a1b", 512L,
                AssetKind.TEXTURE, 16), assets.get(1), "texture asset");
    }

    @Test
    void readsPackSettingsWithDescriptionAsRawJson() {
        PackSettings pack = readFixturePair().snapshot().pack();

        assertEquals(new PackSettings(List.of(16, 32), "{\"text\":\"Cygnus\"}"), pack, "pack settings");
    }

    @Test
    void keepsComponentValueAsCompactJsonWithSortedKeys() {
        ObjectNode snapshot = (ObjectNode) tree(bytes(SNAPSHOT_FIXTURE));
        ObjectNode food = (ObjectNode) snapshot.get("items").get(1).get("components").get("minecraft:food");
        food.removeAll();
        food.put("saturation", 2.4);
        food.put("nutrition", 4);

        Item ration = readSnapshot(snapshot).items().get(1);

        assertEquals("{\"nutrition\":4,\"saturation\":2.4}", ration.components().get("minecraft:food"),
                "component value must be compact JSON with sorted object keys");
    }

    @Test
    void rejectsUnknownReleaseFormat() {
        ObjectNode release = (ObjectNode) tree(bytes(RELEASE_FIXTURE));
        release.put("format", 2);

        ContentFormatException e = assertThrows(ContentFormatException.class,
                () -> ContentReader.read(SOURCE, write(release), bytes(SNAPSHOT_FIXTURE)));

        assertMessageContains(e, "release.json", "2", "supported: 1");
    }

    @Test
    void rejectsUnknownSnapshotFormat() {
        ObjectNode snapshot = (ObjectNode) tree(bytes(SNAPSHOT_FIXTURE));
        snapshot.put("format", 2);

        ContentFormatException e = rejectedSnapshot(snapshot);

        assertMessageContains(e, "snapshot.json", "2", "supported: 1");
    }

    @Test
    void rejectsSnapshotBytesThatDoNotMatchRevision() {
        byte[] snapshot = withTrailingNewline(bytes(SNAPSHOT_FIXTURE));

        ContentFormatException e = assertThrows(ContentFormatException.class,
                () -> ContentReader.read(SOURCE, bytes(RELEASE_FIXTURE), snapshot));

        assertMessageContains(e, "sha256:046d3f84a66ba0940f6686d2679df8ac03d02c0350e0312e79feac5c9318b0b4",
                "sha256:" + sha256Hex(snapshot));
    }

    @Test
    void rejectsUnknownItemProperty() {
        ObjectNode snapshot = (ObjectNode) tree(bytes(SNAPSHOT_FIXTURE));
        ((ObjectNode) snapshot.get("items").get(0)).put("colour", "red");

        ContentFormatException e = rejectedSnapshot(snapshot);

        assertMessageContains(e, SOURCE + ": snapshot.json: $.items[0].colour", "unknown property");
    }

    @Test
    void rejectsMissingRequiredProperty() {
        ObjectNode snapshot = (ObjectNode) tree(bytes(SNAPSHOT_FIXTURE));
        ((ObjectNode) snapshot.get("attributes").get(0)).remove("synced");

        ContentFormatException e = rejectedSnapshot(snapshot);

        assertMessageContains(e, "$.attributes[0].synced", "missing");
    }

    @Test
    void rejectsNullForNonNullableProperty() {
        ObjectNode snapshot = (ObjectNode) tree(bytes(SNAPSHOT_FIXTURE));
        ((ObjectNode) snapshot.get("items").get(0)).putNull("material");

        ContentFormatException e = rejectedSnapshot(snapshot);

        assertMessageContains(e, "$.items[0].material", "must not be null");
    }

    @Test
    void rejectsNonIntegralAmount() {
        ObjectNode snapshot = (ObjectNode) tree(bytes(SNAPSHOT_FIXTURE));
        ((ObjectNode) snapshot.get("items").get(0)).put("amount", 1.5);

        ContentFormatException e = rejectedSnapshot(snapshot);

        assertMessageContains(e, "$.items[0].amount", "expected integer");
    }

    @Test
    void rejectsDuplicateJsonKey() {
        String duplicated = new String(bytes(SNAPSHOT_FIXTURE), UTF_8)
                .replace("\"namespace\":\"cygnus\"", "\"namespace\":\"cygnus\",\"namespace\":\"cygnus\"");
        byte[] snapshot = duplicated.getBytes(UTF_8);

        ContentFormatException e = assertThrows(ContentFormatException.class,
                () -> ContentReader.read(SOURCE, releaseFor(snapshot), snapshot));

        assertMessageContains(e, SOURCE + ": snapshot.json", "Duplicate");
    }

    @Test
    void readsDataJarWithBothMetadataEntries(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve(SOURCE);
        writeJar(jar, Map.of(RELEASE_ENTRY, bytes(RELEASE_FIXTURE), SNAPSHOT_ENTRY, bytes(SNAPSHOT_FIXTURE)));

        ContentRelease content = ContentReader.read(jar);

        assertEquals("cygnus", content.release().project(), "project read from data JAR");
        assertEquals("cygnus", content.snapshot().namespace(), "namespace read from data JAR");
    }

    @Test
    void rejectsDataJarWithoutSnapshot(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve(SOURCE);
        writeJar(jar, Map.of(RELEASE_ENTRY, bytes(RELEASE_FIXTURE)));

        ContentFormatException e = assertThrows(ContentFormatException.class, () -> ContentReader.read(jar));

        assertMessageContains(e, SOURCE + ": missing META-INF/vulpes/snapshot.json");
    }

    private static ContentRelease readFixturePair() {
        return ContentReader.read(SOURCE, bytes(RELEASE_FIXTURE), bytes(SNAPSHOT_FIXTURE));
    }

    private static Snapshot readSnapshot(ObjectNode snapshot) {
        byte[] snapshotBytes = write(snapshot);
        return ContentReader.read(SOURCE, releaseFor(snapshotBytes), snapshotBytes).snapshot();
    }

    private static ContentFormatException rejectedSnapshot(ObjectNode snapshot) {
        byte[] snapshotBytes = write(snapshot);
        return assertThrows(ContentFormatException.class,
                () -> ContentReader.read(SOURCE, releaseFor(snapshotBytes), snapshotBytes));
    }

    private static byte[] releaseFor(byte[] snapshot) {
        ObjectNode release = (ObjectNode) tree(bytes(RELEASE_FIXTURE));
        release.put("revision", "sha256:" + sha256Hex(snapshot));
        return write(release);
    }

    private static byte[] withTrailingNewline(byte[] original) {
        byte[] changed = new byte[original.length + 1];
        System.arraycopy(original, 0, changed, 0, original.length);
        changed[original.length] = '\n';
        return changed;
    }

    private static void writeJar(Path jar, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    private static void assertMessageContains(ContentFormatException e, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(e.getMessage().contains(fragment),
                    "message should contain '" + fragment + "' but was: " + e.getMessage());
        }
    }
}
