# Design

## Context

Every content project reaches the build as a data JAR in the `vulpes` scope, resolved by `vulpesData` and read by `DataJarReader` through `ContentReader`. `VulpesGenerateTask` and `VulpesDiffTask` take those JARs as `@Classpath` inputs and map file names to artifact ids. Stelaris will produce a local build ZIP with the same two files as the JAR, in `vulpes/<project>/`, plus `base` in `release.json`. The reader of format 1 rejects unknown fields (`additionalProperties: false` in the schema, strict parsing), so a `base` record is rejected today.

## Decisions

### Selection per declared project
A project is served from a local directory when the directory `<projectDir>/<localContentDirectory>/<project>/` holds both files. The project set is the set of `vulpes(...)` dependency names; the Maven artifact of a served project is not read. The selection is made at execution time by one helper (`LocalContent`) that takes the directory, the artifact ids by file name and the forbid flag. The helper has no Gradle types, so unit tests run in milliseconds with `@TempDir`.

Alternative considered: decide the selection at configuration time with `File.exists`. Rejected: the configuration cache would not notice a directory that appears or disappears, and the task inputs would have to be recomputed anyway.

### Task inputs
`vulpesGenerate` and `vulpesDiff` receive the whole local content directory tree as `@InputFiles` with `@PathSensitive(RELATIVE)`, filtered to the two file names. An added, changed or deleted file therefore changes the inputs. The data JARs stay `@Classpath`. The helper ignores the JAR of a served project, so switching back to the JAR is a change of the inputs as well. No existence check happens during configuration, so the configuration cache stays valid when directories come and go.

### Undeclared directories are ignored, not used
A directory without a declared dependency is ignored with a warning.
Reasons: the declared `vulpes(...)` lines are the reviewed source of truth for which content a game builds; `vulpes.project("<name>")` already refuses undeclared names; and a leftover directory must not add generated classes silently. Declaring the dependency is required anyway, so the coordinates exist for the Maven fallback.
Alternative considered: use undeclared directories as content. Rejected for this change, and listed as an open question.

### Partial directories fail
A directory with one of the two files fails the build. Falling back to the JAR would hide a half-unzipped ZIP, which is exactly the case the developer is trying to test.

### Notice on every build
A separate `@UntrackedTask` named `vulpesLocalContent` validates the local files, prints the notice and fails on the forbid switch. `vulpesGenerate` depends on it. Printing from `vulpesGenerate` itself would be skipped when the task is up to date, and a local override must never be silent. The task reads only two small files per declared project, so running on every build is cheap.

### Forbid switch
`-Pvulpes.local.forbid=true` is read as a Gradle property and checked by `vulpesLocalContent` before generation. It fails only for served projects; undeclared directories only warn, because they are not used.

### Base record
`base` is optional and read into `Release` as a nullable nested record `Base(repository, branch, commit)`. It is printed in the notice. It is not compared with `libs.versions.vulpes.base`: the plugin pins a version, but `release.json` records a branch and a commit. A branch cannot be mapped to a version, and a commit can only be mapped to a version with a Git lookup, which needs the network and is not reproducible. A mismatch can therefore not be decided reliably, so the plugin prints the recorded values and leaves the judgement to the developer.

### Format number stays 1
The contract in `add-plugin-skeleton` says that additive changes bump the format. This change keeps format 1 and adds an optional field, following `allow-snapshot-versions`, which changed format 1 without a bump because format 1 has not been delivered to the backend. The decision holds only while no format-1 data JAR is published. If one is published, `base` needs format 2 and the reader must accept both; this is the first open question.

### vulpesDiff
The current side of `vulpesDiff` uses the same selection, so a diff compares what the build actually uses. The base side always uses the Maven artifacts of `-Pvulpes.diff.from`.

### Resolution of the Maven artifact
A served project still resolves its Maven artifact, because `vulpesData` is one configuration. A developer therefore still needs the repository credentials. Skipping the download would need per-project resolution and is not part of this change.

## Risks

- A forgotten local directory silently changes which content a release build uses. Mitigation: the notice on every build, and `-Pvulpes.local.forbid=true` in CI.
- Stelaris may emit a `base.commit` that is not a full SHA-1. Mitigation: the format pattern is checked against the Stelaris output before the change ships.

## Open questions

1. Does the backend publish `base` in data JARs too? If yes, the reader needs no further change; if format-1 JARs are already published, `base` requires format 2.
2. Should undeclared local directories become content (for a new content project not yet declared)? This change says no.
3. Should a served project skip its Maven download? This change says no.
4. Should `vulpes.local.forbid` also fail on undeclared directories? This change says warning only.
