# Spec Delta

## Purpose

Defines the data JAR of a Vulpes content release (format 1) and how a consumer reads it into a validated model.

## ADDED Requirements

### Requirement: Release version
`release.json` `version` SHALL equal the Maven version of the data JAR: `X.Y.Z` for a release and `X.Y.Z-SNAPSHOT` for a snapshot. The format-1 schema SHALL reject every other form.

#### Scenario: Snapshot version
- **WHEN** a `release.json` with `"version": "1.5.0-SNAPSHOT"` is validated against the format-1 schema
- **THEN** it validates

#### Scenario: Former preview version
- **WHEN** a `release.json` with `"version": "1.5.0-dev.3"` is validated against the format-1 schema
- **THEN** validation fails at `$.version`
