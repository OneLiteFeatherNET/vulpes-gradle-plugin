# Spec Delta

## ADDED Requirements

### Requirement: Published code generator artifacts
The code generator SHALL be published as the Maven artifacts `net.onelitefeather.vulpes:vulpes-format` and `net.onelitefeather.vulpes:vulpes-codegen`. Both SHALL carry the version of the Vulpes plugin and SHALL be released together with it, to the same repositories.

#### Scenario: Artifacts of a release
- **WHEN** a release of the plugin version `X.Y.Z` is published
- **THEN** `vulpes-format:X.Y.Z` and `vulpes-codegen:X.Y.Z` are published next to the plugin

#### Scenario: Snapshot version
- **WHEN** a snapshot of version `X.Y.Z-SNAPSHOT` is published
- **THEN** both artifacts are published under `X.Y.Z-SNAPSHOT` to the snapshot repository

### Requirement: Dependencies in the POM
The POM of `vulpes-codegen` SHALL list `vulpes-format` with compile scope, and the libraries it needs at runtime with runtime scope. Libraries needed only for compilation or tests SHALL NOT appear in the POM. Every dependency SHALL carry the resolved version.

#### Scenario: Consumer of vulpes-codegen
- **WHEN** a project declares `net.onelitefeather.vulpes:vulpes-codegen:X.Y.Z` as a dependency
- **THEN** it receives `vulpes-format` on its compile classpath and the runtime libraries on its runtime classpath, and no test library

#### Scenario: Test libraries
- **WHEN** the POM of `vulpes-codegen` is read
- **THEN** it contains no dependency whose scope is test or compile-only

### Requirement: Plugin POM unchanged
The POM of the Vulpes plugin SHALL NOT gain dependencies because of this change. The plugin SHALL keep the code of `vulpes-codegen` inside its own JAR.

#### Scenario: Plugin consumer
- **WHEN** a game applies the plugin
- **THEN** the dependency graph of the game does not change because the code generator is published

### Requirement: Coordinated version
The versions of `vulpes-format`, `vulpes-codegen` and the plugin SHALL be the same in every release.

#### Scenario: Version mismatch in the build
- **WHEN** the plugin of version `X.Y.Z` is built
- **THEN** the `vulpes-codegen` it bundles has the version `X.Y.Z`
