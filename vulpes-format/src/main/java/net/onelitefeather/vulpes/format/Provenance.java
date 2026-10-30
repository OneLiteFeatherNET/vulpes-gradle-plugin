package net.onelitefeather.vulpes.format;

import java.time.Instant;
import java.util.Objects;

/**
 * Who and what produced a release.
 */
public record Provenance(String generator, String createdBy, Instant createdAt) {

    public Provenance {
        Objects.requireNonNull(generator, "generator");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
