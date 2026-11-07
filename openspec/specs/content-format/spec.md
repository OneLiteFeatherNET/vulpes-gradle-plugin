# content-format Specification

## Purpose
Defines the data JAR of a Vulpes content release (format 1) and how a consumer reads it into a validated model.

## Requirements

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

### Requirement: Release version
`release.json` `version` SHALL equal the Maven version of the data JAR: `X.Y.Z` for a release and `X.Y.Z-SNAPSHOT` for a snapshot. The format-1 schema SHALL reject every other form.

#### Scenario: Snapshot version
- **WHEN** a `release.json` with `"version": "1.5.0-SNAPSHOT"` is validated against the format-1 schema
- **THEN** it validates

#### Scenario: Former preview version
- **WHEN** a `release.json` with `"version": "1.5.0-dev.3"` is validated against the format-1 schema
- **THEN** validation fails at `$.version`

### Requirement: Stored expressions
`snapshot.json` SHALL contain the list `expressions`, sorted by `name`, whose entries hold `id`, `previousNames`, `name`, `description`, `variables`, `returnType` and `expression`. A name SHALL be unique across `expressions` and `expressionFunctions`.

#### Scenario: Expression is read
- **WHEN** a snapshot with one expression is read
- **THEN** the model contains its name, variables in declaration order, return type and source

#### Scenario: Name used twice
- **WHEN** an expression and an expression function have the same name
- **THEN** reading fails and the message names the name

### Requirement: Format 2 with optional base record
`release.json` format 2 is format 1 plus the optional object `base` with the string fields `repository`, `branch` and `commit`. `commit` SHALL be a full 40-character lowercase hexadecimal SHA-1. The record is informational. Format 2 MAY omit `base`. Unknown fields inside `base` SHALL be rejected.

#### Scenario: Format 2 with base
- **WHEN** a `release.json` in format 2 contains `base` with repository `https://github.com/OneLiteFeatherNET/vulpes-base`, branch `main` and a full commit SHA
- **THEN** the release is read and the model exposes the three values

#### Scenario: Format 2 without base
- **WHEN** a `release.json` in format 2 has no `base` field
- **THEN** the release is read and the model exposes no base

#### Scenario: Format 1 without base
- **WHEN** a `release.json` in format 1 has no `base` field
- **THEN** the release is read and the model exposes no base

#### Scenario: Base in format 1
- **WHEN** a `release.json` in format 1 contains `base`
- **THEN** reading fails and the message names `$.base`

#### Scenario: Unknown field in base
- **WHEN** `base` in a format-2 `release.json` contains a field `tag`
- **THEN** reading fails and the message names `$.base.tag`

#### Scenario: Short commit
- **WHEN** `base.commit` is `0123abc`
- **THEN** reading fails and the message names `$.base.commit`

### Requirement: Supported release formats
The reader SHALL accept `release.json` in format 1 and format 2 and SHALL reject format 1 files that contain `base`, because format 1 stays strict. Any other format number SHALL be rejected with a message that names the supported formats.

#### Scenario: Unsupported format
- **WHEN** a `release.json` declares format 3
- **THEN** reading fails and the message names the supported formats 1 and 2
