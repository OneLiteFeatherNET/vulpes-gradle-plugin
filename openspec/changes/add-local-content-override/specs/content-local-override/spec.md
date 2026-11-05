# Spec Delta

## Purpose

Lets a developer test content that is not published yet: a local directory with `release.json` and `snapshot.json` replaces the data JAR of the same declared content project for the build.

## ADDED Requirements

### Requirement: Local content directory
For every declared content project, the plugin SHALL use the directory `<projectDir>/<localContentDirectory>/<project>/` instead of the data JAR when that directory contains both `release.json` and `snapshot.json`. `localContentDirectory` SHALL default to `vulpes` and SHALL be set with `vulpes { localContentDirectory = "..." }`, relative to the project directory.

#### Scenario: Local directory replaces the data JAR
- **WHEN** `vulpes/cygnus/` holds valid `release.json` and `snapshot.json` and `cygnus` is declared as `vulpes("net.onelitefeather.vulpes.content:cygnus:1.4.0")`
- **THEN** `vulpesGenerate` generates the code from the local files and does not read the data JAR of `cygnus`

#### Scenario: Custom directory
- **WHEN** `vulpes { localContentDirectory = "content" }` is set and `content/cygnus/` holds both files
- **THEN** the local files of `content/cygnus/` are used

#### Scenario: Partial directory
- **WHEN** `vulpes/cygnus/` holds `release.json` but not `snapshot.json`
- **THEN** the build fails and names the missing file and the directory, and does not fall back to the data JAR

### Requirement: Local content validation
Local files SHALL pass the same checks as a data JAR: `release.json` and `snapshot.json` are read with the format reader, the format is supported, the `revision` matches `snapshot.json`, and `release.project` equals the directory name. A failing check SHALL fail the build with a message that names the directory and the check.

#### Scenario: Project does not match directory
- **WHEN** `vulpes/cygnus/release.json` contains `"project": "manis"`
- **THEN** the build fails and names `vulpes/cygnus` and both values

#### Scenario: Unsupported format
- **WHEN** `vulpes/cygnus/release.json` contains `"format": 2`
- **THEN** the build fails and names the supported format

### Requirement: Undeclared local directories
A directory in the local content directory for a project that is not declared as a `vulpes(...)` dependency SHALL NOT add content. The plugin SHALL log a warning that names the directory and states that it is ignored.

#### Scenario: Undeclared directory
- **WHEN** `vulpes/manis/` holds valid files and `manis` is not declared
- **THEN** no code is generated for `manis`, the build succeeds, and the warning names `vulpes/manis`

### Requirement: Local override notice
Every build that serves a declared content project from a local directory SHALL log one lifecycle line per project that names the project, the local version, the Maven version it replaces and the recorded `base` branch and commit, or states that no base is recorded. The notice SHALL be printed even when `vulpesGenerate` is up to date.

#### Scenario: Notice names the override
- **WHEN** `cygnus` is served from `vulpes/cygnus/` with version `1.4.1-SNAPSHOT` and a recorded base branch `main`
- **THEN** the lifecycle output names `cygnus`, `1.4.1-SNAPSHOT`, the replaced Maven version and the base branch `main`

#### Scenario: Up-to-date generation
- **WHEN** `vulpesGenerate` is up to date and the local directory is unchanged
- **THEN** the lifecycle output still contains the notice

### Requirement: Forbid local content
The property `vulpes.local.forbid` SHALL default to `false`. When it is `true` and a declared content project is served from a local directory, the build SHALL fail before code is generated and the message SHALL name each such project.

#### Scenario: CI forbids local content
- **WHEN** the build runs with `-Pvulpes.local.forbid=true` and `vulpes/cygnus/` is used
- **THEN** the build fails and names `cygnus`

#### Scenario: Forbid without local content
- **WHEN** the build runs with `-Pvulpes.local.forbid=true` and no declared project has a local directory
- **THEN** the build succeeds

### Requirement: Local files as task inputs
The local files SHALL be inputs of the generation task and of `vulpesDiff`. Adding, changing or removing them SHALL change the inputs, so that the task re-runs. Removing a local directory SHALL switch the project back to its data JAR. The task SHALL remain cacheable and compatible with the configuration cache.

#### Scenario: Changed local file
- **WHEN** `vulpesGenerate` runs twice and `vulpes/cygnus/snapshot.json` changed between the runs
- **THEN** the second run is not up to date and regenerates the code

#### Scenario: Removed local directory
- **WHEN** `vulpes/cygnus/` is deleted after a run with the local files
- **THEN** the next run generates from the data JAR of `cygnus`

#### Scenario: Configuration cache
- **WHEN** a build with a local directory runs twice with `--configuration-cache`
- **THEN** the second run reuses the configuration cache

### Requirement: Base information
When `release.json` carries `base`, the plugin SHALL print its `repository`, `branch` and `commit` in the local override notice. The plugin SHALL NOT compare `base` with the vulpes-base version it pins and SHALL NOT fail or warn because of it.

#### Scenario: Base is printed
- **WHEN** local `release.json` records base branch `main` at commit `0123abc…`
- **THEN** the notice contains the branch and the commit

#### Scenario: Base differs from pinned version
- **WHEN** the recorded base branch does not correspond to the pinned vulpes-base version
- **THEN** the build succeeds without a warning about the base
