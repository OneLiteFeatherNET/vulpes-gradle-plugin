# Spec Delta

## ADDED Requirements

### Requirement: Base record in release.json
`release.json` MAY contain the object `base` with the string fields `repository`, `branch` and `commit`. `commit` SHALL be a full 40-character lowercase hexadecimal SHA-1. The record describes the vulpes-base source the content was built with; it is informational and optional. Absence of `base` SHALL be accepted. Unknown fields inside `base` SHALL be rejected like unknown fields anywhere else.

#### Scenario: Release with base
- **WHEN** a `release.json` contains `base` with repository `https://github.com/OneLiteFeatherNET/vulpes-base`, branch `main` and a full commit SHA
- **THEN** the release is read and the model exposes the three values

#### Scenario: Release without base
- **WHEN** a `release.json` has no `base` field
- **THEN** the release is read and the model exposes no base

#### Scenario: Unknown field in base
- **WHEN** `base` contains a field `tag`
- **THEN** reading fails and the message names `$.base.tag`

#### Scenario: Short commit
- **WHEN** `base.commit` is `0123abc`
- **THEN** reading fails and the message names `$.base.commit`
