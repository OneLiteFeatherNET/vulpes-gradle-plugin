## MODIFIED Requirements

### Requirement: Local content directory
For every declared content project, the plugin SHALL use the directory `<projectDir>/<localContentDirectory>/<project>/` as local content when that directory contains both `release.json` and `snapshot.json`. The local content SHALL be merged over the published data JAR of that project when the data JAR resolves, and SHALL be used alone as a local-only project when it does not resolve. `localContentDirectory` SHALL default to `vulpes` and SHALL be set with `vulpes { localContentDirectory = "..." }`, relative to the project directory.

#### Scenario: Local entries are merged over the data JAR
- **WHEN** `vulpes/cygnus/` holds valid `release.json` and `snapshot.json` and `cygnus` is declared as `vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")` and that data JAR resolves
- **THEN** `vulpesGenerate` generates the code from the merged snapshot of the local files and the published snapshot of the data JAR of `cygnus`

#### Scenario: Custom directory
- **WHEN** `vulpes { localContentDirectory = "content" }` is set and `content/cygnus/` holds both files
- **THEN** the local files of `content/cygnus/` are merged over the data JAR of `cygnus`

#### Scenario: Partial directory
- **WHEN** `vulpes/cygnus/` holds `release.json` but not `snapshot.json`
- **THEN** the build fails and names the missing file and the directory, and does not fall back to the data JAR or to a local-only project

### Requirement: Local content validation
The local files SHALL pass the format checks: `release.json` and `snapshot.json` are read with the format reader, the format is supported (format 1 or format 2), the `revision` matches `snapshot.json`, and `release.project` equals the directory name. The local version SHALL be in the Maven form `X.Y.Z` or `X.Y.Z-SNAPSHOT`, as `allow-snapshot-versions` requires: a local build carries `0.0.0-SNAPSHOT`, a downloaded published version keeps its `X.Y.Z`. For a merged project the local files alone SHALL NOT be checked for cross-entry consistency, because a local snapshot holds only the entries under test and may refer to published entries. For a local-only project the local files SHALL be checked for cross-entry consistency as a single snapshot. A failing format check SHALL fail the build with a message that names the directory and the check.

#### Scenario: Project does not match directory
- **WHEN** `vulpes/cygnus/release.json` contains `"project": "manis"`
- **THEN** the build fails and names `vulpes/cygnus` and both values

#### Scenario: Unsupported format
- **WHEN** `vulpes/cygnus/release.json` contains `"format": 3`
- **THEN** the build fails and names the supported formats 1 and 2

#### Scenario: Format 2 accepted
- **WHEN** `vulpes/cygnus/release.json` contains `"format": 2` with a valid `base`
- **THEN** the local files are merged or used as local-only content and the notice prints the base

#### Scenario: Local snapshot refers to a published asset
- **WHEN** the local snapshot of a merged project holds a font whose symbol `file` is `cygnus:font/ui.png`, and the asset of that location exists only in the published snapshot
- **THEN** the build succeeds, because the merged snapshot holds the asset, and the cross-entry checks run on the merged snapshot only

#### Scenario: Local-only project refers to a missing asset
- **WHEN** the local snapshot of a local-only project holds a font whose symbol `file` is `cygnus:font/ui.png` and no asset of that location exists in the local snapshot
- **THEN** the build fails and names the font, the missing location and the side `local`

### Requirement: Merge by stable id
The plugin SHALL merge the local snapshot over the published snapshot of the same declared project. In the lists items, attributes, fonts, sounds, advancements, expressions and expressionFunctions an entry SHALL be matched by its stable `id`, which is a UUID; assets SHALL be matched by `path`. A local entry whose id (path) exists in the published snapshot SHALL replace that entry completely. A local entry whose id (path) does not exist in the published snapshot SHALL be added. A published entry without a local counterpart SHALL be kept. Entries SHALL NOT be merged field by field. This requirement applies to merged projects only; a local-only project has no published snapshot to match against.

#### Scenario: Local entry replaces a published entry
- **WHEN** the published snapshot holds item id `00000000-0000-0000-0000-000000000007` with material `IRON_INGOT` and the local snapshot holds the same id with material `GOLD_INGOT`
- **THEN** the merged snapshot holds the local item `00000000-0000-0000-0000-000000000007` with material `GOLD_INGOT` and no field of the published item remains

#### Scenario: Rename through a stable id
- **WHEN** the published snapshot holds item id `00000000-0000-0000-0000-000000000007` with key `cygnus:old_ingot` and the local snapshot holds the same id with key `cygnus:new_ingot`
- **THEN** the merged snapshot holds one item with id `00000000-0000-0000-0000-000000000007` and key `cygnus:new_ingot`, and no item with key `cygnus:old_ingot`

#### Scenario: New entry is added
- **WHEN** the local snapshot holds item id `00000000-0000-0000-0000-000000000042` that the published snapshot does not hold
- **THEN** the merged snapshot holds the item `00000000-0000-0000-0000-000000000042` next to all published items

#### Scenario: Published entry without local counterpart is kept
- **WHEN** the published snapshot holds item id `00000000-0000-0000-0000-000000000003` and the local snapshot does not hold that id
- **THEN** the merged snapshot holds the published item `00000000-0000-0000-0000-000000000003` unchanged

#### Scenario: Assets are matched by path
- **WHEN** the local snapshot holds an asset with the path of a published asset and an asset with a new path
- **THEN** the merged snapshot holds the local asset for the shared path and both assets for the other paths

### Requirement: Consistency after the merge
The merged snapshot of a merged project, and the local snapshot of a local-only project, SHALL pass the consistency checks of a single snapshot: ids are unique across the snapshot; key or name is unique within each list; a name is unique across `expressions` and `expressionFunctions`; a previous key or name never equals a current one in the same list; asset paths are unique; every used resource location has an asset; advancement parents, which are keys, name an existing advancement, and the advancements form a forest without cycles. For a merged project these checks SHALL run only on the merged snapshot. A failing check SHALL fail the build. The message SHALL name the project, the conflicting entries and the side (local or published) each of them comes from; for a local-only project every entry comes from the local side.

#### Scenario: Key conflict after the merge
- **WHEN** the local snapshot adds item id `00000000-0000-0000-0000-0000000000a1` with key `cygnus:iron_sword` and the published snapshot holds item id `00000000-0000-0000-0000-0000000000b2` with key `cygnus:iron_sword`
- **THEN** the build fails and names both ids, the key `cygnus:iron_sword`, `...00a1` as local and `...00b2` as published

#### Scenario: Name clash between expression and expression function after the merge
- **WHEN** the local snapshot adds expression `00000000-0000-0000-0000-0000000000a1` with name `cygnus:calc` and the published snapshot holds expression function `00000000-0000-0000-0000-0000000000b2` with name `cygnus:calc`
- **THEN** the build fails and names both ids, the name `cygnus:calc`, and the side of each entry

#### Scenario: Dangling advancement parent after the merge
- **WHEN** the local snapshot replaces advancement `00000000-0000-0000-0000-0000000000a1` with the new key `cygnus:new_root`, and a published advancement `00000000-0000-0000-0000-0000000000b2` names the old key `cygnus:old_root` as its parent
- **THEN** the build fails and names the published child `00000000-0000-0000-0000-0000000000b2`, the missing parent key `cygnus:old_root`, and the local replacement `00000000-0000-0000-0000-0000000000a1` with its key `cygnus:new_root`, each with its side

#### Scenario: Duplicate key in a local-only project
- **WHEN** the local snapshot of a local-only project holds two items with key `cygnus:iron_sword` and ids `00000000-0000-0000-0000-0000000000a1` and `00000000-0000-0000-0000-0000000000a2`
- **THEN** the build fails and names both ids, the key `cygnus:iron_sword` and the side `local` for each

### Requirement: Local override notice
Every build that uses local content SHALL log one lifecycle line per project that uses local content. For a merged project the line SHALL name the project, the local version, the published version it is merged over, the number of replaced entries and added entries over all lists, and the recorded `base` branch and commit, or state that no base is recorded. For a local-only project the line SHALL follow the rule of the local-only notice requirement. The notice SHALL be printed even when `vulpesGenerate` is up to date.

#### Scenario: Notice names the merge
- **WHEN** `cygnus` is merged from `vulpes/cygnus/` with version `1.4.1-SNAPSHOT` over published version `1.4.0`, three entries are replaced, two entries are added and the recorded base branch is `main`
- **THEN** the lifecycle output names `cygnus`, `1.4.1-SNAPSHOT`, `1.4.0`, `3` replaced entries, `2` added entries and the base branch `main`

#### Scenario: Up-to-date generation
- **WHEN** `vulpesGenerate` is up to date and the local directory is unchanged
- **THEN** the lifecycle output still contains the notice

### Requirement: Forbid local content
The property `vulpes.local.forbid` SHALL default to `false`. When it is `true` and a declared content project uses a local directory, whether merged or local-only, the build SHALL fail before code is generated and the message SHALL name each such project. For a local-only project the message SHALL also name the declared coordinate and the reason that the data JAR did not resolve. Undeclared directories SHALL NOT cause a failure.

#### Scenario: CI forbids local content
- **WHEN** the build runs with `-Pvulpes.local.forbid=true` and `vulpes/cygnus/` is used
- **THEN** the build fails and names `cygnus`

#### Scenario: CI forbids local-only content
- **WHEN** the build runs with `-Pvulpes.local.forbid=true`, `cygnus` is declared as `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT`, its data JAR does not resolve and `vulpes/cygnus/` is used
- **THEN** the build fails and names `cygnus`, the coordinate `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT` and the reason

#### Scenario: Forbid without local content
- **WHEN** the build runs with `-Pvulpes.local.forbid=true` and no declared project has a local directory
- **THEN** the build succeeds

#### Scenario: Forbid with undeclared directory
- **WHEN** the build runs with `-Pvulpes.local.forbid=true` and `vulpes/manis/` holds valid files but `manis` is not declared
- **THEN** the build succeeds and the warning names `vulpes/manis`

### Requirement: Local files as task inputs
The local files SHALL be inputs of the generation task and of `vulpesDiff`. The declared coordinates of the content projects SHALL be inputs of the same tasks. Adding, changing or removing local files SHALL change the inputs, so that the task re-runs. Removing a local directory of a merged project SHALL return the project to the published content only. Removing a local directory of a local-only project SHALL fail the build, because no published content provides the project, and the message SHALL name the declared coordinate and the reason. An artifact that becomes resolvable SHALL change the classpath and SHALL switch the project from local-only to merged. The task SHALL remain cacheable and compatible with the configuration cache.

#### Scenario: Changed local file
- **WHEN** `vulpesGenerate` runs twice and `vulpes/cygnus/snapshot.json` changed between the runs
- **THEN** the second run is not up to date and regenerates the code

#### Scenario: Removed local directory
- **WHEN** `vulpes/cygnus/` is deleted after a run with the local files of a merged project
- **THEN** the next run generates from the published snapshot of `cygnus` only

#### Scenario: Removed local directory of a local-only project
- **WHEN** `vulpes/cygnus/` is deleted after a run with the local files of a local-only project
- **THEN** the next run fails and names the declared coordinate of `cygnus` and the reason that the data JAR did not resolve

#### Scenario: Published release appears
- **WHEN** `cygnus` was built as local-only and its data JAR becomes resolvable, while `vulpes/cygnus/` still holds both files
- **THEN** the next run is not up to date, merges the local files over the published snapshot, and the notice names the published version

#### Scenario: Configuration cache
- **WHEN** a build with a local directory runs twice with `--configuration-cache`
- **THEN** the second run reuses the configuration cache

## ADDED Requirements

### Requirement: Local-only content projects
When a declared content project has a local directory with both files and its data JAR does not resolve, the plugin SHALL use the local files alone as the content of that project. The plugin SHALL NOT match them against a published snapshot. The project SHALL be generated like any other content project, with its local release constants. The fallback SHALL apply to every reason of a resolution failure, and the notice SHALL name the reason.

#### Scenario: Unpublished project used locally
- **WHEN** `cygnus` is declared as `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT`, no data JAR of that coordinate exists and `vulpes/cygnus/` holds valid `release.json` and `snapshot.json`
- **THEN** `vulpesGenerate` generates the code of `cygnus` from the local files alone, and the build succeeds

#### Scenario: Authentication failure with local directory
- **WHEN** the repository of `cygnus` answers with an authentication failure and `vulpes/cygnus/` holds both files
- **THEN** the build succeeds with the local files alone, and the notice names the authentication failure as the reason

#### Scenario: Resolvable project merges
- **WHEN** the data JAR of `cygnus` resolves and `vulpes/cygnus/` holds both files
- **THEN** the local files are merged over the published snapshot and no local-only line is printed for `cygnus`

### Requirement: Unresolvable project without local content
A declared content project whose data JAR does not resolve and that has no local directory with both files SHALL fail the build. The message SHALL name the declared coordinate and the reason of the resolution failure. The plugin SHALL NOT skip such a project silently.

#### Scenario: Unresolvable project without local directory
- **WHEN** `cygnus` is declared as `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT`, no data JAR of that coordinate exists and no `vulpes/cygnus/` directory with both files exists
- **THEN** the build fails and names `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT` and the reason `not found`

### Requirement: Local-only notice
For a local-only project the local override notice SHALL be one lifecycle line that names the project, the local version, that no published release is used, the declared coordinate, the reason that the data JAR did not resolve, and the recorded `base` branch and commit or the statement that no base is recorded.

#### Scenario: Local-only line
- **WHEN** `cygnus` is local-only with local version `0.1.0-SNAPSHOT`, declared as `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT`, the resolution failure is `not found` and the recorded base branch is `main`
- **THEN** the lifecycle output names `cygnus`, `0.1.0-SNAPSHOT`, `no published release used`, the coordinate `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT`, `not found` and the base branch `main`

### Requirement: Local content in vulpesDiff
The current side of `vulpesDiff` SHALL use the same content as generation: the merged content of a merged project and the local content of a local-only project. The base side SHALL NOT change; it SHALL use the Maven artifacts of `-Pvulpes.diff.from`, as before.

#### Scenario: Diff with a local-only project
- **WHEN** `cygnus` is local-only and `vulpesDiff` runs with `-Pvulpes.diff.from=1.4.0`
- **THEN** the current side of `cygnus` holds the local content, and the base side holds the published `1.4.0` of `cygnus`

#### Scenario: Missing base release
- **WHEN** `vulpesDiff` runs with `-Pvulpes.diff.from=9.9.9` and that version of a declared project does not exist
- **THEN** the build fails as before, because the base side is not a local override

### Requirement: Local content without a publish is never hidden
A build that uses a local-only project SHALL NOT pass without a visible statement of that fact. The notice of every such build SHALL name the project and the reason, and `-Pvulpes.local.forbid=true` SHALL fail the build.

#### Scenario: Typo in the declared version
- **WHEN** `cygnus` is declared with version `0.1.0-SNAPSHOT1` and `vulpes/cygnus/` holds both files
- **THEN** the build uses the local files alone, the notice names the coordinate `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT1` and the reason, and with `-Pvulpes.local.forbid=true` the build fails
