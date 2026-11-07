# Proposal

**Conventional Commits:** `feat(diff): compare two vulpes content releases`

## Why

When Renovate raises the version of a content dependency, the reviewer of the game needs to see what the new release changes for the code: which keys are added, removed or renamed and what that means for the build. Stelaris shows a diff as well, but the build is where removed keys break compilation, so the same answer has to be available locally with `./gradlew vulpesDiff` (design document, section "6. Gradle-Plugin").

## What Changes

- Task `vulpesDiff` compares the resolved data JAR of each content project with another version of the same artifact, given as `-Pvulpes.diff.from=<version>` (optional `-Pvulpes.diff.project=<name>` when several content projects are declared).
- Entities are matched by their stable `id`. Per entity type the report lists added, removed and renamed keys (same `id`, other key), and entities whose values changed. Pack changes (added, removed, other SHA-1) and expression function signature changes are listed as well.
- The report states the version bump the changes require (removed key: major; added key or rename with alias in `previousKeys`: minor; only values or packs: patch), so a reviewer can check the version chosen in the release.
- Output on the console and as files under `build/reports/vulpes/diff/` (Markdown for humans, JSON for tooling).

## Capabilities

### New Capabilities
- `content-diff`: Comparing two releases of one content project by key, id and value.

### Modified Capabilities
<!-- None. -->

## Non-goals

- No comparison with the live backend, only between published data JARs.
- No automatic version choice; the backend proposes the version, the diff only reports the required bump.
- No diff of asset bytes beyond the SHA-1 listed in the snapshot.

## Impact

- Depends on `add-plugin-skeleton` (`vulpes-format` reader and the `vulpes` dependency scope).
- Needs the snapshot fields `id` and `previousKeys` from contract format 1.
- No new dependencies.
