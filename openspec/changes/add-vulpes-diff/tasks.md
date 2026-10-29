# Tasks

## 1. Diff model (`vulpes-format`)

- [ ] 1.1 Write the failing unit tests for the comparison: added, removed, renamed by `id`, changed fields, packs, expression signatures, required bump, no changes
- [ ] 1.2 Implement the comparison of two snapshot/release pairs until the tests pass

## 2. Task

- [ ] 2.1 Write the failing TestKit tests: compares with `-Pvulpes.diff.from`, fails without it, selects a project with `-Pvulpes.diff.project`, writes Markdown and JSON reports, reuses the configuration cache
- [ ] 2.2 Add a resolvable configuration for the base version, filled lazily from the Gradle properties
- [ ] 2.3 Implement `vulpesDiff` with console summary and report files

## 3. Verification and pull request

- [ ] 3.1 Run `./gradlew build` with tests
- [ ] 3.2 Open the pull request titled `feat(diff): compare two vulpes content releases`
