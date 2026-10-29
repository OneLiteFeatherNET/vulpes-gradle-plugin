# Spec Delta

## Purpose

Generates Java code for every entity type of the content contract (format 1) from a data JAR.

## ADDED Requirements

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
