# Spec Delta

## Purpose

Lets a game build declare Vulpes content releases as dependencies and generate Java sources from them before compilation.

## ADDED Requirements

### Requirement: Plugin and dependency scope
The plugin `net.onelitefeather.vulpes` SHALL add a dependency scope `vulpes` for data JARs and SHALL resolve it without transitive dependencies. Applying the plugin SHALL require the `java` plugin and SHALL fail with a clear message when it is missing at configuration end.

#### Scenario: Declaring content
- **WHEN** a build applies the plugin and declares `vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")`
- **THEN** `vulpesGenerate` reads that data JAR

### Requirement: Generation task
The plugin SHALL register `vulpesGenerate`, which writes into `build/generated/sources/vulpes/java/main`, clears that directory at the start of every run, and is a source directory of the `main` source set so that it runs before `compileJava`. The task SHALL be cacheable and compatible with the configuration cache.

#### Scenario: Compile depends on generation
- **WHEN** `compileJava` runs in a build with a `vulpes` dependency
- **THEN** `vulpesGenerate` runs first and the generated sources are compiled

#### Scenario: Up to date
- **WHEN** `vulpesGenerate` runs twice without changes to inputs
- **THEN** the second run is up to date

#### Scenario: Removed content
- **WHEN** the declared content version changes to one that has fewer packs
- **THEN** the output contains no file or constant for the removed packs

#### Scenario: Configuration cache
- **WHEN** a build with the plugin runs twice with `--configuration-cache`
- **THEN** the second run reuses the configuration cache

### Requirement: Packages
The plugin SHALL generate the code of each content project into one package, by default `net.onelitefeather.vulpes.content.<project>` with the project name sanitized to a Java identifier.

#### Scenario: Default package
- **WHEN** the content project `cygnus` is declared without package settings
- **THEN** its code is generated into `net.onelitefeather.vulpes.content.cygnus`

### Requirement: Package overrides
`vulpes.packageName` SHALL set the package when exactly one content project is declared. `vulpes.project("<name>").packageName` SHALL set the package of one content project. When several content projects are declared and `vulpes.packageName` is set, the build SHALL fail and name the per-project setting.

#### Scenario: Single project
- **WHEN** one content project is declared and `vulpes { packageName = "net.onelitefeather.cygnus.content" }`
- **THEN** its code is generated into `net.onelitefeather.cygnus.content`

#### Scenario: Ambiguous package
- **WHEN** two content projects are declared and `vulpes.packageName` is set
- **THEN** the build fails and the message names `vulpes.project("<name>").packageName`

### Requirement: Project consistency
The task SHALL fail when `release.json.project` differs from the artifactId of the data JAR it was read from.

#### Scenario: Wrong artifact
- **WHEN** the JAR `…content:cygnus:1.4.0` contains a `release.json` with `"project": "manis"`
- **THEN** the task fails and names both values

### Requirement: Release constants
For every content project the task SHALL generate a final class `<Project>Release` with the constants `PROJECT`, `VERSION`, `REVISION`, one `Pack` constant per entry of `release.json.packs` named after the upper-cased pack id (`16x` → `PACK_16X`), and `PACKS` holding all packs in contract order. The generated code SHALL NOT contain `provenance` values.

#### Scenario: Pack constants
- **WHEN** `release.json` lists packs `16x` and `32x`
- **THEN** `CygnusRelease.PACK_16X` and `CygnusRelease.PACK_32X` carry id, resolution, URL, SHA-1 and size, and `PACKS` contains both in that order

#### Scenario: No packs yet
- **WHEN** `release.json.packs` is empty
- **THEN** the class compiles and `PACKS` is empty
