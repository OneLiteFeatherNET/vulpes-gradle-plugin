package net.onelitefeather.vulpes.format.diff;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.onelitefeather.vulpes.format.Attribute;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.ExpressionFunction;
import net.onelitefeather.vulpes.format.ExpressionFunctionKind;
import net.onelitefeather.vulpes.format.ExpressionParameter;
import net.onelitefeather.vulpes.format.ExpressionType;
import net.onelitefeather.vulpes.format.Item;
import net.onelitefeather.vulpes.format.Pack;
import net.onelitefeather.vulpes.format.PackSettings;
import net.onelitefeather.vulpes.format.Provenance;
import net.onelitefeather.vulpes.format.Release;
import net.onelitefeather.vulpes.format.Snapshot;

/**
 * Small in-memory releases for the diff tests. Every value is built here, nothing is read from files.
 */
final class DiffFixtures {

    static final UUID PROJECT_ID = UUID.fromString("5b0c7a52-3c1e-4d7a-9f2e-0a1b2c3d4e5f");
    static final UUID SWORD = UUID.fromString("12f6f240-843f-5833-8fba-a8ffbe31c173");
    static final UUID CRIT = UUID.fromString("ff4741e6-d71f-5ef3-9fc3-d24e4d136193");
    static final UUID IN_ARENA = UUID.fromString("709b4d25-2545-5bde-a8bf-06d9fef28803");
    static final UUID ARMOR_DAMAGE = UUID.fromString("c91dab27-e52f-5b83-aa81-cb2b3d8a971e");
    static final String OLD_SWORD = "cygnus:old_sword";
    static final String IRON_SWORD = "cygnus:iron_sword";
    static final String STEEL_SWORD = "cygnus:steel_sword";

    private DiffFixtures() {
    }

    static Item item(UUID id, String key, List<String> previousKeys) {
        return new Item(id, previousKeys, key, "minecraft:iron_sword", 1, Map.of());
    }

    static Attribute attribute(UUID id, String key, double maximumValue) {
        return new Attribute(id, List.of(), key, "Krit-Chance", null, 0.0, 0.0, maximumValue, true);
    }

    static ExpressionFunction inArena(List<ExpressionParameter> parameters) {
        return new ExpressionFunction(IN_ARENA, List.of(), "inArena", ExpressionFunctionKind.INTERFACE,
                "Arena", parameters, ExpressionType.BOOLEAN, null);
    }

    static ExpressionFunction armorDamage(String expression) {
        return new ExpressionFunction(ARMOR_DAMAGE, List.of(), "armorDamage", ExpressionFunctionKind.FORMULA,
                "Schaden", List.of(new ExpressionParameter("base", ExpressionType.NUMBER)), ExpressionType.NUMBER,
                expression);
    }

    static Snapshot snapshot(List<Item> items, List<Attribute> attributes, List<ExpressionFunction> functions) {
        return new Snapshot(1, PROJECT_ID, "cygnus", items, attributes, List.of(), List.of(), List.of(), functions,
                List.of(), new PackSettings(List.of(16), "{\"text\":\"Cygnus\"}"));
    }

    static Pack pack(String id, String sha1) {
        return new Pack(id, 16, "https://cdn.onelitefeather.net/packs/cygnus/" + sha1 + ".zip", sha1, 1L);
    }

    static ContentRelease content(String version, Snapshot snapshot, List<Pack> packs) {
        Release release = new Release(1, "cygnus", PROJECT_ID, version, "sha256:unused", packs,
                new Provenance("2.0.0", "stelaris", Instant.parse("2026-10-29T10:00:00Z")));
        return new ContentRelease(release, snapshot);
    }

    static ContentRelease content(String version, Snapshot snapshot) {
        return content(version, snapshot, List.of());
    }
}
