# Spec Delta

## ADDED Requirements

### Requirement: Stored expressions
`snapshot.json` SHALL contain the list `expressions`, sorted by `name`, whose entries hold `id`, `previousNames`, `name`, `description`, `variables`, `returnType` and `expression`. A name SHALL be unique across `expressions` and `expressionFunctions`.

#### Scenario: Expression is read
- **WHEN** a snapshot with one expression is read
- **THEN** the model contains its name, variables in declaration order, return type and source

#### Scenario: Name used twice
- **WHEN** an expression and an expression function have the same name
- **THEN** reading fails and the message names the name
