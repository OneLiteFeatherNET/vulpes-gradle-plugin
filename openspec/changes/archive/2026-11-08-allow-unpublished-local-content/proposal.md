# Proposal

**Conventional Commits:** `feat(plugin): use local content when no published release exists`

## Why

A new game or a new content project has no published data JAR yet. Today the plugin merges local content over the published data JAR of a declared project, and the declared `vulpes("group:<project>:<version>")` dependency must resolve. The project set and the Maven versions are even derived from the resolved artifacts. A developer who wants to try a brand-new content project locally therefore has to wait until someone publishes a first snapshot from Stelaris, although the local directory already holds everything the build needs.

The team wants local testing without any publish. A local directory that holds `release.json` and `snapshot.json` should be usable on its own when no published release exists for its declared project, and the fallback must stay visible, so that a missing publish is never hidden in CI.

## What Changes

- The declared `vulpes(...)` dependencies stay the only source of which content projects a game uses. The project set and the declared versions come from the declared dependencies of the `vulpes` configuration (module name and version), not from the resolved artifacts.
- The data JARs are resolved leniently. For each declared content project the plugin then picks one of four cases:
  - published data JAR resolved and a local directory with both files: merge, as today;
  - published data JAR resolved and no local directory: published content only, as today;
  - published data JAR not resolvable and a local directory with both files: the local content is used alone, as a local-only project;
  - published data JAR not resolvable and no local directory: the build fails and names the dependency and the reason of the resolution failure. Nothing is skipped silently.
- A local-only project gets all cross-entry checks of a single snapshot, because there is nothing to merge with. The checks and their messages are the ones of the merged snapshot; a failure names the local files as the side of each entry.
- Each local-only project logs one lifecycle line that names the project, the local version, that no published release is used, the declared coordinate and the short reason of the resolution failure, for example `could not resolve net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT: not found`. The recorded `base` is printed as for merged projects. Merged projects keep their line.
- `-Pvulpes.local.forbid=true` also fails for local-only projects, and names them. A missing publish never passes a CI build silently.
- `vulpesDiff` uses the local-only content on its current side. Its base side is unchanged and still comes from Maven with `-Pvulpes.diff.from`.
- The declared coordinates are inputs of the generation and diff tasks. When the artifact becomes resolvable later, the classpath changes and the project switches to the merge.
- Directories for undeclared projects are still ignored with a warning. The `vulpes(...)` line is still required, with the version the project will be published under, for example `0.1.0-SNAPSHOT`.
- The README section "Testing content locally" describes local-only projects.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `content-local-override`: local directories are used alone when no published release exists for a declared project; the notice, the forbid switch, the consistency checks, the task inputs and `vulpesDiff` cover local-only projects.
- `content-generation-task`: the content projects come from the declared dependencies, and the data JARs are resolved leniently; an unresolvable declared dependency fails the build unless a local directory covers it.

## Decisions

- **Selection by declared dependency, not by resolved artifact.** The declared dependencies decide which projects exist. A resolved artifact only supplies the published content of a declared project.
- **Any resolution failure leads to local-only.** When a local directory exists and the published data JAR of that declared dependency fails to resolve for any reason (not found, authentication, network or offline repository), the local content is used alone, and the notice names the reason. The alternative, to fall back only when the module or version is not found, was rejected: Gradle does not expose the failure kind reliably across repository types, and a developer without repository credentials should still be able to test local content. Making the reason visible in the notice and forbidding local-only projects in CI cover the risk that a fallback is taken silently.

## Non-goals

- No new DSL for local projects. A local-only project is declared exactly like any other content project, with its `vulpes(...)` line.
- No automatic publish and no upload of local files.
- No change to the format of `release.json` or `snapshot.json`.
- No skipping of the Maven resolution for merged projects; this change is about projects whose published release does not exist.
- No check that the local version equals the declared version. The notice shows both values, so a difference is visible.
- No change to undeclared directories: they are still ignored with a warning and do not fail under forbid.

## Risks

- A typo in the declared version silently falls back to local-only when a local directory exists. Mitigation: the notice names the unresolved coordinate and the reason; `vulpes.local.forbid=true` in CI fails the build for local-only projects.
- A developer who is offline builds from the local content only, instead of the published merge. Mitigation: the notice on every build names the reason.

## Impact

- `vulpes-gradle-plugin`: lenient resolution of the `vulpesData` view and the diff view, the declared project set taken from the declared dependencies, the selection in `LocalContent` (without Gradle types) for local-only projects, the notice in `VulpesLocalContentTask`, the forbid check, the README section, unit and functional tests.
- No change to `vulpes-format`, the codegen library or the template runtime.
- Depends on `content-local-override` and `content-generation-task` as they are in `openspec/specs/` on main, and on the archived `add-local-content-override` change.
