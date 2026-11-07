# Spec Delta

## Purpose

Lets a developer test content that is not published yet: local entries from a directory with `release.json` and `snapshot.json` are merged entry by entry over the published data JAR of the same declared content project for the build.

## ADDED Requirements

### Requirement: Local content directory
For every declared content project, the plugin SHALL use the directory `<projectDir>/<localContentDirectory>/<project>/` as local content when that directory contains both `release.json` and `snapshot.json`. `localContentDirectory` SHALL default to `vulpes` and SHALL be set with `vulpes { localContentDirectory = "..." }`, relative to the project directory.

#### Scenario: Local entries are merged over the data JAR
- **WHEN** `vulpes/cygnus/` holds valid `release.json` and `snapshot.json` and `cygnus` is declared as `vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")`
- **THEN** `vulpesGenerate` generates the code from the merged snapshot of the local files and the published snapshot of the data JAR of `cygnus`

#### Scenario: Custom directory
- **WHEN** `vulpes { localContentDirectory = "content" }` is set and `content/cygnus/` holds both files
- **THEN** the local files of `content/cygnus/` are merged over the data JAR of `cygnus`

#### Scenario: Partial directory
- **WHEN** `vulpes/cygnus/` holds `release.json` but not `snapshot.json`
- **THEN** the build fails and names the missing file and the directory, and does not fall back to the data JAR

### Requirement: Merge by stable id
The plugin SHALL merge the local snapshot over the published snapshot of the same declared project. In the lists items, attributes, fonts, sounds, advancements and expressionFunctions an entry SHALL be matched by its stable `id`; assets SHALL be matched by `path`. A local entry whose id (path) exists in the published snapshot SHALL replace that entry completely. A local entry whose id (path) does not exist in the published snapshot SHALL be added. A published entry without a local counterpart SHALL be kept. Entries SHALL NOT be merged field by field.

#### Scenario: Local entry replaces a published entry
- **WHEN** the published snapshot holds item id `7` and the local snapshot holds item id `7` with a different `name`
- **THEN** the merged snapshot holds the local item `7` and no field of the published item `7` remains

#### Scenario: Rename through a stable id
- **WHEN** the published snapshot holds item id `7` with key `cygnus:old_ingot` and the local snapshot holds item id `7` with key `cygnus:new_ingot`
- **THEN** the merged snapshot holds one item with id `7` and key `cygnus:new_ingot`, and no item with key `cygnus:old_ingot`

#### Scenario: New entry is added
- **WHEN** the local snapshot holds item id `42` that the published snapshot does not hold
- **THEN** the merged snapshot holds the item `42` next to all published items

#### Scenario: Published entry without local counterpart is kept
- **WHEN** the published snapshot holds item id `3` and the local snapshot does not hold id `3`
- **THEN** the merged snapshot holds the published item `3` unchanged

#### Scenario: Assets are matched by path
- **WHEN** the local snapshot holds an asset with the path of a published asset and an asset with a new path
- **THEN** the merged snapshot holds the local asset for the shared path and both assets for the other paths

### Requirement: Top-level fields of the merge
`projectId` and `namespace` of the local snapshot SHALL equal the `projectId` and `namespace` of the published snapshot. A mismatch SHALL fail the build with a message that names both values. `pack` SHALL be taken from the local snapshot. The version, revision and base of the merged result SHALL be taken from the local `release.json`, and the notice SHALL name the published version that the local content is merged over.

#### Scenario: Namespace mismatch
- **WHEN** the local snapshot of `cygnus` has namespace `manis` and the published snapshot has namespace `cygnus`
- **THEN** the build fails and names both namespaces

#### Scenario: Project mismatch
- **WHEN** the local snapshot of `cygnus` has `projectId` `manis` and the published snapshot has `projectId` `cygnus`
- **THEN** the build fails and names both project ids

### Requirement: No deletion through local content
An entry that is missing from the local snapshot SHALL stay in the merged result. The plugin SHALL NOT treat a missing entry as deleted.

#### Scenario: Missing entry stays
- **WHEN** the published snapshot holds item id `3` and the local snapshot is valid and does not contain `3`
- **THEN** the merged snapshot contains item `3` and the build succeeds

### Requirement: Consistency after the merge
The merged snapshot SHALL pass the same consistency checks as a single snapshot: ids are unique across the snapshot; key or name is unique within each list; a previous key or name never equals a current one in the same list; asset paths are unique; every used resource location has an asset; advancement parents form a forest without cycles and point to existing advancements. A failing check SHALL fail the build. The message SHALL name the project, the conflicting entries and the side (local or published) each of them comes from.

#### Scenario: Key conflict after the merge
- **WHEN** the local snapshot adds item id `X` with key `cygnus:iron_sword` and the published snapshot holds item id `Y` with key `cygnus:iron_sword`
- **THEN** the build fails and names `X` and `Y`, the key `cygnus:iron_sword`, `X` as local and `Y` as published

#### Scenario: Dangling advancement parent after the merge
- **WHEN** the local snapshot replaces an advancement that a published advancement uses as parent, and the replacement has a different id
- **THEN** the build fails and names the published advancement, the missing parent id and the side of each entry

### Requirement: Undeclared local directories
A directory in the local content directory for a project that is not declared as a `vulpes(...)` dependency SHALL NOT add content. The plugin SHALL log a warning that names the directory and states that it is ignored.

#### Scenario: Undeclared directory
- **WHEN** `vulpes/manis/` holds valid files and `manis` is not declared
- **THEN** no code is generated for `manis`, the build succeeds, and the warning names `vulpes/manis`

### Requirement: Local override notice
Every build that merges local content into a declared content project SHALL log one lifecycle line per project that names the project, the local version, the published version it is merged over, the number of replaced entries and added entries over all lists, and the recorded `base` branch and commit, or states that no base is recorded. The notice SHALL be printed even when `vulpesGenerate` is up to date.

#### Scenario: Notice names the merge
- **WHEN** `cygnus` is merged from `vulpes/cygnus/` with version `1.4.1-SNAPSHOT` over published version `1.4.0`, three entries are replaced, two entries are added and the recorded base branch is `main`
- **THEN** the lifecycle output names `cygnus`, `1.4.1-SNAPSHOT`, `1.4.0`, `3` replaced entries, `2` added entries and the base branch `main`

#### Scenario: Up-to-date generation
- **WHEN** `vulpesGenerate` is up to date and the local directory is unchanged
- **THEN** the lifecycle output still contains the notice

### Requirement: Local content validation
Local files SHALL pass the same checks as a data JAR: `release.json` and `snapshot.json` are read with the format reader, the format is supported (format 1 or format 2), the `revision` matches `snapshot.json`, and `release.project` equals the directory name. The local version SHALL be in the Maven form `X.Y.Z` or `X.Y.Z-SNAPSHOT`, as `allow-snapshot-versions` requires: a local build carries `0.0.0-SNAPSHOT`, a downloaded published version keeps its `X.Y.Z`. A failing check SHALL fail the build with a message that names the directory and the check.

#### Scenario: Project does not match directory
- **WHEN** `vulpes/cygnus/release.json` contains `"project": "manis"`
- **THEN** the build fails and names `vulpes/cygnus` and both values

#### Scenario: Unsupported format
- **WHEN** `vulpes/cygnus/release.json` contains `"format": 3`
- **THEN** the build fails and names the supported formats 1 and 2

#### Scenario: Format 2 accepted
- **WHEN** `vulpes/cygnus/release.json` contains `"format": 2` with a valid `base`
- **THEN** the local files are merged and the notice prints the base

### Requirement: Forbid local content
The property `vulpes.local.forbid` SHALL default to `false`. When it is `true` and a declared content project is merged with a local directory, the build SHALL fail before code is generated and the message SHALL name each such project. Undeclared directories SHALL NOT cause a failure.

#### Scenario: CI forbids local content
- **WHEN** the build runs with `-Pvulpes.local.forbid=true` and `vulpes/cygnus/` is used
- **THEN** the build fails and names `cygnus`

#### Scenario: Forbid without local content
- **WHEN** the build runs with `-Pvulpes.local.forbid=true` and no declared project has a local directory
- **THEN** the build succeeds

#### Scenario: Forbid with undeclared directory
- **WHEN** the build runs with `-Pvulpes.local.forbid=true` and `vulpes/manis/` holds valid files but `manis` is not declared
- **THEN** the build succeeds and the warning names `vulpes/manis`

### Requirement: Local files as task inputs
The local files SHALL be inputs of the generation task and of `vulpesDiff`. Adding, changing or removing them SHALL change the inputs, so that the task re-runs. Removing a local directory SHALL return the project to the published content only. The task SHALL remain cacheable and compatible with the configuration cache.

#### Scenario: Changed local file
- **WHEN** `vulpesGenerate` runs twice and `vulpes/cygnus/snapshot.json` changed between the runs
- **THEN** the second run is not up to date and regenerates the code

#### Scenario: Removed local directory
- **WHEN** `vulpes/cygnus/` is deleted after a run with the local files
- **THEN** the next run generates from the published snapshot of `cygnus` only

#### Scenario: Configuration cache
- **WHEN** a build with a local directory runs twice with `--configuration-cache`
- **THEN** the second run reuses the configuration cache

### Requirement: Base information
When `release.json` in format 2 carries `base`, the plugin SHALL print its `repository`, `branch` and `commit` in the local override notice. The plugin SHALL NOT compare `base` with the vulpes-base version it pins and SHALL NOT fail or warn because of it.

#### Scenario: Base is printed
- **WHEN** local `release.json` records base branch `main` at commit `0123abc…` (full 40-character SHA)
- **THEN** the notice contains the branch and the commit

#### Scenario: Base differs from pinned version
- **WHEN** the recorded base branch does not correspond to the pinned vulpes-base version
- **THEN** the build succeeds without a warning about the base
