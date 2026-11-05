# Proposal

**Conventional Commits:** `feat(plugin): use local content directories instead of published data jars`

## Why

Stelaris offers a "local build" download: a ZIP with `vulpes/<project>/release.json` and `vulpes/<project>/snapshot.json`. A developer unzips it into the game's project root to test content before it is published. Today the plugin only reads data JARs resolved as Maven modules, so the local files cannot be used, and a `release.json` with a `base` record is rejected by the strict format-1 reader.

## What Changes

- If `<projectDir>/vulpes/<project>/` holds `release.json` and `snapshot.json`, the plugin uses that directory instead of the data JAR of the same declared `vulpes(...)` project. The parent directory is configurable with `vulpes { localContentDirectory = "..." }` (default `vulpes`).
- The local files pass the same validation as a data JAR: readable, supported format, `release.project` equals the directory name, `revision` matches `snapshot.json`.
- A directory for a project that is not declared is ignored with a warning. It does not add content.
- Each build that uses local content logs one lifecycle line per project with its version, the Maven version it replaces, and the recorded `base` branch and commit.
- `-Pvulpes.local.forbid=true` fails the build when a declared project is served from a local directory, for CI.
- Local files are task inputs: removing the directory switches back to the Maven artifact and re-runs generation; the configuration cache keeps working.
- `vulpesDiff` compares the local content on the current side.
- `release.json` may carry an optional `base` record (`repository`, `branch`, `commit`) in format 1. It is printed, never compared with the pinned vulpes-base version.
- The README gets a section "Testing content locally".

## Capabilities

### New Capabilities
- `content-local-override`: Serving a declared content project from a local directory instead of its data JAR, including the notice, the forbid switch and the task inputs.

### Modified Capabilities
- `content-format`: `release.json` may carry the optional `base` record. The change is additive; see design.md for why the format number stays 1.

## Non-goals

- No hot reload while a build runs; a changed local file is picked up by the next build.
- No comparison of the recorded `base` with the plugin's pinned vulpes-base version.
- No download of the local ZIP by the plugin.
- Declared projects still resolve their Maven artifact (see design.md, open questions).

## Impact

- `vulpes-gradle-plugin`: new resolution helper shared by `vulpesGenerate`, `vulpesDiff` and a notice task; extension property `localContentDirectory`; property `vulpes.local.forbid`; README; functional and unit tests.
- `vulpes-format`: optional `base` in `release-1.schema.json`, the `Release` record and the reader.
- Stelaris local build: produces `release.json` with `base` in the layout above.
- Depends on the `content-format` and `content-generation-task` requirements of the in-flight proposals `add-plugin-skeleton` and `allow-snapshot-versions`.
