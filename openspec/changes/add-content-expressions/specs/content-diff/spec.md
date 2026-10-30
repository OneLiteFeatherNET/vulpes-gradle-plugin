# Spec Delta

## ADDED Requirements

### Requirement: Expression comparison
`vulpesDiff` SHALL match expressions by `id` and report them: added or renamed with the old name in `previousNames` requires minor, removed or a changed signature requires major, a changed source requires patch.

#### Scenario: Removed expression
- **WHEN** an expression of the old release is missing in the new one
- **THEN** the report lists it and requires a major version

#### Scenario: Changed source
- **WHEN** only the source of an expression changed
- **THEN** the report lists it and requires a patch version
