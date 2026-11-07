# Spec Delta

## ADDED Requirements

### Requirement: Template output
`vulpesGenerate` SHALL write the complete output of the selected template for every content project, including the release constants, and SHALL treat the template name as a task input.

#### Scenario: Template change
- **WHEN** `vulpes.template` changes from `minestom` to `keys` between two runs
- **THEN** the second run is not up to date and the output contains only the files of the `keys` template
