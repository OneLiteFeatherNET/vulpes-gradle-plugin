# Proposal

**Conventional Commits:** `fix(plugin): warn instead of failing on missing local assets`

## Why

A content project that was imported into the dev backend without its assets cannot be built locally. The Cygnus snapshot from the dev backend holds no assets, but 21 fonts and 3 sounds refer to resource locations. The generator serves this snapshot through `/projects/{id}/local-build` as a valid local build. Plugin 0.4.1 fails the build in the consistency checks with `... uses resource location '...' that has no asset entry`, as `content-local-override` requires ("every used resource location has an asset").

Code generation does not read the assets. A missing asset only matters for the resource pack, which is not built by this plugin. Failing the build blocks local testing of content whose assets are not uploaded yet, without protecting anything the plugin produces.

## What Changes

- A used resource location without an asset no longer fails the build of a merged or local-only project. The plugin logs one warning per reference, which names the project, the entry, the side (local or published) of the entry and the resource location. The build continues and generates code.
- Used resource locations stay as today: the symbol `file` of a font, the name of a sound entry of type file, and the `background` of an advancement. The candidate asset paths stay as today.
- Every other consistency check keeps failing the build: unique ids, unique keys and names, previous keys, unique asset paths, advancement parents and cycles.
- The warnings are logged once per build with the local content notice, also when `vulpesGenerate` is up to date.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `content-local-override`: a missing asset after the merge, or in a local-only project, is a warning instead of a failure.

## Decisions

- **Warning per reference, not per location.** Each entry that uses a missing location gets its own line, so the author sees every entry to fix. The Cygnus case gives 24 lines, which is acceptable.
- **No switch.** A property that turns the warning back into a failure is not added. Nobody needs the failure today, and `vulpes.local.forbid=true` already keeps local content out of CI builds.
- **Checks stay without Gradle types.** The merge reports the missing assets as data; the Gradle task logs them.

## Non-goals

- No change to the other consistency checks or their messages.
- No check of published-only projects; they are not checked by the plugin today.
- No change to the format, to the generator or to the asset path candidates.
- No resource pack handling.

## Risks

- A missing asset reaches a later step unnoticed. Mitigation: the warning names the entry and the location on every build, and publishing in the generator is unaffected by this change.

## Impact

- `vulpes-gradle-plugin`: `ContentMerge` reports missing assets instead of throwing, `DataJarReader`/`LocalContent` pass them on, `VulpesLocalContentTask` logs them; unit and functional tests.
- No change to `vulpes-format`, the codegen library or the template runtime.
- Released as a patch release (0.4.2) through release-please.
