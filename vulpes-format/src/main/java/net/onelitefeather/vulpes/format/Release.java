package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Content of {@code META-INF/vulpes/release.json}.
 *
 * @param base where the release was built from; {@code null} when the release records none (format 1, or format 2 without
 *             {@code base}). Informational only.
 */
public record Release(int format, String project, UUID projectId, String version, String revision, List<Pack> packs,
                      Provenance provenance, Base base) {

    public Release {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(revision, "revision");
        packs = List.copyOf(packs);
        Objects.requireNonNull(provenance, "provenance");
    }

    /**
     * A release without a base record.
     */
    public Release(int format, String project, UUID projectId, String version, String revision, List<Pack> packs,
                   Provenance provenance) {
        this(format, project, projectId, version, revision, packs, provenance, null);
    }

    /**
     * The vulpes-base source a release was generated with.
     *
     * @param repository the repository URL
     * @param branch the branch name
     * @param commit the full 40-character lower-case SHA-1 of the commit
     */
    public record Base(String repository, String branch, String commit) {

        public Base {
            Objects.requireNonNull(repository, "repository");
            Objects.requireNonNull(branch, "branch");
            Objects.requireNonNull(commit, "commit");
        }
    }
}
