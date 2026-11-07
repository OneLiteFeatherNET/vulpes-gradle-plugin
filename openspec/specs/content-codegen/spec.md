# content-codegen Specification

## Purpose
Generates Java code for every entity type of the content contract (format 1) from a data JAR.

## Requirements

### Requirement: Entity code
The generator SHALL produce, for every entity type of the snapshot, a key constant per entry and, where the template builds entries, a constant per entry that is constructed with the public factories of `vulpes-base`, plus a list of all entries of that type.

#### Scenario: Item constant
- **WHEN** the snapshot contains item `cygnus:iron_sword` with material `minecraft:iron_sword`, amount 1 and lore
- **THEN** `CygnusItemKeys.IRON_SWORD` is the key `cygnus:iron_sword` and `CygnusItems.IRON_SWORD` builds that item with its components

#### Scenario: Type without entries
- **WHEN** the snapshot contains no sounds
- **THEN** the sound classes are generated and their list of all entries is empty

### Requirement: Identifiers from keys
The generator SHALL derive constant names from the keys and SHALL fail the generation with a message naming both keys when two keys of one type map to the same constant name.

#### Scenario: Colliding keys
- **WHEN** a snapshot contains items `cygnus:iron-sword` and `cygnus:iron_sword`
- **THEN** generation fails and the message names both keys

### Requirement: Deprecated aliases
For every key in `previousKeys` the generator SHALL emit a constant marked `@Deprecated(forRemoval = true)` that refers to the current entry, and the content object SHALL map the alias key to the current key.

#### Scenario: Renamed item
- **WHEN** item `cygnus:iron_sword` lists `cygnus:old_sword` in `previousKeys`
- **THEN** `CygnusItems.OLD_SWORD` is deprecated for removal and refers to `CygnusItems.IRON_SWORD`

### Requirement: Content object
For the `minestom` template the generator SHALL emit `<Project>Content` implementing `VulpesContent` with a singleton `INSTANCE` that returns all generated entries and aliases.

#### Scenario: Bootstrap generated content
- **WHEN** a test calls `Vulpes.bootstrap(CygnusContent.INSTANCE)` on the code generated from the fixture
- **THEN** the item registry contains every item key of the fixture snapshot

### Requirement: Expression function interface
The generator SHALL emit `<Project>ExpressionFunctions` with one method per function of kind `interface`, sorted by name ignoring case, mapping `number` to `BigDecimal`, `boolean` to `boolean` and `string` to `String`. It SHALL NOT emit the interface when there is no such function.

#### Scenario: Interface function
- **WHEN** the snapshot contains interface function `inArena(arena: string): boolean`
- **THEN** the interface declares `boolean inArena(String arena)`

### Requirement: Resource pack infos
For the `minestom` template the generator SHALL emit one resource pack info per pack of `release.json`, built from its URL and SHA-1.

#### Scenario: Two packs
- **WHEN** `release.json` lists packs `16x` and `32x`
- **THEN** `CygnusResourcePacks.PACK_16X` and `PACK_32X` carry the URL and hash of their pack

### Requirement: Deterministic output
Equal content and package SHALL produce byte-identical sources.

#### Scenario: Repeated generation
- **WHEN** the same data JAR is generated twice
- **THEN** both outputs are byte-identical

### Requirement: Typed formula access
Template `minestom` SHALL generate `<Project>Formulas` with one method per formula function and expression, mapping `number` to `BigDecimal`, `boolean` to `boolean` and `string` to `String`. No class SHALL be generated when the project has neither.

#### Scenario: Formula function
- **WHEN** the snapshot has the formula `armorDamage(base: number, armor: number): number`
- **THEN** `CygnusFormulas` has the method `BigDecimal armorDamage(BigDecimal base, BigDecimal armor)` and evaluating it returns the result of the formula

#### Scenario: Interface function used by a formula
- **WHEN** the project has interface functions
- **THEN** the constructor of `CygnusFormulas` takes `CygnusExpressionFunctions` and formulas can call its methods

#### Scenario: Renamed expression
- **WHEN** an expression lists `oldName` in `previousNames`
- **THEN** a deprecated method `oldName` calls the current method

#### Scenario: Keys template
- **WHEN** template `keys` is used
- **THEN** no `CygnusFormulas` is generated

### Requirement: Built-in function names
Code generation SHALL fail when an expression function or an expression has the name of a built-in EvalEx function, ignoring case. The message SHALL name the function.

#### Scenario: Function named like a built-in
- **WHEN** the snapshot has an expression function named `max`
- **THEN** code generation fails and the message names `max`

#### Scenario: Different case
- **WHEN** the snapshot has an expression named `IF`
- **THEN** code generation fails and the message names `IF`

#### Scenario: Other name
- **WHEN** no name matches a built-in function
- **THEN** code generation succeeds

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
