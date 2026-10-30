package net.onelitefeather.vulpes.format.diff;

import java.util.List;

/**
 * The differences of one entity type. Every list is sorted.
 */
public record EntityDiff(List<String> added, List<String> removed, List<Rename> renamed, List<Change> changed) {

    public EntityDiff {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
        renamed = List.copyOf(renamed);
        changed = List.copyOf(changed);
    }

    public boolean hasChanges() {
        return !added.isEmpty() || !removed.isEmpty() || !renamed.isEmpty() || !changed.isEmpty();
    }
}
