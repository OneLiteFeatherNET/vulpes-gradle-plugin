package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A bitmap font symbol; symbols sharing a font belong to one font JSON.
 */
public record FontSymbol(UUID id, List<String> previousKeys, String key, String font, String type, String file,
                         int ascent, int height, List<String> chars) {

    public FontSymbol {
        Objects.requireNonNull(id, "id");
        previousKeys = List.copyOf(previousKeys);
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(file, "file");
        chars = List.copyOf(chars);
    }
}
