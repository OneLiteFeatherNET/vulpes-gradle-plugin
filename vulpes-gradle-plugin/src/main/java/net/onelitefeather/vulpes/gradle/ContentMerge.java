package net.onelitefeather.vulpes.gradle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.onelitefeather.vulpes.format.Advancement;
import net.onelitefeather.vulpes.format.Asset;
import net.onelitefeather.vulpes.format.Attribute;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.Expression;
import net.onelitefeather.vulpes.format.ExpressionFunction;
import net.onelitefeather.vulpes.format.FontSymbol;
import net.onelitefeather.vulpes.format.Item;
import net.onelitefeather.vulpes.format.Snapshot;
import net.onelitefeather.vulpes.format.SoundEntry;
import net.onelitefeather.vulpes.format.SoundEntryType;
import net.onelitefeather.vulpes.format.SoundEvent;

/**
 * Merges a local snapshot over the published snapshot of the same content project, entry by entry. Uses no Gradle
 * types.
 *
 * <p>In the lists items, attributes, fonts, sounds, advancements, expression functions and expressions an entry is
 * matched by its {@code id}, assets by their {@code path}. A local entry replaces the published entry with the same
 * identity as a whole, a local entry with a new identity is added, and a published entry without a local counterpart
 * is kept. Nothing is deleted. The merged lists are sorted as the schema requires, and every cross-entry rule of a
 * single snapshot is checked on the merged result, naming the side (local or published) of each conflicting entry.
 */
final class ContentMerge {

    private static final String LOCAL = "local";
    private static final String PUBLISHED = "published";

    /**
     * The merged content, the number of local entries that replaced a published entry and that were added, counted
     * over all lists, and the references to resource locations without an asset.
     *
     * @param content the merged release, with the release and pack of the local side
     * @param replaced local entries whose identity exists in the published snapshot
     * @param added local entries whose identity does not exist in the published snapshot
     * @param missingAssets one entry per reference to a resource location that has no asset
     */
    record Merged(ContentRelease content, int replaced, int added, List<MissingAsset> missingAssets) {
    }

    /**
     * A reference from an entry to a resource location that no asset of the snapshot provides. It is a warning, not a
     * failure, so that a local build can be tested before its assets are complete.
     *
     * @param project the content project name
     * @param entry the referencing entry, as {@code kind id 'key'}
     * @param side the side of the referencing entry, {@code local} or {@code published}
     * @param location the resource location the entry uses
     */
    record MissingAsset(String project, String entry, String side, String location) {

        /** The warning text, without a prefix. */
        String message() {
            return project + ": " + entry + " (" + side + ") uses resource location '" + location
                    + "' that has no asset entry";
        }
    }

    private final String project;
    private final Map<Object, String> sides = new IdentityHashMap<>();
    private final Map<Object, Object> replacedBy = new IdentityHashMap<>();
    private final List<MissingAsset> missingAssets = new ArrayList<>();
    private int replaced;
    private int added;

    private ContentMerge(String project) {
        this.project = project;
    }

    /**
     * Merges the local content over the published content of the same project.
     *
     * @param project the content project name, for error messages
     * @param published the published release and snapshot of the data JAR
     * @param local the local release and snapshot, which passed the format checks
     * @return the merged content
     * @throws LocalContentException if the projects differ or a merged cross-entry rule fails
     */
    static Merged merge(String project, ContentRelease published, ContentRelease local) {
        return new ContentMerge(project).run(published, local);
    }

    private Merged run(ContentRelease published, ContentRelease local) {
        Snapshot before = published.snapshot();
        Snapshot after = local.snapshot();
        if (!before.projectId().equals(after.projectId())) {
            throw fail("local projectId " + after.projectId() + " differs from the published projectId "
                    + before.projectId());
        }
        if (!before.namespace().equals(after.namespace())) {
            throw fail("local namespace '" + after.namespace() + "' differs from the published namespace '"
                    + before.namespace() + "'");
        }
        Snapshot merged = new Snapshot(after.format(), after.projectId(), after.namespace(),
                mergeList(before.items(), after.items(), Item::id, Item::key),
                mergeList(before.attributes(), after.attributes(), Attribute::id, Attribute::key),
                mergeList(before.fonts(), after.fonts(), FontSymbol::id, FontSymbol::key),
                mergeList(before.sounds(), after.sounds(), SoundEvent::id, SoundEvent::key),
                mergeList(before.advancements(), after.advancements(), Advancement::id, Advancement::key),
                mergeList(before.expressionFunctions(), after.expressionFunctions(), ExpressionFunction::id,
                        ExpressionFunction::name),
                mergeList(before.expressions(), after.expressions(), Expression::id, Expression::name),
                mergeList(before.assets(), after.assets(), Asset::path, Asset::path),
                after.pack());
        check(merged);
        return new Merged(new ContentRelease(local.release(), merged), replaced, added, List.copyOf(missingAssets));
    }

    /**
     * Checks a local-only snapshot with the cross-entry rules of a single snapshot. Every entry comes from the local
     * side, so a failure names {@code local} for each entry it names.
     *
     * @param project the content project name, for error messages
     * @param local the local release and snapshot of a project that has no published release
     * @return one entry per reference to a resource location that has no asset
     * @throws LocalContentException if a rule fails
     */
    static List<MissingAsset> checkLocalOnly(String project, ContentRelease local) {
        ContentMerge checker = new ContentMerge(project);
        Snapshot s = local.snapshot();
        for (Object entry : concat(s.items(), s.attributes(), s.fonts(), s.sounds(), s.advancements(),
                s.expressionFunctions(), s.expressions(), s.assets())) {
            checker.sides.put(entry, LOCAL);
        }
        checker.check(s);
        return List.copyOf(checker.missingAssets);
    }

    private <T> List<T> mergeList(List<T> published, List<T> local, Function<T, Object> identity,
                                  Function<T, String> order) {
        Map<Object, T> publishedById = new HashMap<>();
        for (T entry : published) {
            publishedById.putIfAbsent(identity.apply(entry), entry);
        }
        Set<Object> localIds = new HashSet<>();
        for (T entry : local) {
            localIds.add(identity.apply(entry));
        }
        List<T> merged = new ArrayList<>();
        for (T entry : published) {
            if (!localIds.contains(identity.apply(entry))) {
                merged.add(entry);
                sides.put(entry, PUBLISHED);
            }
        }
        for (T entry : local) {
            merged.add(entry);
            sides.put(entry, LOCAL);
            T previous = publishedById.get(identity.apply(entry));
            if (previous == null) {
                added++;
            } else {
                replaced++;
                replacedBy.put(previous, entry);
            }
        }
        merged.sort(Comparator.comparing(order));
        return merged;
    }

    /** The cross-entry rules of a single snapshot, run on the merged result. */
    private void check(Snapshot s) {
        checkIdsUnique(s);
        checkUnique("key", s.items(), ContentMerge::label);
        checkUnique("key", s.attributes(), ContentMerge::label);
        checkUnique("key", s.fonts(), ContentMerge::label);
        checkUnique("key", s.sounds(), ContentMerge::label);
        checkUnique("key", s.advancements(), ContentMerge::label);
        checkUnique("name", concat(s.expressionFunctions(), s.expressions()), ContentMerge::label);
        checkUnique("path", s.assets(), ContentMerge::label);
        checkPreviousDiffer(s.items());
        checkPreviousDiffer(s.attributes());
        checkPreviousDiffer(s.fonts());
        checkPreviousDiffer(s.sounds());
        checkPreviousDiffer(s.advancements());
        checkPreviousDiffer(s.expressionFunctions());
        checkPreviousDiffer(s.expressions());
        checkResources(s);
        checkAdvancements(s);
    }

    private void checkIdsUnique(Snapshot s) {
        Map<Object, Object> byId = new HashMap<>();
        for (Object entry : concat(s.items(), s.attributes(), s.fonts(), s.sounds(), s.advancements(),
                s.expressionFunctions(), s.expressions())) {
            Object other = byId.putIfAbsent(identity(entry), entry);
            if (other != null) {
                throw fail("id " + identity(entry) + " is used by " + describe(other) + " and " + describe(entry));
            }
        }
    }

    private <T> void checkUnique(String what, Collection<T> entries, Function<T, String> label) {
        Map<String, T> byLabel = new HashMap<>();
        for (T entry : entries) {
            T other = byLabel.putIfAbsent(label.apply(entry), entry);
            if (other != null) {
                throw fail(what + " '" + label.apply(entry) + "' is used by " + describe(other) + " and "
                        + describe(entry));
            }
        }
    }

    private <T> void checkPreviousDiffer(List<T> entries) {
        Map<String, T> byLabel = new HashMap<>();
        for (T entry : entries) {
            byLabel.putIfAbsent(label(entry), entry);
        }
        for (T entry : entries) {
            for (String previous : previous(entry)) {
                T holder = byLabel.get(previous);
                if (holder != null) {
                    throw fail("previous key '" + previous + "' of " + describe(entry)
                            + " equals the current key of " + describe(holder));
                }
            }
        }
    }

    private void checkResources(Snapshot s) {
        Set<String> paths = new HashSet<>();
        s.assets().forEach(asset -> paths.add(asset.path()));
        for (FontSymbol font : s.fonts()) {
            requireAsset(paths, font, font.file());
        }
        for (SoundEvent sound : s.sounds()) {
            for (SoundEntry entry : sound.sounds()) {
                if (entry.type() == SoundEntryType.FILE) {
                    requireAsset(paths, sound, entry.name());
                }
            }
        }
        for (Advancement advancement : s.advancements()) {
            if (advancement.background() != null) {
                requireAsset(paths, advancement, advancement.background());
            }
        }
    }

    private void requireAsset(Set<String> paths, Object owner, String location) {
        for (String candidate : assetPaths(location)) {
            if (paths.contains(candidate)) {
                return;
            }
        }
        missingAssets.add(new MissingAsset(project, describeEntry(owner), sides.get(owner), location));
    }

    /**
     * The pack paths a resource location can map to: the location as it is, under {@code textures}, or a sound file of
     * the same name. The format does not define the mapping, so the candidates are the layouts the packs use.
     */
    static List<String> assetPaths(String location) {
        int colon = location.indexOf(':');
        if (colon < 0) {
            return List.of();
        }
        String namespace = location.substring(0, colon);
        String path = location.substring(colon + 1);
        return List.of("assets/" + namespace + "/" + path, "assets/" + namespace + "/textures/" + path,
                "assets/" + namespace + "/sounds/" + path + ".ogg", "assets/" + namespace + "/sounds/" + path);
    }

    private void checkAdvancements(Snapshot s) {
        Map<String, Advancement> byKey = new HashMap<>();
        s.advancements().forEach(advancement -> byKey.putIfAbsent(advancement.key(), advancement));
        for (Advancement advancement : s.advancements()) {
            String parent = advancement.parent();
            if (parent != null && !byKey.containsKey(parent)) {
                throw fail(describe(advancement) + " names parent '" + parent + "', which no advancement has"
                        + replacementOf(parent));
            }
        }
        for (Advancement advancement : s.advancements()) {
            List<String> chain = new ArrayList<>();
            Advancement node = advancement;
            while (node.parent() != null) {
                chain.add(node.key());
                if (chain.subList(0, chain.size() - 1).contains(node.key())) {
                    throw fail("advancement parents form a cycle: " + String.join(" -> ", chain));
                }
                node = byKey.get(node.parent());
            }
        }
    }

    /** Names the local replacement when a missing parent key belonged to a published entry that was replaced. */
    private String replacementOf(String key) {
        for (Map.Entry<Object, Object> replacement : replacedBy.entrySet()) {
            if (replacement.getKey() instanceof Advancement published && key.equals(published.key())) {
                return "; the published advancement with that key was replaced by " + describe(replacement.getValue());
            }
        }
        return "";
    }

    private String describe(Object entry) {
        return describeEntry(entry) + " (" + sides.get(entry) + ")";
    }

    private String describeEntry(Object entry) {
        return kind(entry) + " " + identity(entry) + " '" + label(entry) + "'";
    }

    private static String kind(Object entry) {
        if (entry instanceof Item) {
            return "item";
        }
        if (entry instanceof Attribute) {
            return "attribute";
        }
        if (entry instanceof FontSymbol) {
            return "font";
        }
        if (entry instanceof SoundEvent) {
            return "sound";
        }
        if (entry instanceof Advancement) {
            return "advancement";
        }
        if (entry instanceof ExpressionFunction) {
            return "expression function";
        }
        if (entry instanceof Expression) {
            return "expression";
        }
        return "asset";
    }

    private static Object identity(Object entry) {
        return entry instanceof Asset asset ? asset.path() : idOf(entry);
    }

    private static Object idOf(Object entry) {
        if (entry instanceof Item item) {
            return item.id();
        }
        if (entry instanceof Attribute attribute) {
            return attribute.id();
        }
        if (entry instanceof FontSymbol font) {
            return font.id();
        }
        if (entry instanceof SoundEvent sound) {
            return sound.id();
        }
        if (entry instanceof Advancement advancement) {
            return advancement.id();
        }
        if (entry instanceof ExpressionFunction function) {
            return function.id();
        }
        return ((Expression) entry).id();
    }

    private static String label(Object entry) {
        if (entry instanceof Item item) {
            return item.key();
        }
        if (entry instanceof Attribute attribute) {
            return attribute.key();
        }
        if (entry instanceof FontSymbol font) {
            return font.key();
        }
        if (entry instanceof SoundEvent sound) {
            return sound.key();
        }
        if (entry instanceof Advancement advancement) {
            return advancement.key();
        }
        if (entry instanceof ExpressionFunction function) {
            return function.name();
        }
        if (entry instanceof Expression expression) {
            return expression.name();
        }
        return ((Asset) entry).path();
    }

    private static List<String> previous(Object entry) {
        if (entry instanceof Item item) {
            return item.previousKeys();
        }
        if (entry instanceof Attribute attribute) {
            return attribute.previousKeys();
        }
        if (entry instanceof FontSymbol font) {
            return font.previousKeys();
        }
        if (entry instanceof SoundEvent sound) {
            return sound.previousKeys();
        }
        if (entry instanceof Advancement advancement) {
            return advancement.previousKeys();
        }
        if (entry instanceof ExpressionFunction function) {
            return function.previousNames();
        }
        if (entry instanceof Expression expression) {
            return expression.previousNames();
        }
        return List.of();
    }

    private static List<Object> concat(List<?>... lists) {
        List<Object> all = new ArrayList<>();
        for (List<?> list : lists) {
            all.addAll(list);
        }
        return all;
    }

    private LocalContentException fail(String detail) {
        return new LocalContentException(project + ": " + detail);
    }
}
