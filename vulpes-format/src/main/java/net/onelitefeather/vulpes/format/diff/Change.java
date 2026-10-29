package net.onelitefeather.vulpes.format.diff;

import java.util.List;

/**
 * An entity with the same id and key whose values differ.
 *
 * @param key the key in the new release
 * @param fields names of the changed fields, sorted
 * @param signatureChanged whether a field of the expression function signature changed; always {@code false}
 *                         for entities other than expression functions
 */
public record Change(String key, List<String> fields, boolean signatureChanged) {

    public Change {
        fields = List.copyOf(fields);
    }
}
