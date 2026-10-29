package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.onelitefeather.vulpes.format.Advancement;
import net.onelitefeather.vulpes.format.AdvancementFrame;
import net.onelitefeather.vulpes.format.Attribute;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.ExpressionFunction;
import net.onelitefeather.vulpes.format.ExpressionFunctionKind;
import net.onelitefeather.vulpes.format.ExpressionParameter;
import net.onelitefeather.vulpes.format.ExpressionType;
import net.onelitefeather.vulpes.format.FontSymbol;
import net.onelitefeather.vulpes.format.Item;
import net.onelitefeather.vulpes.format.Pack;
import net.onelitefeather.vulpes.format.PackSettings;
import net.onelitefeather.vulpes.format.Provenance;
import net.onelitefeather.vulpes.format.Release;
import net.onelitefeather.vulpes.format.Snapshot;
import net.onelitefeather.vulpes.format.SoundEntry;
import net.onelitefeather.vulpes.format.SoundEntryType;
import net.onelitefeather.vulpes.format.SoundEvent;
import net.onelitefeather.vulpes.format.fixtures.DataJarFixtures;

/**
 * Small in-memory contents for the generator tests. Every call returns new immutable records.
 */
final class TestContents {

    static final String NAMESPACE = "cygnus";
    static final String VERSION = "1.4.0";
    static final String PACKAGE = "net.onelitefeather.vulpes.content.cygnus";
    static final GenerationContext CONTEXT = new GenerationContext(PACKAGE, "cygnus", VERSION, NAMESPACE);

    private static final UUID PROJECT_ID = UUID.fromString("5b0c7a52-3c1e-4d7a-9f2e-0a1b2c3d4e5f");

    private TestContents() {
    }

    /** The contract fixture of project {@code cygnus}, as read from its data JAR. */
    static ContentRelease fixture() {
        return ContentReader.read("fixture", DataJarFixtures.releaseJson(), DataJarFixtures.snapshotJson());
    }

    static Item item(String key, String material, int amount, Map<String, String> components, String... previousKeys) {
        return new Item(id(key), List.of(previousKeys), key, material, amount, components);
    }

    static Attribute attribute(String key, String displayName, String translationKey, double minimum,
                               double defaultValue, double maximum, boolean synced, String... previousKeys) {
        return new Attribute(id(key), List.of(previousKeys), key, displayName, translationKey, defaultValue,
                minimum, maximum, synced);
    }

    static SoundEvent sound(String key, String... previousKeys) {
        SoundEntry entry = new SoundEntry("cygnus:ui/click", SoundEntryType.FILE, 1.0, 1.0, 1, false, 16, false);
        return new SoundEvent(id(key), List.of(previousKeys), key, null, false, List.of(entry));
    }

    static FontSymbol font(String key, String font, String file, int ascent, int height, List<String> chars,
                           String... previousKeys) {
        return new FontSymbol(id(key), List.of(previousKeys), key, font, "bitmap", file, ascent, height, chars);
    }

    static Advancement advancement(String key, String parent, String background, AdvancementFrame frame,
                                   double x, double y) {
        return new Advancement(id(key), List.of(), key, parent, "minecraft:iron_sword", frame,
                "{\"text\":\"Erster Kill\"}", "{\"text\":\"Besiege einen Gegner\"}", background, x, y,
                true, false, false);
    }

    static ExpressionFunction function(String name, ExpressionFunctionKind kind, ExpressionType returnType,
                                       ExpressionParameter... parameters) {
        return new ExpressionFunction(id(name), List.of(), name, kind, null, List.of(parameters), returnType, null);
    }

    static ExpressionParameter parameter(String name, ExpressionType type) {
        return new ExpressionParameter(name, type);
    }

    static Pack pack(String id, int resolution, String url, String sha1) {
        return new Pack(id, resolution, url, sha1, 1L);
    }

    static ContentRelease content(Snapshot snapshot, Pack... packs) {
        Release release = new Release(1, "cygnus", PROJECT_ID, VERSION, "sha256:test", List.of(packs),
                new Provenance("2.0.0", "stelaris", Instant.parse("2026-10-29T10:00:00Z")));
        return new ContentRelease(release, snapshot);
    }

    static Snapshot empty() {
        return new Builder().build();
    }

    static Builder snapshot() {
        return new Builder();
    }

    static UUID id(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(UTF_8));
    }

    /**
     * Collects the entities of one snapshot; every list not set stays empty.
     */
    static final class Builder {

        private final List<Item> items = new ArrayList<>();
        private final List<Attribute> attributes = new ArrayList<>();
        private final List<FontSymbol> fonts = new ArrayList<>();
        private final List<SoundEvent> sounds = new ArrayList<>();
        private final List<Advancement> advancements = new ArrayList<>();
        private final List<ExpressionFunction> functions = new ArrayList<>();

        Builder item(Item item) {
            items.add(item);
            return this;
        }

        Builder attribute(Attribute attribute) {
            attributes.add(attribute);
            return this;
        }

        Builder font(FontSymbol font) {
            fonts.add(font);
            return this;
        }

        Builder sound(SoundEvent sound) {
            sounds.add(sound);
            return this;
        }

        Builder advancement(Advancement advancement) {
            advancements.add(advancement);
            return this;
        }

        Builder function(ExpressionFunction function) {
            functions.add(function);
            return this;
        }

        Snapshot build() {
            return new Snapshot(1, PROJECT_ID, NAMESPACE, items, attributes, fonts, sounds, advancements, functions,
                    List.of(), new PackSettings(List.of(16, 32), "{\"text\":\"Cygnus\"}"));
        }
    }
}
