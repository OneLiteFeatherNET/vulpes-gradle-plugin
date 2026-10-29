package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;

/**
 * Build parameters of the resource packs.
 *
 * @param description pack description as compact JSON text component
 */
public record PackSettings(List<Integer> resolutions, String description) {

    public PackSettings {
        resolutions = List.copyOf(resolutions);
        Objects.requireNonNull(description, "description");
    }
}
