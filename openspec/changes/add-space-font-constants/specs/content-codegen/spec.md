# Spec Delta

## ADDED Requirements

### Requirement: Space glyph constants
Code generation SHALL write `<Project>SpaceGlyphs` for every project and every template. The class SHALL contain `FONT` with the key `space:default` and one `String` constant per glyph of the space font, named after its advance.

#### Scenario: Glyph codepoints
- **WHEN** code generation writes `<Project>SpaceGlyphs`
- **THEN** `NEGATIVE_1`..`NEGATIVE_8` are `\uF001`..`\uF008`, `NEGATIVE_16`, `NEGATIVE_32`, `NEGATIVE_64`, `NEGATIVE_128` and `NEGATIVE_256` are `\uF009`..`\uF00D`, `ZERO` is `\uF00E`, `POSITIVE_1`..`POSITIVE_8` are `\uF00F`..`\uF016`, and `POSITIVE_16`, `POSITIVE_32`, `POSITIVE_64`, `POSITIVE_128` and `POSITIVE_256` are `\uF017`..`\uF01B`

#### Scenario: Project without fonts
- **WHEN** the snapshot contains no fonts
- **THEN** code generation writes `<Project>SpaceGlyphs` with all 27 glyph constants and `FONT`

#### Scenario: Template keys
- **WHEN** code generation runs with template `keys`
- **THEN** `<Project>SpaceGlyphs` is written and compiles against `adventure-key` alone

### Requirement: Space glyphs match vulpes-base
The advance of every glyph constant SHALL equal the advance of the same codepoint in the `space:default` table of `vulpes-base` (`font/font-widths.json`), and the constant name SHALL state that advance. A test SHALL fail on any difference and name the codepoint.

#### Scenario: Table drifts
- **WHEN** the `space:default` table of `vulpes-base` gives a codepoint another advance than the constant name states
- **THEN** the test fails and names the codepoint

### Requirement: Reserved space namespace
Code generation SHALL fail when a font in the snapshot has a key or font in the namespace `space`. The message SHALL name the key.

#### Scenario: Project font in space namespace
- **WHEN** the snapshot has a font whose font key is `space:default`
- **THEN** code generation fails and the message names the font's key
