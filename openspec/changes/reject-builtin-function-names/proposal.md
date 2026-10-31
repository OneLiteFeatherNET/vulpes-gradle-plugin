# Proposal

**Conventional Commits:** `feat(codegen): reject project functions named like built-in ones`

## Why

A project function named like a built-in EvalEx function, for example `if` or `max`, silently replaces it, and every formula that uses the built-in then calculates something else. Nobody sees this: no error appears in the build, at startup or at runtime, only wrong numbers.

## What Changes

- `vulpesGenerate` fails when an expression function or an expression has the name of a built-in EvalEx function, ignoring case, and the message names the function.
- The list of built-in names comes from the EvalEx version `vulpes-base` uses, so it does not have to be kept by hand.
- Template `keys` is checked as well, because the content is equally wrong there.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `content-codegen`: Adds the name check.

## Non-goals

- `ExpressionEngine` in `vulpes-base` stays as it is. Games get the error at build time.
- The editor keeps its own validation; the backend can reject such names when they are saved.

## Impact

- `vulpes-codegen` depends on EvalEx, bundled and relocated in the plugin JAR.
- A project that already uses such a name fails its build until the function is renamed; no real content exists yet.
