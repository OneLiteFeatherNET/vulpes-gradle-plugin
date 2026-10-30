package net.onelitefeather.vulpes.format.diff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.onelitefeather.vulpes.format.Advancement;
import net.onelitefeather.vulpes.format.Attribute;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.ExpressionFunction;
import net.onelitefeather.vulpes.format.ExpressionFunctionKind;
import net.onelitefeather.vulpes.format.FontSymbol;
import net.onelitefeather.vulpes.format.Item;
import net.onelitefeather.vulpes.format.Pack;
import net.onelitefeather.vulpes.format.Snapshot;
import net.onelitefeather.vulpes.format.SoundEvent;

/**
 * Compares two releases of one content project. Entities are matched by {@code id}.
 */
public final class ContentDiff {

    private static final Rules<Item> ITEMS = new Rules<>(Item::id, Item::key, Item::previousKeys,
            item -> fieldsOf("material", item.material(), "amount", item.amount(), "components", item.components()),
            Set.of(), item -> true);
    private static final Rules<Attribute> ATTRIBUTES = new Rules<>(Attribute::id, Attribute::key,
            Attribute::previousKeys,
            attribute -> fieldsOf("displayName", attribute.displayName(), "translationKey", attribute.translationKey(),
                    "defaultValue", attribute.defaultValue(), "minimumValue", attribute.minimumValue(),
                    "maximumValue", attribute.maximumValue(), "synced", attribute.synced()),
            Set.of(), attribute -> true);
    private static final Rules<FontSymbol> FONTS = new Rules<>(FontSymbol::id, FontSymbol::key,
            FontSymbol::previousKeys,
            font -> fieldsOf("font", font.font(), "type", font.type(), "file", font.file(), "ascent", font.ascent(),
                    "height", font.height(), "chars", font.chars()),
            Set.of(), font -> true);
    private static final Rules<SoundEvent> SOUNDS = new Rules<>(SoundEvent::id, SoundEvent::key,
            SoundEvent::previousKeys,
            sound -> fieldsOf("subtitle", sound.subtitle(), "replace", sound.replace(), "sounds", sound.sounds()),
            Set.of(), sound -> true);
    private static final Rules<Advancement> ADVANCEMENTS = new Rules<>(Advancement::id, Advancement::key,
            Advancement::previousKeys,
            advancement -> fieldsOf("parent", advancement.parent(), "icon", advancement.icon(),
                    "frame", advancement.frame(), "title", advancement.title(),
                    "description", advancement.description(), "background", advancement.background(),
                    "x", advancement.x(), "y", advancement.y(), "showToast", advancement.showToast(),
                    "hidden", advancement.hidden(), "announceToChat", advancement.announceToChat()),
            Set.of(), advancement -> true);
    private static final Rules<ExpressionFunction> FUNCTIONS = new Rules<>(ExpressionFunction::id,
            ExpressionFunction::name, ExpressionFunction::previousNames,
            function -> fieldsOf("kind", function.kind(), "description", function.description(),
                    "parameters", function.parameters(), "returnType", function.returnType(),
                    "expression", function.expression()),
            Set.of("kind", "parameters", "returnType"),
            function -> function.kind() == ExpressionFunctionKind.INTERFACE);

    private ContentDiff() {
    }

    /**
     * Compares {@code from} with {@code to}.
     *
     * @throws IllegalArgumentException if the releases belong to different projects or an id occurs twice
     */
    public static DiffReport compare(ContentRelease from, ContentRelease to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        String project = from.release().project();
        if (!project.equals(to.release().project())) {
            throw new IllegalArgumentException("cannot compare project '" + project + "' with project '"
                    + to.release().project() + "'");
        }
        Snapshot before = from.snapshot();
        Snapshot after = to.snapshot();

        Compared<EntityDiff> items = compareEntities(before.items(), after.items(), ITEMS);
        Compared<EntityDiff> attributes = compareEntities(before.attributes(), after.attributes(), ATTRIBUTES);
        Compared<EntityDiff> fonts = compareEntities(before.fonts(), after.fonts(), FONTS);
        Compared<EntityDiff> sounds = compareEntities(before.sounds(), after.sounds(), SOUNDS);
        Compared<EntityDiff> advancements = compareEntities(before.advancements(), after.advancements(), ADVANCEMENTS);
        Compared<EntityDiff> functions = compareEntities(before.expressionFunctions(), after.expressionFunctions(),
                FUNCTIONS);
        Compared<PackDiff> packs = comparePacks(from.release().packs(), to.release().packs());

        VersionBump requiredBump = List.of(items, attributes, fonts, sounds, advancements, functions, packs).stream()
                .map(Compared::bump)
                .reduce(VersionBump.NONE, ContentDiff::higher);
        return new DiffReport(project, from.release().version(), to.release().version(), requiredBump,
                items.diff(), attributes.diff(), fonts.diff(), sounds.diff(), advancements.diff(), functions.diff(),
                packs.diff());
    }

    private static <T> Compared<EntityDiff> compareEntities(List<T> before, List<T> after, Rules<T> rules) {
        Map<UUID, T> previous = index(before, rules.id(), "entity");
        Map<UUID, T> current = index(after, rules.id(), "entity");
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<Rename> renamed = new ArrayList<>();
        List<Change> changed = new ArrayList<>();
        VersionBump bump = VersionBump.NONE;

        for (Map.Entry<UUID, T> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                T old = entry.getValue();
                removed.add(rules.key().apply(old));
                bump = higher(bump, rules.versioned().test(old) ? VersionBump.MAJOR : VersionBump.PATCH);
            }
        }
        for (T now : current.values()) {
            T old = previous.get(rules.id().apply(now));
            if (old == null) {
                added.add(rules.key().apply(now));
                bump = higher(bump, rules.versioned().test(now) ? VersionBump.MINOR : VersionBump.PATCH);
                continue;
            }
            String oldKey = rules.key().apply(old);
            String newKey = rules.key().apply(now);
            boolean versioned = rules.versioned().test(old) || rules.versioned().test(now);
            if (!oldKey.equals(newKey)) {
                boolean aliased = rules.previousKeys().apply(now).contains(oldKey);
                renamed.add(new Rename(oldKey, newKey, aliased));
                bump = higher(bump, !versioned ? VersionBump.PATCH
                        : aliased ? VersionBump.MINOR : VersionBump.MAJOR);
            }
            List<String> fields = changedFields(rules.fields().apply(old), rules.fields().apply(now));
            if (!fields.isEmpty()) {
                boolean signature = fields.stream().anyMatch(rules.signatureFields()::contains);
                changed.add(new Change(newKey, fields, signature));
                bump = higher(bump, signature && versioned ? VersionBump.MAJOR : VersionBump.PATCH);
            }
        }

        added.sort(Comparator.naturalOrder());
        removed.sort(Comparator.naturalOrder());
        renamed.sort(Comparator.comparing(Rename::fromKey));
        changed.sort(Comparator.comparing(Change::key));
        return new Compared<>(new EntityDiff(added, removed, renamed, changed), bump);
    }

    private static Compared<PackDiff> comparePacks(List<Pack> before, List<Pack> after) {
        Map<String, Pack> previous = index(before, Pack::id, "pack");
        Map<String, Pack> current = index(after, Pack::id, "pack");
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        for (String id : current.keySet()) {
            if (!previous.containsKey(id)) {
                added.add(id);
            } else if (!previous.get(id).sha1().equals(current.get(id).sha1())) {
                changed.add(id);
            }
        }
        for (String id : previous.keySet()) {
            if (!current.containsKey(id)) {
                removed.add(id);
            }
        }
        added.sort(Comparator.naturalOrder());
        removed.sort(Comparator.naturalOrder());
        changed.sort(Comparator.naturalOrder());
        boolean any = !added.isEmpty() || !removed.isEmpty() || !changed.isEmpty();
        return new Compared<>(new PackDiff(added, removed, changed), any ? VersionBump.PATCH : VersionBump.NONE);
    }

    private static <K, T> Map<K, T> index(List<T> entities, Function<T, K> id, String what) {
        Map<K, T> index = new LinkedHashMap<>();
        for (T entity : entities) {
            if (index.put(id.apply(entity), entity) != null) {
                throw new IllegalArgumentException("duplicate " + what + " id " + id.apply(entity));
            }
        }
        return index;
    }

    private static List<String> changedFields(Map<String, Object> before, Map<String, Object> after) {
        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, Object> field : before.entrySet()) {
            if (!Objects.equals(field.getValue(), after.get(field.getKey()))) {
                changed.add(field.getKey());
            }
        }
        return changed;
    }

    private static Map<String, Object> fieldsOf(Object... nameAndValue) {
        Map<String, Object> fields = new TreeMap<>();
        for (int i = 0; i < nameAndValue.length; i += 2) {
            fields.put((String) nameAndValue[i], nameAndValue[i + 1]);
        }
        return fields;
    }

    private static VersionBump higher(VersionBump a, VersionBump b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /**
     * How one entity type is matched and which version bump its changes require.
     *
     * @param signatureFields fields that form the signature; changing one of them is a signature change
     * @param versioned entities whose keys the game depends on, so their added, removed, renamed and changed
     *                  signature entries count for the version; all other entities require a patch only
     */
    private record Rules<T>(Function<T, UUID> id, Function<T, String> key, Function<T, List<String>> previousKeys,
                            Function<T, Map<String, Object>> fields, Set<String> signatureFields,
                            Predicate<T> versioned) {
    }

    private record Compared<D>(D diff, VersionBump bump) {
    }
}
