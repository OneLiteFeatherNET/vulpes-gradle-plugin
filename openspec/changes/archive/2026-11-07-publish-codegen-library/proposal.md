# Proposal

**Conventional Commits:** `feat(codegen): publish the code generator as a library`

## Why

The Vulpes generator service (`vulpes-generator`, proposal `add-editor-release-flow`) must produce exactly the code the Gradle plugin produces, so that a release can be compiled against a chosen vulpes-base branch before it is published. Today the generator would have to copy the plugin's code or shell out to the plugin. Both leave two generators that can drift apart, and the plugin's generator is only reachable inside a Gradle build. Publishing the generator as a Maven library gives the service the same code, with the same version, that the plugin ships.

## What Changes

- `net.onelitefeather.vulpes:vulpes-format` and `net.onelitefeather.vulpes:vulpes-codegen` are published as Maven artifacts with the plugin's version, to the same repositories and by the same release workflow.
- Each published POM lists its dependencies with scopes: `vulpes-codegen` lists `vulpes-format` as a compile dependency and its own libraries (JavaPoet, EvalEx, Jackson through `vulpes-format`) as runtime dependencies. No test or compile-only library appears.
- `vulpes-codegen` gets one public entry point, `VulpesCodegen`, that takes a parsed content release from `vulpes-format` and options (package name, template `minestom` or `keys`). It returns the generated sources, or writes them to a directory.
- Every other class of `vulpes-codegen` moves to an internal package and is not part of the library's contract.
- The compatibility promise covers only the entry point and the types in its signatures. Semantic versioning rules are written down in the README and in the spec.
- The plugin keeps working for users without change: its published POM stays free of runtime dependencies, and it keeps bundling a relocated copy of `vulpes-codegen`.
- The README gets a section "Using the code generator as a library".

## Capabilities

### New Capabilities
- `codegen-publication`: Which Maven artifacts the code generator is published as, their coordinates, versions, POM dependencies and the rule that they are released with the plugin.
- `codegen-api`: The public entry point of the generator library, its options, its output and its compatibility promise.

### Modified Capabilities
<!-- None. The plugin's behaviour does not change. -->

## Non-goals

- No change to generated code. Its output stays byte-identical to the plugin's output for the same release (golden tests stay unchanged).
- No separate version line for the library; it shares the plugin's version (see design.md).
- No publication of the test fixtures of `vulpes-format`.
- No changes to `vulpes-generator`; its adoption is a separate change there.

## Impact

- `vulpes-codegen` and `vulpes-format`: `maven-publish`, POM metadata, the entry point and the internal package move.
- `vulpes-gradle-plugin`: calls the entry point instead of the internal templates; its bundling of `vulpes-codegen` stays as it is.
- Root build: the publication setup is shared by the two library modules; the version line is unchanged.
- The release workflow already runs the root `publish` task, so the new artifacts are published with the plugin without workflow changes.
- README: new section; the plugin's usage section stays as it is.
