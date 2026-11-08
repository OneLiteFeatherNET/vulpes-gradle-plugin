# content-templates Specification

## Purpose
Lets a game choose what is generated (a template) and adds the runtime library the generated code needs.

## Requirements

### Requirement: Template selection
The plugin SHALL offer the templates `minestom` (default) and `keys`, selected with `vulpes.template`, and SHALL fail with a message listing the known templates for any other name.

#### Scenario: Default template
- **WHEN** a build does not set `vulpes.template`
- **THEN** the `minestom` template is used

#### Scenario: Unknown template
- **WHEN** `vulpes.template` is `paper`
- **THEN** the build fails and the message lists `keys` and `minestom`

### Requirement: Runtime dependencies
The plugin SHALL add the runtime library of the selected template with the version the plugin was built for: `net.onelitefeather.vulpes:vulpes-base` for `minestom`, `net.kyori:adventure-key` for `keys`. When the project applies `java-library`, the library SHALL be added to the `api` configuration, so that consumers of the module compile against the generated types. When the project applies only `java`, the library SHALL be added to the `implementation` configuration.

#### Scenario: Minestom template
- **WHEN** a build applies `java-library` and uses the `minestom` template
- **THEN** `api` contains `net.onelitefeather.vulpes:vulpes-base` in the version bundled with the plugin

#### Scenario: Minestom template with java only
- **WHEN** a build applies only `java` and uses the `minestom` template
- **THEN** `implementation` contains `net.onelitefeather.vulpes:vulpes-base` in the version bundled with the plugin

#### Scenario: Consumer of a generated module
- **WHEN** module `game` depends on module `content` with `implementation(project(":content"))`, and `content` applies the plugin with `java-library` and the `minestom` template
- **THEN** `game` compiles code that uses a generated entry type without declaring `vulpes-base` itself

#### Scenario: Keys template with java-library
- **WHEN** a build applies `java-library` and uses the `keys` template
- **THEN** `api` contains `net.kyori:adventure-key` in the version bundled with the plugin

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
