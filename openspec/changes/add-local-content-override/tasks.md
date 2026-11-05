# Tasks

## 1. Format: optional base record

- [ ] 1.1 Write the failing reader tests: `base` is optional, a full SHA-1 `commit` is accepted, an unknown field in `base` and a short commit are rejected
- [ ] 1.2 Add `base` to `release-1.schema.json`, the `Release` record with a nullable `Base`, and the reader; keep the contract tests green

## 2. Local content resolution

- [ ] 2.1 Write the failing unit tests for `LocalContent` (`@TempDir`): both files present selects the directory, one file missing fails naming it, directory name differs from `release.project` fails, undeclared directory is reported as ignored, forbid lists served projects, no directory keeps the JAR
- [ ] 2.2 Implement `LocalContent` without Gradle types and reuse `ContentReader` for validation

## 3. Plugin wiring

- [ ] 3.1 Write the failing functional tests (TestKit, `@TempDir`, file repository, no network): local directory replaces the data JAR; partial directory fails; undeclared directory warns and adds no content; notice names version, replaced Maven version and base; notice printed when `vulpesGenerate` is up to date; forbid fails and names the project; removing the directory switches back and re-runs; configuration cache reuse with a local directory
- [ ] 3.2 Add the extension property `localContentDirectory` and the `vulpesLocalContent` untracked task; make `vulpesGenerate` depend on it
- [ ] 3.3 Switch `vulpesGenerate` and `vulpesDiff` to `LocalContent` with `@InputFiles @PathSensitive(RELATIVE)` local inputs

## 4. Documentation and verification

- [ ] 4.1 Add the README section "Testing content locally": where the ZIP goes, the `.gitignore` entry for `vulpes/`, the notice, `-Pvulpes.local.forbid=true` for CI, the undeclared-directory rule
- [ ] 4.2 Run `./gradlew build` with tests, once, under `flock ~/.cache/claude-heavy.lock`
- [ ] 4.3 Open the code pull request titled `feat(plugin): use local content directories instead of published data jars`
