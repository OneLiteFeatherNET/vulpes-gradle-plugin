package net.onelitefeather.vulpes.format;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * A client resource the resource packs are built from.
 *
 * @param resolution pixels per block for textures, {@code null} otherwise
 */
public record Asset(String path, String sha1, long size, AssetKind kind, @Nullable Integer resolution) {

    public Asset {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(sha1, "sha1");
        Objects.requireNonNull(kind, "kind");
    }
}
