# Spec Delta

## ADDED Requirements

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
