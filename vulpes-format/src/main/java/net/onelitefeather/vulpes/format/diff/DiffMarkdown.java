package net.onelitefeather.vulpes.format.diff;

import java.util.List;

/**
 * Renders a {@link DiffReport} as Markdown for people. Only sections with changes are written.
 */
final class DiffMarkdown {

    private DiffMarkdown() {
    }

    static String render(DiffReport report) {
        StringBuilder out = new StringBuilder();
        out.append("# Vulpes diff: ").append(report.project()).append(' ').append(report.fromVersion())
                .append(" -> ").append(report.toVersion()).append("\n\n");
        if (!report.hasChanges()) {
            out.append("No changes. No version bump is required.\n");
            return out.toString();
        }
        out.append("Required version bump: ").append(report.requiredBump()).append("\n");
        entitySection(out, "Items", report.items());
        entitySection(out, "Attributes", report.attributes());
        entitySection(out, "Fonts", report.fonts());
        entitySection(out, "Sound events", report.sounds());
        entitySection(out, "Advancements", report.advancements());
        entitySection(out, "Expression functions", report.expressionFunctions());
        entitySection(out, "Expressions", report.expressions());
        if (report.packs().hasChanges()) {
            out.append("\n## Packs\n");
            keyList(out, "Added", report.packs().added());
            keyList(out, "Removed", report.packs().removed());
            keyList(out, "Changed sha1", report.packs().changed());
        }
        return out.toString();
    }

    private static void entitySection(StringBuilder out, String title, EntityDiff diff) {
        if (!diff.hasChanges()) {
            return;
        }
        out.append("\n## ").append(title).append('\n');
        keyList(out, "Added", diff.added());
        keyList(out, "Removed", diff.removed());
        if (!diff.renamed().isEmpty()) {
            out.append("\n### Renamed\n");
            for (Rename rename : diff.renamed()) {
                out.append("- `").append(rename.fromKey()).append("` -> `").append(rename.toKey()).append("` (")
                        .append(rename.aliased() ? "old key kept in previousKeys" : "old key not in previousKeys")
                        .append(")\n");
            }
        }
        if (!diff.changed().isEmpty()) {
            out.append("\n### Changed\n");
            for (Change change : diff.changed()) {
                out.append("- `").append(change.key()).append("`: ").append(String.join(", ", change.fields()));
                if (change.signatureChanged()) {
                    out.append(" (signature changed)");
                }
                out.append('\n');
            }
        }
    }

    private static void keyList(StringBuilder out, String heading, List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        out.append("\n### ").append(heading).append('\n');
        for (String key : keys) {
            out.append("- `").append(key).append("`\n");
        }
    }
}
