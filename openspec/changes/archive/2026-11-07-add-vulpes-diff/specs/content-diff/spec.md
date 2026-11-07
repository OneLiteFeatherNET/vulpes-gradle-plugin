# Spec Delta

## Purpose

Shows what a new content release changes compared with another release of the same content project.

## ADDED Requirements

### Requirement: Diff task
The plugin SHALL register `vulpesDiff`, which compares the resolved data JAR of a content project with the version given by the Gradle property `vulpes.diff.from` of the same artifact. With several content projects, the property `vulpes.diff.project` SHALL select one; without it the task SHALL compare every content project. The task SHALL be compatible with the configuration cache and SHALL fail with a clear message when `vulpes.diff.from` is missing.

#### Scenario: Compare with an older release
- **WHEN** a build depends on `…content:cygnus:1.5.0` and runs `vulpesDiff -Pvulpes.diff.from=1.4.0`
- **THEN** the task resolves `…content:cygnus:1.4.0` and reports the differences from 1.4.0 to 1.5.0

#### Scenario: Missing base version
- **WHEN** `vulpesDiff` runs without `-Pvulpes.diff.from`
- **THEN** the task fails and the message names the property

### Requirement: Matching by id
The diff SHALL match entities of the same type by `id`. An entity present only in the new release SHALL be reported as added, one present only in the old release as removed, and one with the same `id` but another key as renamed. An entity with the same `id` and key but other values SHALL be reported as changed, naming the changed fields.

#### Scenario: Renamed item
- **WHEN** an item keeps its `id` and its key changes from `cygnus:old_sword` to `cygnus:iron_sword`
- **THEN** the report lists it as renamed from `cygnus:old_sword` to `cygnus:iron_sword` and not as removed plus added

#### Scenario: Changed attribute value
- **WHEN** an attribute keeps `id` and key and its `maximumValue` changes
- **THEN** the report lists it as changed with the field `maximumValue`

### Requirement: Packs and expression functions
The diff SHALL report added and removed packs by `id` and packs whose `sha1` changed. It SHALL report expression functions whose parameters or return type changed.

#### Scenario: New pack content
- **WHEN** the pack `16x` has another `sha1` in the new release
- **THEN** the report lists pack `16x` as changed

### Requirement: Required version bump
The report SHALL state the smallest version bump the changes require: major for a removed key, for a renamed key whose old key is missing from `previousKeys`, or for a changed signature of an interface expression function; minor for an added key or a rename that keeps the old key in `previousKeys`; patch for value or pack changes only.

#### Scenario: Removed key
- **WHEN** an item exists in 1.4.0 and not in 1.5.0
- **THEN** the report states that a major version is required

### Requirement: Report output
The task SHALL print a summary to the console and SHALL write `build/reports/vulpes/diff/<project>-<from>-<to>.md` and `.json` with the full result. Equal releases SHALL produce a report that states that nothing changed.

#### Scenario: No changes
- **WHEN** both versions contain the same snapshot
- **THEN** the console and the reports state that nothing changed and that no version bump is required
