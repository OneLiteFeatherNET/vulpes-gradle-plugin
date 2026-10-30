package net.onelitefeather.vulpes.format;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * A custom item.
 *
 * @param components component id to compact JSON of its value, sorted by component id
 */
public record Item(UUID id, List<String> previousKeys, String key, String material, int amount,
                   Map<String, String> components) {

    public Item {
        Objects.requireNonNull(id, "id");
        previousKeys = List.copyOf(previousKeys);
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(material, "material");
        components = Collections.unmodifiableSortedMap(new TreeMap<>(components));
    }
}
