package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Content of {@code META-INF/vulpes/release.json}.
 */
public record Release(int format, String project, UUID projectId, String version, String revision, List<Pack> packs,
                      Provenance provenance) {

    public Release {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(revision, "revision");
        packs = List.copyOf(packs);
        Objects.requireNonNull(provenance, "provenance");
    }
}
