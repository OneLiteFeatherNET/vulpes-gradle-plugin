package net.onelitefeather.vulpes.format;

import java.util.Objects;

/**
 * One sound played by a sound event.
 */
public record SoundEntry(String name, SoundEntryType type, double volume, double pitch, int weight, boolean stream,
                         int attenuationDistance, boolean preload) {

    public SoundEntry {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}
