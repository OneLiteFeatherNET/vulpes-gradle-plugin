## MODIFIED Requirements

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
