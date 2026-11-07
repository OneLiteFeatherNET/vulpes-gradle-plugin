# Tasks

## 1. Format: format 2 with optional base record

- [ ] 1.1 Write the failing reader tests: format 1 without `base` still accepted; format 2 with a full SHA-1 `commit` accepted; format 2 without `base` accepted; `base` in format 1 rejected; unknown field in `base` and a short commit rejected; format 3 rejected naming the supported formats
- [ ] 1.2 Add the format-2 schema `release-2.schema.json` (format 1 plus optional `base`), the `Release` record with a nullable `Base`, and reader support for formats 1 and 2; keep the contract tests green

## 2. Local content resolution and merge

- [ ] 2.1 Write the failing unit tests for `LocalContent` (`@TempDir`): both files present selects the directory, one file missing fails naming it, directory name differs from `release.project` fails, a version that is neither `X.Y.Z` nor `X.Y.Z-SNAPSHOT` fails, local files get format checks only (a local reference to a published asset is accepted), undeclared directory is reported as ignored and does not fail under forbid, forbid lists merged projects, no directory keeps the published content only. Write the failing unit tests for `ContentMerge` (in-memory snapshots, no Gradle types): local entry replaces published entry by id, rename through a stable id leaves no old key, new id is added, published-only entry is kept, assets are matched by path, expressions are matched by id, `projectId` mismatch fails naming both values, `namespace` mismatch fails naming both values, key conflict after the merge fails naming both ids and both sides, name clash between an expression and an expression function after the merge fails, dangling advancement parent key after the merge fails naming the published child, the missing key and the local replacement with their sides
- [ ] 2.2 Implement `LocalContent` without Gradle types: reuse `ContentReader` for the format checks of the local files only. Implement `ContentMerge` without Gradle types: the merge rules by stable id, the counts for the notice, and then the consistency checks of a single snapshot on the merged result only

## 3. Plugin wiring

- [ ] 3.1 Write the failing functional tests (TestKit, `@TempDir`, file repository, no network): local entries merge over the data JAR; partial directory fails; undeclared directory warns and adds no content; notice names the local version, the published version, the replaced and added entry counts and the base; notice printed when `vulpesGenerate` is up to date; forbid fails and names the project; forbid with undeclared directory succeeds; removing the directory returns to the published content and re-runs; configuration cache reuse with a local directory
- [ ] 3.2 Add the extension property `localContentDirectory` and the `vulpesLocalContent` untracked task; make `vulpesGenerate` depend on it
- [ ] 3.3 Switch `vulpesGenerate` and `vulpesDiff` to `LocalContent` and `ContentMerge` with `@InputFiles @PathSensitive(RELATIVE)` local inputs

## 4. Documentation and verification

- [ ] 4.1 Add the README section "Testing content locally": where the ZIP goes, the `.gitignore` entry for `vulpes/`, the notice, the merge by stable id and the rule that deleting is not possible, `-Pvulpes.local.forbid=true` for CI, the undeclared-directory rule, and that a `base` record needs a plugin release with format 2 support (generator writes format 2 only after that release)
- [ ] 4.2 Run `./gradlew build` with tests, once, under `flock ~/.cache/claude-heavy.lock`
- [ ] 4.3 Open the code pull request titled `feat(plugin): merge local content over published data jars`
