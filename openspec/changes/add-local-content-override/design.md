# Design

## Context

Every content project reaches the build as a data JAR in the `vulpes` scope, resolved by `vulpesData` and read by `DataJarReader` through `ContentReader`. `VulpesGenerateTask` and `VulpesDiffTask` take those JARs as `@Classpath` inputs and map file names to artifact ids. Stelaris will produce a local build ZIP with the same two files as the JAR, in `vulpes/<project>/`, plus `base` in `release.json`, written as format 2. The reader of format 1 rejects unknown fields (`additionalProperties: false` in the schema, strict parsing), so a `base` record is rejected today.

## Decisions

### Selection per declared project
A project is served from a local directory when the directory `<projectDir>/<localContentDirectory>/<project>/` holds both files. The project set is the set of `vulpes(...)` dependency names; the Maven artifact of a served project is not read. The selection is made at execution time by one helper (`LocalContent`) that takes the directory, the artifact ids by file name and the forbid flag. The helper has no Gradle types, so unit tests run in milliseconds with `@TempDir`.

Alternative considered: decide the selection at configuration time with `File.exists`. Rejected: the configuration cache would not notice a directory that appears or disappears, and the task inputs would have to be recomputed anyway.

### Task inputs
`vulpesGenerate` and `vulpesDiff` receive the whole local content directory tree as `@InputFiles` with `@PathSensitive(RELATIVE)`, filtered to the two file names. An added, changed or deleted file therefore changes the inputs. The data JARs stay `@Classpath`. The helper ignores the JAR of a served project, so switching back to the JAR is a change of the inputs as well. No existence check happens during configuration, so the configuration cache stays valid when directories come and go.

### Undeclared directories are ignored with a warning
A directory without a declared dependency is ignored with a warning that names it. It never adds content and never fails the build, not even with the forbid switch.
Reasons: the declared `vulpes(...)` lines are the reviewed source of truth for which content a game builds; `vulpes.project("<name>")` already refuses undeclared names; and a leftover directory must not add generated classes silently. Declaring the dependency is required anyway, so the coordinates exist for the Maven fallback.
Alternative considered: use undeclared directories as content. Rejected.

### Partial directories fail
A directory with one of the two files fails the build. Falling back to the JAR would hide a half-unzipped ZIP, which is exactly the case the developer is trying to test.

### Notice on every build
A separate `@UntrackedTask` named `vulpesLocalContent` validates the local files, prints the notice and fails on the forbid switch. `vulpesGenerate` depends on it. Printing from `vulpesGenerate` itself would be skipped when the task is up to date, and a local override must never be silent. The task reads only two small files per declared project, so running on every build is cheap.

### Forbid switch
`-Pvulpes.local.forbid=true` is read as a Gradle property and checked by `vulpesLocalContent` before generation. It fails only for served projects; undeclared directories only warn, as decided above.

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
The current side of `vulpesDiff` uses the same selection, so a diff compares what the build actually uses. The base side always uses the Maven artifacts of `-Pvulpes.diff.from`.

### Resolution of the Maven artifact
A served project still resolves its Maven artifact, because `vulpesData` is one configuration. A developer therefore still needs the repository credentials. Skipping the download would need per-project resolution and is not part of this change.

## Risks

- A forgotten local directory silently changes which content a release build uses. Mitigation: the notice on every build, and `-Pvulpes.local.forbid=true` in CI.
- The generator writes format 2 before a plugin that reads it is released, and older plugins fail on every build. Mitigation: the ordering constraint above, stated in the release notes of this change.

## Open questions

1. Should a served project skip its Maven download? This change says no.
