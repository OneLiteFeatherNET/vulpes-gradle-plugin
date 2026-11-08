# content-generation-task Specification

## Purpose
Lets a game build declare Vulpes content releases as dependencies and generate Java sources from them before compilation.

## Requirements

### Requirement: Plugin and dependency scope
The plugin `net.onelitefeather.vulpes` SHALL add a dependency scope `vulpes` for data JARs and SHALL resolve it without transitive dependencies. The data JARs SHALL be resolved leniently: a declared dependency whose data JAR does not resolve SHALL NOT fail the resolution itself, and SHALL be handled by the rules of the content projects (see the local-only content requirements of `content-local-override`). A declared dependency that does not resolve and has no local content SHALL fail the build with the declared coordinate and the reason. Applying the plugin SHALL require the `java` plugin and SHALL fail with a clear message when it is missing at configuration end.

#### Scenario: Declaring content
- **WHEN** a build applies the plugin and declares `vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")`
- **THEN** `vulpesGenerate` reads that data JAR

#### Scenario: Declared content does not resolve
- **WHEN** a build declares `vulpes("net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT")`, the data JAR does not resolve and no local directory provides `cygnus`
- **THEN** the build fails and names `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT` and the reason, and the configuration does not fail before the task runs

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

### Requirement: Template output
`vulpesGenerate` SHALL write the complete output of the selected template for every content project, including the release constants, and SHALL treat the template name as a task input.

#### Scenario: Template change
- **WHEN** `vulpes.template` changes from `minestom` to `keys` between two runs
- **THEN** the second run is not up to date and the output contains only the files of the `keys` template

### Requirement: Declared content projects
The content projects of a build SHALL be the module dependencies declared in the `vulpes` scope. The module name is the content project name, and the declared version is the coordinate the project is published under. The project set and the declared versions SHALL NOT be derived from the resolved artifacts. The declared coordinates SHALL be inputs of `vulpesGenerate` and `vulpesDiff`, so that changing a declared version re-runs the tasks.

#### Scenario: Unpublished declared project is part of the project set
- **WHEN** `cygnus` is declared as `vulpes("net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT")` and its data JAR does not resolve
- **THEN** `cygnus` is still a content project of the build, with the declared version `0.1.0-SNAPSHOT`

#### Scenario: Changed declared version
- **WHEN** the declared version of `cygnus` changes from `1.4.0` to `1.4.1` between two runs
- **THEN** the second run is not up to date and reads the data JAR of `1.4.1`
