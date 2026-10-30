# Proposal

**Conventional Commits:** `feat(codegen): generate typed access to content expressions`

## Why

Format 1 carries formula functions as text, but generated code ignores them, and the expressions designers store in Stelaris are not part of the format at all. A game therefore cannot use anything a designer calculated in the editor, except the signatures of interface functions.

## What Changes

- Format 1 gets the required top-level list `expressions`: `id`, `previousNames`, `name`, `description`, `variables` (`name`, `type`), `returnType` and `expression`, sorted by `name`. Names are unique across `expressions` and `expressionFunctions`. Format 1 was never delivered, so the format number stays 1.
- Template `minestom` generates `<Project>Formulas`, a class with one typed method per formula function and expression. It evaluates through the `ExpressionEngine` of `vulpes-base`. If the project has interface functions, its constructor takes the generated `<Project>ExpressionFunctions`, so formulas can call them.
- Renamed entries keep a deprecated method under their old name.
- `vulpesDiff` compares expressions: added or renamed with alias is minor, removed or a changed signature is major, a changed source is patch.
- The generated `vulpes-base` version moves to the release with `ExpressionEngine`.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `content-format`: Adds the expression list.
- `content-codegen`: Adds typed formula access.
- `content-diff`: Adds expressions to the comparison.

## Non-goals

- Template `keys` stays free of a runtime engine and generates no `<Project>Formulas`.
- No translation of formulas to Java: game and editor run the same EvalEx source.
- Test values and test stubs of the editor stay out of the format.

## Impact

- Depends on `vulpes-base` proposal `add-expression-engine`.
- The backend proposal `add-snapshot-format-1` has to deliver `expressions`, with an identifier-safe name and a return type.
- Data JARs without `expressions` no longer read; only test data exists so far.
