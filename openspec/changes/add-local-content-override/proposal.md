# Proposal

**Conventional Commits:** `feat(plugin): merge local content over published data jars`

## Why

Stelaris offers a "local build" download: a ZIP with `vulpes/<project>/release.json` and `vulpes/<project>/snapshot.json`. A developer unzips it into the game's project root to test content before it is published. Today the plugin only reads data JARs resolved as Maven modules, so the local files cannot be used, and a `release.json` with a `base` record is rejected by the strict format-1 reader.

Developers usually want to try a few changed or new entries together with the content that is already published, for example one new item next to the published items of the same project. Replacing the whole project would force them to copy the complete published content into the local directory, and any entry they forgot would silently disappear from the build. Merging entry by entry avoids both.

## What Changes

- If `<projectDir>/vulpes/<project>/` holds `release.json` and `snapshot.json`, the plugin merges them over the data JAR of the same declared `vulpes(...)` project. The parent directory is configurable with `vulpes { localContentDirectory = "..." }` (default `vulpes`).
- The merge is entry by entry. In items, attributes, fonts, sounds, advancements, expressions and expressionFunctions an entry is matched by its stable `id`; assets are matched by `path`. A local entry whose id (path) exists in the published snapshot replaces that entry completely, also when the local entry renames it, because the id stays the same. A local entry with a new id (path) is added. Published entries without a local counterpart are kept. Entries are not merged field by field.
- Deleting is not possible. An entry that is missing from the local snapshot stays in the build.
- `projectId` and `namespace` of the local snapshot must equal the published ones, otherwise the build fails and names both values. `pack` comes from the local snapshot. The local `release.json` (version, revision, base) describes the merged result; the published version is the version that the local content is merged over.
- The local files pass the format checks of a data JAR: readable, supported format, `release.project` equals the directory name, `revision` matches the local `snapshot.json`, and the local version is `X.Y.Z` or `X.Y.Z-SNAPSHOT`. A local build carries `0.0.0-SNAPSHOT`.
- The local files alone get no cross-entry checks. A local snapshot holds only the entries under test, so it may refer to published entries. All cross-entry checks (uniqueness, previous keys and names, asset references, advancement parents, a name clash between expressions and expression functions) run once, on the merged snapshot, with the same rules as a single snapshot. A failure names the project and the conflicting entries and states which side (local or published) each of them comes from.
- A directory for a project that is not declared is ignored with a warning. It does not add content, and it does not make `vulpes.local.forbid` fail.
- A directory that holds only one of the two files fails the build.
- Each build that uses local content logs one lifecycle line per project with the project, the local version, the published version it is merged over, the number of replaced and added entries (over all lists), and the recorded `base` branch and commit.
- `-Pvulpes.local.forbid=true` fails the build when a declared project is merged with a local directory, for CI.
- Local files are task inputs: changing them re-runs generation. Removing the directory returns the project to the published content only; the configuration cache keeps working.
- `vulpesDiff` compares the merged content on the current side.
- `release.json` format 2 is format 1 plus an optional `base` record (`repository`, `branch`, `commit`). The reader accepts format 1 and format 2. `base` is printed, never compared with the pinned vulpes-base version.
- The README gets a section "Testing content locally".

## Capabilities

### New Capabilities
- `content-local-override`: Merging local entries over the published data JAR of a declared content project, including the entry matching, the consistency checks after the merge, the notice, the forbid switch and the task inputs.

### Modified Capabilities
- `content-format`: `release.json` format 2 adds the optional `base` record to format 1; the reader accepts both formats.

## Non-goals

- No hot reload while a build runs; a changed local file is picked up by the next build.
- No comparison of the recorded `base` with the plugin's pinned vulpes-base version.
- No download of the local ZIP by the plugin.
- No deletion of published entries. A local snapshot cannot remove an entry; a tombstone mechanism would need a format change and is not part of this change.
- No field-by-field merge of an entry. A local entry replaces the published entry with the same id as a whole.
- No skipping of the Maven download for a merged project. The merge needs the published snapshot (see design.md).

## Impact

- `vulpes-gradle-plugin`: new resolution helper `LocalContent` and merge helper `ContentMerge` (both without Gradle types), shared by `vulpesGenerate`, `vulpesDiff` and a notice task; extension property `localContentDirectory`; property `vulpes.local.forbid`; README; functional and unit tests.
- `vulpes-format`: new format-2 schema `release-2.schema.json` (format 1 plus optional `base`), the `Release` record with a nullable `Base`, and a reader that accepts formats 1 and 2.
- Stelaris and the generator: write `release.json` format 2 with `base` only after the plugin release that contains this change (ordering constraint, see design.md). Until then they keep writing format 1 without `base`.
- Depends on the `content-format` and `content-generation-task` requirements of the in-flight proposals `add-plugin-skeleton` and `allow-snapshot-versions`, and on the `expressions` list of `add-content-expressions`.
