## MODIFIED Requirements

### Requirement: Plugin and dependency scope
The plugin `net.onelitefeather.vulpes` SHALL add a dependency scope `vulpes` for data JARs and SHALL resolve it without transitive dependencies. The data JARs SHALL be resolved leniently: a declared dependency whose data JAR does not resolve SHALL NOT fail the resolution itself, and SHALL be handled by the rules of the content projects (see the local-only content requirements of `content-local-override`). A declared dependency that does not resolve and has no local content SHALL fail the build with the declared coordinate and the reason. Applying the plugin SHALL require the `java` plugin and SHALL fail with a clear message when it is missing at configuration end.

#### Scenario: Declaring content
- **WHEN** a build applies the plugin and declares `vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")`
- **THEN** `vulpesGenerate` reads that data JAR

#### Scenario: Declared content does not resolve
- **WHEN** a build declares `vulpes("net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT")`, the data JAR does not resolve and no local directory provides `cygnus`
- **THEN** the build fails and names `net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT` and the reason, and the configuration does not fail before the task runs

## ADDED Requirements

### Requirement: Declared content projects
The content projects of a build SHALL be the module dependencies declared in the `vulpes` scope. The module name is the content project name, and the declared version is the coordinate the project is published under. The project set and the declared versions SHALL NOT be derived from the resolved artifacts. The declared coordinates SHALL be inputs of `vulpesGenerate` and `vulpesDiff`, so that changing a declared version re-runs the tasks.

#### Scenario: Unpublished declared project is part of the project set
- **WHEN** `cygnus` is declared as `vulpes("net.onelitefeather.vulpes.content:cygnus:0.1.0-SNAPSHOT")` and its data JAR does not resolve
- **THEN** `cygnus` is still a content project of the build, with the declared version `0.1.0-SNAPSHOT`

#### Scenario: Changed declared version
- **WHEN** the declared version of `cygnus` changes from `1.4.0` to `1.4.1` between two runs
- **THEN** the second run is not up to date and reads the data JAR of `1.4.1`
