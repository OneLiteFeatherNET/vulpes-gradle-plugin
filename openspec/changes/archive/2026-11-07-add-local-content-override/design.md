# Design

## Context

Every content project reaches the build as a data JAR in the `vulpes` scope, resolved by `vulpesData` and read by `DataJarReader` through `ContentReader`. `VulpesGenerateTask` and `VulpesDiffTask` take those JARs as `@Classpath` inputs and map file names to artifact ids. Stelaris will produce a local build ZIP with the same two files as the JAR, in `vulpes/<project>/`, plus `base` in `release.json`, written as format 2. The local snapshot holds the entries a developer wants to test; it is not a complete copy of the published project. The reader of format 1 rejects unknown fields (`additionalProperties: false` in the schema, strict parsing), so a `base` record is rejected today.

## Decisions

### Merge per declared project
A declared project with a local directory `<projectDir>/<localContentDirectory>/<project>/` that holds both files is merged: the published snapshot of its data JAR and the local snapshot are read, and one merged snapshot is built. Generation and the current side of `vulpesDiff` use the merged snapshot. The project set is the set of `vulpes(...)` dependency names. The directory is detected at execution time by one helper (`LocalContent`) that takes the directory, the artifact ids by file name and the forbid flag. A second helper (`ContentMerge`) takes two in-memory snapshots and returns the merged snapshot and the counts for the notice. Neither helper has Gradle types, so unit tests run in milliseconds with `@TempDir` or in-memory snapshots.

Alternative considered: decide the selection at configuration time with `File.exists`. Rejected: the configuration cache would not notice a directory that appears or disappears, and the task inputs would have to be recomputed anyway.

### Merge by stable id
In the lists items, attributes, fonts, sounds, advancements, expressions and expressionFunctions an entry is matched by its stable `id`, which is a UUID. Assets are matched by `path`. The rules are the same for every list:

- A local entry whose id exists in the published snapshot replaces that entry completely. The published entry is dropped, not merged field by field.
- A local entry with an id that does not exist in the published snapshot is added.
- A published entry without a local counterpart is kept.

Because the id stays stable, a rename is a replacement: a local entry with the same id and a new key replaces the published entry, and the old key disappears from the merged snapshot. Matching by key would make a rename look like a delete and an add, and the published entry would stay next to the new one.

Alternative considered: replace per category, so that a local `items` list replaces all published items. Rejected: too coarse for experiments with single entries, because one changed item would drop all other published items of the project.

Alternative considered: replace the whole project, which was the previous plan of this change. Rejected: the team wants to test single entries together with the published content, and a full replacement forces a copy of all published content into the local ZIP.

### Top-level fields
`projectId` and `namespace` of the local snapshot SHALL equal the published ones. A mismatch fails the build and names both values. `pack` comes from the local snapshot. The local `release.json` (version, revision and base) describes the merged result, because the merged content is what the developer tests. The published version is the version the local content is merged over; the notice names it.

The revision check of the local files stays as it is: the local `release.json` revision must match the local `snapshot.json`.

### No deletion
An entry that is missing from the local snapshot stays in the merged result. The local snapshot cannot tell "deleted" from "not part of the experiment", so a missing entry is never treated as a deletion. Supporting deletion would need a tombstone record in the format, which is a format change and is not part of this change.

### Consistency after the merge
The local files alone get only the format checks: the format reader, the supported format number, the `revision` against `snapshot.json`, the `release.project` against the directory name, and the version form. They get no cross-entry checks. A local snapshot holds only the entries under test, so it may legitimately refer to published entries: an advancement `parent` key, or a font symbol `file` whose asset exists only in the published snapshot.

All cross-entry checks run once, on the merged snapshot, with the same rules as a single snapshot:

- ids are unique across the snapshot;
- key or name is unique within each list;
- a name is unique across `expressions` and `expressionFunctions`;
- a previous key or name never equals a current one in the same list;
- asset paths are unique;
- every used resource location has an asset;
- advancement parents, which are keys, name an existing advancement, and the advancements form a forest without cycles.

A failing check fails the build. The message names the project, the conflicting entries, and the side each entry comes from (local or published). Example: the local snapshot adds item id `00000000-0000-0000-0000-0000000000a1` with key `cygnus:iron_sword`, while the published item `00000000-0000-0000-0000-0000000000b2` keeps `cygnus:iron_sword`. The build fails and names both ids, the shared key, and the side of each.

A local snapshot that is valid on its own can still fail after the merge, and that is the case the merged checks are for. The reverse also holds: a reference from a local entry to a published entry is valid, because the merged snapshot holds that entry.

### Task inputs
`vulpesGenerate` and `vulpesDiff` receive the whole local content directory tree as `@InputFiles` with `@PathSensitive(RELATIVE)`, filtered to the two file names. An added, changed or deleted file therefore changes the inputs. The data JARs stay `@Classpath`, because the published snapshot is still read from them. No existence check happens during configuration, so the configuration cache stays valid when directories come and go.

### Undeclared directories are ignored with a warning
A directory without a declared dependency is ignored with a warning that names it. It never adds content and never fails the build, not even with the forbid switch.
Reasons: the declared `vulpes(...)` lines are the reviewed source of truth for which content a game builds; `vulpes.project("<name>")` already refuses undeclared names; and a leftover directory must not add generated classes silently. Declaring the dependency is required anyway, because the merge needs its published snapshot.
Alternative considered: use undeclared directories as content. Rejected.

### Partial directories fail
A directory with one of the two files fails the build. Falling back to the published content alone would hide a half-unzipped ZIP, which is exactly the case the developer is trying to test.

### Notice on every build
A separate `@UntrackedTask` named `vulpesLocalContent` validates the local files, prints the notice and fails on the forbid switch. `vulpesGenerate` depends on it. Printing from `vulpesGenerate` itself would be skipped when the task is up to date, and a local override must never be silent. The task reads only two small files per declared project, so running on every build is cheap.

The notice is one lifecycle line per merged project. It names the project, the local version, the published version it is merged over, the number of replaced entries and added entries (counted over all lists), and the recorded base branch and commit, or states that no base is recorded.

### Forbid switch
`-Pvulpes.local.forbid=true` is read as a Gradle property and checked by `vulpesLocalContent` before generation. It fails only for merged projects; undeclared directories only warn, as decided above.

### Format 2 adds the base record
Format 2 is format 1 plus the optional `base` object `Base(repository, branch, commit)`. The reader accepts format 1 and format 2. A format-1 file that contains `base` is rejected as an unknown field, because format 1 stays strict. Any other format number is rejected with the supported formats named.

Ordering constraint: the generator writes `release.json` in format 2 only after a plugin release that contains this change is out. Until then it keeps writing format 1 without `base`. This applies to the local build ZIP as well, because an older plugin cannot read format 2.

This supersedes the earlier plan to add `base` to format 1 without a bump. Format 1 was never delivered, but a format-2 number makes the rule for readers explicit: format 2 is never read by a plugin older than this change.

### Commit form
`base.commit` is a full 40-character lowercase SHA-1. The generator emits the full SHA, so the strict pattern stays. A short commit is rejected.

### Snapshot version form
A local version is in the Maven form `X.Y.Z` or `X.Y.Z-SNAPSHOT`, consistent with `allow-snapshot-versions`: the generator writes `0.0.0-SNAPSHOT` into a local build, and a downloaded published version keeps its `X.Y.Z`. The local check uses the same version rule as the format; the notice prints the version as read.

### Base information
`base` is printed in the notice and is not compared with `libs.versions.vulpes.base`. The plugin pins a version, but `release.json` records a branch and a commit. A branch cannot be mapped to a version, and a commit can only be mapped to a version with a Git lookup, which needs the network and is not reproducible. A mismatch can therefore not be decided reliably, so the plugin prints the recorded values and leaves the judgement to the developer.

### vulpesDiff
The current side of `vulpesDiff` uses the same merged content, so a diff compares what the build actually uses. The base side always uses the Maven artifacts of `-Pvulpes.diff.from`.

### Resolution of the Maven artifact
A merged project still resolves its Maven artifact, because the merge needs the published snapshot and `vulpesData` is one configuration. A developer therefore still needs the repository credentials. Skipping the download for a merged project would need per-project resolution and is not part of this change.

## Risks

- A forgotten local directory silently changes which content a release build uses. Mitigation: the notice on every build, and `-Pvulpes.local.forbid=true` in CI.
- Keys that appear inside free-form values (item `components`, expression text) are not checked by this change. A local entry can replace a published entry that such a value still refers to by key, and the build does not notice.
- The generator writes format 2 before a plugin that reads it is released, and older plugins fail on every build. Mitigation: the ordering constraint above, stated in the release notes of this change.

## Open questions

None. The question whether a merged project can skip its Maven download is answered above: it cannot, because the merge needs the published snapshot.
