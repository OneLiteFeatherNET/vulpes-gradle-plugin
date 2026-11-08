# Tasks

## 1. Checks without Gradle types

- [ ] 1.1 Write the failing unit tests in `ContentMergeTest` (in-memory snapshots, no Gradle types): a local-only snapshot without assets whose font uses `cygnus:font/ui.png` passes and reports one missing asset with the font, the side `local` and the location; a merged snapshot reports a published sound file entry without asset with the side `published`; each reference is reported once; a snapshot whose assets cover every location reports nothing; a duplicate key next to a missing asset still fails
- [ ] 1.2 Make `ContentMerge` collect missing assets instead of throwing; expose them on `Merged` and as the result of the local-only check; keep every other rule and message unchanged; make the tests of 1.1 green

## 2. Warning in the build

- [ ] 2.1 Write the failing functional test (TestKit, `@TempDir`, file repository, no network): a local-only project without assets whose font and sound refer to locations generates code, the build succeeds, and the output holds one warning per reference naming the project, the entry, the side and the location; the warnings are printed again when `vulpesGenerate` is up to date
- [ ] 2.2 Pass the missing assets through `LocalContent` and `DataJarReader` and log them as warnings in `VulpesLocalContentTask`, once per build; make the test of 2.1 green

## 3. Documentation and verification

- [ ] 3.1 Update the README section "Testing content locally": a missing asset is a warning, not a failure
- [ ] 3.2 Run the plugin build with tests once through `heavy` with `--max-workers=2`
- [ ] 3.3 Open the code pull request titled `fix(plugin): warn instead of failing on missing local assets`
