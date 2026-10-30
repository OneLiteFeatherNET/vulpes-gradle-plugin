# Spec Delta

## ADDED Requirements

### Requirement: Typed formula access
Template `minestom` SHALL generate `<Project>Formulas` with one method per formula function and expression, mapping `number` to `BigDecimal`, `boolean` to `boolean` and `string` to `String`. No class SHALL be generated when the project has neither.

#### Scenario: Formula function
- **WHEN** the snapshot has the formula `armorDamage(base: number, armor: number): number`
- **THEN** `CygnusFormulas` has the method `BigDecimal armorDamage(BigDecimal base, BigDecimal armor)` and evaluating it returns the result of the formula

#### Scenario: Interface function used by a formula
- **WHEN** the project has interface functions
- **THEN** the constructor of `CygnusFormulas` takes `CygnusExpressionFunctions` and formulas can call its methods

#### Scenario: Renamed expression
- **WHEN** an expression lists `oldName` in `previousNames`
- **THEN** a deprecated method `oldName` calls the current method

#### Scenario: Keys template
- **WHEN** template `keys` is used
- **THEN** no `CygnusFormulas` is generated
