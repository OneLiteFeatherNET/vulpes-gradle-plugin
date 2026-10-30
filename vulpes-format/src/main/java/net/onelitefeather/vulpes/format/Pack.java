package net.onelitefeather.vulpes.format;

import java.util.Objects;

/**
 * One resource pack variant of a release.
 */
public record Pack(String id, int resolution, String url, String sha1, long size) {

    public Pack {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(sha1, "sha1");
    }
}
