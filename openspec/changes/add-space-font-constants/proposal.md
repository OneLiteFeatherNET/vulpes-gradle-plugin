# Proposal

**Conventional Commits:** `feat(codegen): generate space font constants for every project`

## Why

Every generated resource pack will contain the `space:default` font (vulpes-generator change `add-space-font-to-packs`). Its private-use characters `\uF001`..`\uF01B` move the text cursor by -256..+256 pixels. Games that place a glyph directly, for example a HUD icon followed by a fixed offset, need these characters as named constants. Today each game writes them out by hand, as Manis does with its own `SpaceFont` class, and a typo in a codepoint shows up only as a misplaced HUD in the client.

`vulpes-base` builds offsets from the same characters (`SpaceFont.offset(int)`), but it keeps them private and is only available to the `minestom` template.

## What Changes

- `vulpesGenerate` writes `<Project>SpaceGlyphs` for every project, for both templates `minestom` and `keys`, even when the snapshot contains no fonts.
- The class contains `FONT` (`Key.key("space", "default")`) and one `String` constant per glyph: `NEGATIVE_1`..`NEGATIVE_8`, `NEGATIVE_16`, `NEGATIVE_32`, `NEGATIVE_64`, `NEGATIVE_128`, `NEGATIVE_256`, `ZERO`, `POSITIVE_1`..`POSITIVE_8`, `POSITIVE_16`, `POSITIVE_32`, `POSITIVE_64`, `POSITIVE_128`, `POSITIVE_256`.
- The codepoints are fixed in the plugin. A test compares them with the `space:default` table in `vulpes-base` (`font/font-widths.json`) and fails on any difference.
- A snapshot font whose key or font is in the namespace `space` makes `vulpesGenerate` fail with a message naming the key, matching the reserved namespace in the generator.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `content-codegen`: Adds the space glyph class and the reserved `space` namespace.

## Non-goals

- No change to the snapshot or release format. The constants do not come from the data JAR.
- `<Project>SpaceGlyphs` is not part of `<Project>Content` and is not registered at bootstrap. It only holds constants.
- No offset builder in the generated code. Games that need an arbitrary offset keep using `SpaceFont.offset(int)` from `vulpes-base`.
- `vulpes-base` is not changed.

## Impact

- One more generated class per project. Golden files for both templates gain `CygnusSpaceGlyphs`.
- A project named so that `<Project>SpaceGlyphs` collides with another generated class does not exist today; the existing class name claims cover it.
