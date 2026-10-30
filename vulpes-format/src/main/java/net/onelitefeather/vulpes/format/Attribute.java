package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * A numeric player attribute.
 */
public record Attribute(UUID id, List<String> previousKeys, String key, String displayName,
                        @Nullable String translationKey, double defaultValue, double minimumValue,
                        double maximumValue, boolean synced) {

    public Attribute {
        Objects.requireNonNull(id, "id");
        previousKeys = List.copyOf(previousKeys);
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(displayName, "displayName");
    }
}
