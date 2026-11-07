# Spec Delta

## Purpose

Lets a game choose what is generated (a template) and adds the runtime library the generated code needs.

## ADDED Requirements

### Requirement: Template selection
The plugin SHALL offer the templates `minestom` (default) and `keys`, selected with `vulpes.template`, and SHALL fail with a message listing the known templates for any other name.

#### Scenario: Default template
- **WHEN** a build does not set `vulpes.template`
- **THEN** the `minestom` template is used

#### Scenario: Unknown template
- **WHEN** `vulpes.template` is `paper`
- **THEN** the build fails and the message lists `keys` and `minestom`

### Requirement: Runtime dependencies
The plugin SHALL add the runtime library of the selected template to the `implementation` configuration with the version the plugin was built for: `net.onelitefeather.vulpes:vulpes-base` for `minestom`, `net.kyori:adventure-key` for `keys`.

#### Scenario: Minestom template
- **WHEN** a build uses the `minestom` template
- **THEN** `implementation` contains `net.onelitefeather.vulpes:vulpes-base` in the version bundled with the plugin

### Requirement: Supported entity types
Each template SHALL declare the entity types it supports, and generation SHALL fail with a message naming the template and the unsupported types when the snapshot contains entries of other types.

#### Scenario: Unsupported type
- **WHEN** a template supports only items and the snapshot contains sounds
- **THEN** generation fails and the message names the template and `sounds`

### Requirement: Keys template
The `keys` template SHALL generate only key constants, the release constants and the expression function interface, and SHALL NOT reference `vulpes-base`.

#### Scenario: Keys only
- **WHEN** the `keys` template generates the fixture
- **THEN** the sources compile with only `adventure-key` on the classpath
