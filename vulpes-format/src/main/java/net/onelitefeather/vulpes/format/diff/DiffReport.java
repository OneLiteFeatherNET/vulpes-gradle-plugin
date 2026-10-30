package net.onelitefeather.vulpes.format.diff;

import java.util.Objects;

/**
 * What changed from one release of a content project to another.
 *
 * @param requiredBump the smallest version bump the changes require
 */
public record DiffReport(String project, String fromVersion, String toVersion, VersionBump requiredBump,
                         EntityDiff items, EntityDiff attributes, EntityDiff fonts, EntityDiff sounds,
                         EntityDiff advancements, EntityDiff expressionFunctions, EntityDiff expressions,
                         PackDiff packs) {

    public DiffReport {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(fromVersion, "fromVersion");
        Objects.requireNonNull(toVersion, "toVersion");
        Objects.requireNonNull(requiredBump, "requiredBump");
    }

    /** Whether any entity or pack differs. */
    public boolean hasChanges() {
        return items.hasChanges() || attributes.hasChanges() || fonts.hasChanges() || sounds.hasChanges()
                || advancements.hasChanges() || expressionFunctions.hasChanges() || expressions.hasChanges()
                || packs.hasChanges();
    }

    /** Markdown for people, with the changes grouped by entity type. */
    public String toMarkdown() {
        return DiffMarkdown.render(this);
    }

    /** JSON for tooling, with sorted object keys and two-space indentation. */
    public String toJson() {
        return DiffJson.render(this);
    }
}
