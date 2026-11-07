# Spec Delta

## ADDED Requirements

### Requirement: Built-in function names
Code generation SHALL fail when an expression function or an expression has the name of a built-in EvalEx function, ignoring case. The message SHALL name the function.

#### Scenario: Function named like a built-in
- **WHEN** the snapshot has an expression function named `max`
- **THEN** code generation fails and the message names `max`

#### Scenario: Different case
- **WHEN** the snapshot has an expression named `IF`
- **THEN** code generation fails and the message names `IF`

#### Scenario: Other name
- **WHEN** no name matches a built-in function
- **THEN** code generation succeeds
