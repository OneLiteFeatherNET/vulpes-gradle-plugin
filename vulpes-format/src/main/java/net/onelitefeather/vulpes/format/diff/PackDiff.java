package net.onelitefeather.vulpes.format.diff;

import java.util.List;

/**
 * Packs of the release by id. Every list is sorted.
 *
 * @param changed ids of packs whose sha1 differs
 */
public record PackDiff(List<String> added, List<String> removed, List<String> changed) {

    public PackDiff {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
        changed = List.copyOf(changed);
    }

    public boolean hasChanges() {
        return !added.isEmpty() || !removed.isEmpty() || !changed.isEmpty();
    }
}
