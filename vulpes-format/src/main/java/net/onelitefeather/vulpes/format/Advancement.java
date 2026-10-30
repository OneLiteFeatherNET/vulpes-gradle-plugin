package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * An advancement; {@code title} and {@code description} are compact JSON text components.
 */
public record Advancement(UUID id, List<String> previousKeys, String key, @Nullable String parent, String icon,
                          AdvancementFrame frame, String title, String description, @Nullable String background,
                          double x, double y, boolean showToast, boolean hidden, boolean announceToChat) {

    public Advancement {
        Objects.requireNonNull(id, "id");
        previousKeys = List.copyOf(previousKeys);
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
    }
}
