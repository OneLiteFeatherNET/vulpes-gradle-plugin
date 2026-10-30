# Spec Delta

## Purpose

Defines the data JAR of a Vulpes content release (format 1) and how a consumer reads it into a validated model.

## ADDED Requirements

### Requirement: Data JAR layout
A content release SHALL be a JAR `net.onelitefeather.vulpes.content:<project>:<version>` that contains `META-INF/vulpes/snapshot.json` and `META-INF/vulpes/release.json`, both conforming to the format-1 JSON Schemas of this repository. The JAR SHALL NOT contain classes.

#### Scenario: Valid data JAR
- **WHEN** a JAR with both files in format 1 is read
- **THEN** the reader returns the release and the snapshot model

#### Scenario: Missing file
- **WHEN** a JAR lacks `META-INF/vulpes/snapshot.json` or `META-INF/vulpes/release.json`
- **THEN** reading fails with a message that names the JAR and the missing file

### Requirement: Format version
The reader SHALL accept only `format` values it supports in `release.json` and in `snapshot.json`. For any other value it SHALL fail with a message that names the file, the found value and the supported values.

#### Scenario: Unknown release format
- **WHEN** `release.json` has `"format": 2` and the reader supports only 1
- **THEN** reading fails and the message names `release.json`, `2` and `1`

#### Scenario: Unknown snapshot format
- **WHEN** `snapshot.json` has `"format": 2` and the reader supports only 1
- **THEN** reading fails and the message names `snapshot.json`, `2` and `1`

### Requirement: Revision integrity
The reader SHALL compute the SHA-256 of the exact bytes of `snapshot.json` and SHALL fail when `release.json.revision` is not `sha256:` followed by that digest in lowercase hex.

#### Scenario: Tampered snapshot
- **WHEN** one byte of `snapshot.json` differs from the bytes the revision was computed over
- **THEN** reading fails with a message that shows the expected and the actual revision

### Requirement: Strict model
The reader SHALL reject unknown properties and missing required properties, and SHALL report the JSON path of the offending value.

#### Scenario: Unknown property
- **WHEN** an item in `snapshot.json` has a property not defined in format 1
- **THEN** reading fails and the message contains the JSON path of that property

### Requirement: Contract fixtures
The repository SHALL contain a fixture pair that validates against the format-1 schemas, whose lists are sorted as the contract requires, and whose `revision` matches the fixture snapshot bytes.

#### Scenario: Fixture conformance
- **WHEN** the contract tests run
- **THEN** both fixtures validate against their schema, every list is sorted, and the revision matches
