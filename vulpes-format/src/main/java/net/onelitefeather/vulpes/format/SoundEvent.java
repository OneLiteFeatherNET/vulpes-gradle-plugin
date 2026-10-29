package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * A sound event that plays one of its entries.
 */
public record SoundEvent(UUID id, List<String> previousKeys, String key, @Nullable String subtitle, boolean replace,
                         List<SoundEntry> sounds) {

    public SoundEvent {
        Objects.requireNonNull(id, "id");
        previousKeys = List.copyOf(previousKeys);
        Objects.requireNonNull(key, "key");
        sounds = List.copyOf(sounds);
    }
}
