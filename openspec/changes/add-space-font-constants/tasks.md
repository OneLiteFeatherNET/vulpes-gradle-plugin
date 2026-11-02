# Tasks

## 1. Space glyph class

- [ ] 1.1 Write the failing generator tests: `<Project>SpaceGlyphs` for a snapshot without fonts, for template `minestom` and for template `keys`, with golden files `CygnusSpaceGlyphs`
- [ ] 1.2 Write the failing test that every glyph constant's advance equals the `space:default` table of `vulpes-base` and that the failure names the codepoint
- [ ] 1.3 Implement the generator for both templates

## 2. Reserved namespace

- [ ] 2.1 Write the failing generator and TestKit tests that a font in namespace `space` fails code generation and the message names the key
- [ ] 2.2 Implement the check for all templates

## 3. Verification and pull request

- [ ] 3.1 Run `./gradlew build`
- [ ] 3.2 Open the pull request titled `feat(codegen): generate space font constants for every project`
