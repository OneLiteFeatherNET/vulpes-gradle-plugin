## MODIFIED Requirements

### Requirement: Consistency after the merge
The merged snapshot of a merged project, and the local snapshot of a local-only project, SHALL pass the consistency checks of a single snapshot: ids are unique across the snapshot; key or name is unique within each list; a name is unique across `expressions` and `expressionFunctions`; a previous key or name never equals a current one in the same list; asset paths are unique; advancement parents, which are keys, name an existing advancement, and the advancements form a forest without cycles. For a merged project these checks SHALL run only on the merged snapshot. A failing check SHALL fail the build. The message SHALL name the project, the conflicting entries and the side (local or published) each of them comes from; for a local-only project every entry comes from the local side.

A used resource location without an asset SHALL NOT fail the build. Used resource locations are the symbol `file` of a font, the name of a sound entry of type file and the `background` of an advancement. For every entry that uses a location without an asset, the plugin SHALL log one warning that names the project, the entry, the side (local or published) of the entry and the location, and the build SHALL continue. The warnings SHALL be logged once per build, also when `vulpesGenerate` is up to date.

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

#### Scenario: Missing asset after the merge
- **WHEN** a published sound `00000000-0000-0000-0000-0000000000b2` has a file entry `cygnus:ui/click` and neither the published nor the local snapshot holds an asset of that location
- **THEN** the build succeeds and one warning names `cygnus`, the sound `00000000-0000-0000-0000-0000000000b2`, the side `published` and the location `cygnus:ui/click`

#### Scenario: Snapshot without assets
- **WHEN** the local snapshot of a local-only project holds no assets, two fonts whose symbol `file` is `cygnus:font/ui.png`, and one sound with a file entry `cygnus:ui/click`
- **THEN** the build succeeds, the code is generated, and the output holds three warnings, one per font and one for the sound, each with the side `local` and its location

#### Scenario: Missing asset next to a failing check
- **WHEN** the local snapshot of a local-only project holds no asset for a used location and two items with the same key
- **THEN** the build fails because of the duplicate key

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
- **THEN** the build succeeds without a warning for that font, because the merged snapshot holds the asset, and the cross-entry checks run on the merged snapshot only

#### Scenario: Local-only project refers to a missing asset
- **WHEN** the local snapshot of a local-only project holds a font whose symbol `file` is `cygnus:font/ui.png` and no asset of that location exists in the local snapshot
- **THEN** the build succeeds and a warning names the font, the missing location and the side `local`
