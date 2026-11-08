# Tasks

## 1. Selection without Gradle types

- [x] 1.1 Write the failing unit tests for `LocalContent` (`@TempDir`, declared projects given as a map of declared version and optional resolution reason): a declared project without a published JAR and with both files is a local-only project, named with its declared version and reason; a resolved project with both files is merged; a declared project without a published JAR and without a directory is reported as unresolvable with its coordinate and reason; a partial directory fails also for a local-only project; a local-only project with a format error fails naming the directory; a local version that is neither `X.Y.Z` nor `X.Y.Z-SNAPSHOT` fails; forbid lists merged and local-only projects and names the coordinate and reason of local-only projects; an undeclared directory is reported as ignored and does not fail under forbid
- [x] 1.2 Write the failing unit tests for the cross-entry checks of a local-only snapshot (in-memory snapshots, no Gradle types): a duplicate key within the local snapshot fails naming both ids and the side `local`; a missing font asset fails naming the side `local`; a valid local snapshot with an advancement tree passes
- [x] 1.3 Implement the local-only case in `LocalContent` and the single-snapshot checks for a local-only snapshot in `ContentMerge` (or a sibling helper without Gradle types), reusing the existing rules; keep the merged path unchanged; make the tests of 1.1 and 1.2 green

## 2. Lenient resolution and the notice

- [x] 2.1 Write the failing functional tests (TestKit, `@TempDir`, file repository, no network): a declared project that does not resolve and has a local directory generates code and compiles; the notice names the coordinate, the local version, the reason (`not found`) and the base; a declared project that does not resolve without a local directory fails and names the coordinate and the reason; a 401-style failure with a local directory is used locally and the notice names that reason (use a repository that fails with an authentication error, if the file repository cannot simulate it, a unit test for the reason text is enough)
- [x] 2.2 Write the failing functional tests for the forbid switch and the lifecycle output: `-Pvulpes.local.forbid=true` fails for a local-only project and names it; a merged project with forbid still fails as before; the notice of a local-only project is printed when `vulpesGenerate` is up to date
- [x] 2.3 Switch `vulpesData` and the current view of `vulpesDiff` to a lenient artifact view; take the project set and the declared versions from the declared dependencies of the `vulpes` scope; take the reason of an unresolved declared dependency from its resolution result, shortened to one line; make the tests of 2.1 and 2.2 green
- [x] 2.4 Update `VulpesLocalContentTask` and `DataJarReader` for the local-only notice and the forbid message; add the declared coordinates as `@Input` properties of `vulpesGenerate`, `vulpesDiff` and `vulpesLocalContent`; keep the base side of `vulpesDiff` strict

## 3. Task inputs and configuration cache

- [x] 3.1 Write the failing functional tests: a local-only project whose data JAR later becomes resolvable switches to the merge on the next run, and the notice names the published version; a removed local directory of a local-only project fails naming the coordinate and the reason; a removed local directory of a merged project returns to the published content and re-runs; a build with a local-only project runs twice with `--configuration-cache` and reuses the cache
- [x] 3.2 Make the tests of 3.1 green without changing the task types: the declared coordinates and the local files are inputs, the data JARs stay `@Classpath`, and no existence check runs during configuration

## 4. Documentation and verification

- [x] 4.1 Update the README section "Testing content locally": a content project can be tested before its first publish when its `vulpes(...)` line names the version it will be published under; the notice of a local-only project and the reason; `-Pvulpes.local.forbid=true` also fails for local-only projects; the Maven artifact is still needed for merged projects; the undeclared-directory rule is unchanged
- [x] 4.2 Run `./gradlew build` with tests, once, under `flock ~/.cache/claude-heavy.lock`; check `MemAvailable` in `/proc/meminfo` before starting
- [x] 4.3 Open the code pull request titled `feat(plugin): use local content when no published release exists`
