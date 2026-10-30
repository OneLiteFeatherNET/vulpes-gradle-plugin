package net.onelitefeather.vulpes.format;

import java.util.Objects;

/**
 * A complete content release: its descriptor and the snapshot it refers to.
 */
public record ContentRelease(Release release, Snapshot snapshot) {

    public ContentRelease {
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(snapshot, "snapshot");
    }
}
