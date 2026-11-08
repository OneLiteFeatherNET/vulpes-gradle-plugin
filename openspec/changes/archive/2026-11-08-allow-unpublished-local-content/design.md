# Design

## Context

Today every content project reaches the build as a data JAR in the `vulpes` scope. `vulpesData` extends that scope, is non-transitive, and is resolved strictly through `getResolvedArtifacts()`. The project set (`mavenVersionsByArtifactId`) and the artifact ids by file name are derived from those resolved artifacts. `LocalContent.resolve` iterates over the resolved projects, so a local directory is only seen for a project that resolved. A declared dependency that cannot be resolved therefore fails the build before any local file is read, and a new project without any publish cannot be tried locally.

The archived change `add-local-content-override` merges local entries over the published snapshot of the same project. This change keeps that merge and adds a second case for projects without a published release.

## Decisions

### The declared dependencies decide the project set
The projects of a build are the module dependencies declared in the `vulpes` scope: the module name is the content project, the declared version is the coordinate the project is published under. The project set no longer comes from resolved artifacts. `LocalContent` and `DataJarReader` receive, per declared project, the declared version and, when the data JAR did not resolve, the reason.

The declared coordinates are `@Input` properties of `vulpesGenerate`, `vulpesDiff` and `vulpesLocalContent`. They are read at execution time through a provider over the `vulpes` scope, so the configuration cache stores their values and a change of the declared version re-runs the tasks.

### Lenient resolution of the data JARs
`vulpesData` and the current view of `vulpesDiff` are read through an artifact view with lenient resolution (`incoming.artifactView { lenient = true }`). A declared dependency that fails to resolve then produces no artifact instead of failing the configuration. The reason is read from the resolution result of the same configuration (its unresolved dependency results, which carry the failure), and it is shortened to one line for the notice. The exact accessor is checked in the first failing functional test; if the resolution result cannot give the reason, the notice still names the coordinate and reports the reason as "not resolved".

The base side of `vulpesDiff` (`vulpesDiffBase`, `-Pvulpes.diff.from`) stays strict. A missing base release still fails the diff, as today.

### Four cases per declared project
For each declared content project the selection is one of four cases:

1. published data JAR resolved and a local directory with both files: merge (unchanged);
2. published data JAR resolved and no local directory: published content only (unchanged);
3. published data JAR not resolved and a local directory with both files: local-only project;
4. published data JAR not resolved and no local directory: the build fails, naming the declared coordinate and the reason, for example `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT: not resolved (not found); no local directory vulpes/cygnus holds release.json and snapshot.json`. No project is skipped silently.

A partial directory (one file only) fails in every case where a directory exists, as today. It is never used as a local-only project.

Alternative considered: fall back to local-only only when Gradle reports "not found". Rejected. Gradle does not expose the failure kind reliably across repository types (a missing module, a 401 from a credentials-protected repository and an offline repository can surface as different exception types or messages). A developer without repository credentials should still be able to test local content. The fallback therefore covers every resolution failure of that dependency, and the notice names the reason so that a 401 or an offline repository is visible.

### Local-only validation
A local-only project has no published snapshot, so its local files are the complete content of the project. The local files get the format checks of a local directory (format reader, supported format, `revision` against `snapshot.json`, `release.project` against the directory name, version form `X.Y.Z` or `X.Y.Z-SNAPSHOT`) and then the consistency checks of a single snapshot: unique ids, unique key or name per list, unique name across expressions and expression functions, no previous key or name that equals a current one, unique asset paths, every used resource location has an asset, and advancement parents that exist and form a forest. Those rules are the ones that `ContentMerge` already applies to a merged snapshot, so the local-only path reuses them with the local side as the only side. A failure names the project, the conflicting entries and `local` as the side.

A local-only snapshot that refers to an entry that exists nowhere (for example a font asset `cygnus:font/ui.png` without an asset) fails. This is the intended difference to a merged project, where such a reference may point at a published asset.

The local `release.json` describes the content, including its version, revision, base and packs. Generated constants (`CygnusRelease`) take their values from the local file.

The declared version and the local version are not compared. A local-only build usually declares `0.1.0-SNAPSHOT` and carries the same local version, but the notice prints both values, so a difference is visible without a new rule.

### Notice
`vulpesLocalContent` prints one lifecycle line per local-only project, in the same task that prints the merged lines. The line names the project, the local version, that no published release is used, the declared coordinate, the reason and the recorded base, or states that no base is recorded. Example:

```
vulpes local content: cygnus 0.1.0-SNAPSHOT: no published release used, local content only; could not resolve net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT: not found; base main at ...
```

Merged projects keep the line of the archived change. The notice is printed on every build, as before, including when `vulpesGenerate` is up to date.

### Forbid switch
`vulpes.local.forbid=true` fails the build before generation when any declared project uses a local directory, merged or local-only. The message names each such project, and for local-only projects it also names the declared coordinate and the reason, so that the CI log shows the missing publish. Undeclared directories still only warn.

### vulpesDiff
The current side of `vulpesDiff` uses the same selection, so a local-only project appears on the current side with its local content. The base side is unchanged: it resolves `-Pvulpes.diff.from` strictly through Maven and fails as today when that version does not exist.

### Task inputs and configuration cache
The local files stay `@InputFiles` with `@PathSensitive(RELATIVE)`. The data JARs stay `@Classpath`. The declared coordinates are added as inputs. Consequences:

- An artifact that becomes resolvable changes the classpath, so the task re-runs and the project switches from local-only to merge. The notice then names the published version.
- A removed local directory of a local-only project makes the next run fail, because nothing else provides the project. The message names the dependency and the reason. A removed local directory of a merged project returns that project to the published content, as before.
- No existence check runs during configuration, so the configuration cache stays valid when directories come and go, as in the archived change.

### Undeclared directories
Unchanged. A directory without a `vulpes(...)` dependency is ignored with a warning and adds no content, and it does not fail under forbid. The dependency line is still required, because the declared coordinate is the source of truth for which content a game uses and for which version it is published under.

### Non-goals
- No new DSL for local projects.
- No automatic publish and no upload.
- No change to the format of `release.json` or `snapshot.json`.
- No change to the merge rules of merged projects.

## Risks

- A typo in the declared version silently selects local-only when a local directory exists. Mitigation: the notice names the coordinate and the reason on every build, and `-Pvulpes.local.forbid=true` fails the build in CI.
- A developer who is offline builds from the local content alone, instead of the published merge. Mitigation: the notice names the reason.
- A local-only snapshot is checked more strictly than a merged one, so a local file that refers to a published entry fails once the project has no published release. Mitigation: the message names the missing reference and the local side; the developer publishes or adds the entry.

## Open questions

None. The reason text is the first line of the resolution failure, shortened; the exact accessor is confirmed by the first failing functional test, and that test decides the wording of the reason, not the decision.
