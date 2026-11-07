# Design

## Context

See proposal.md, Why.

- `vulpes-format` and `vulpes-codegen` have no publication today. Only `vulpes-gradle-plugin` applies `maven-publish`; the root build sets one version (`0.2.0`, `x-release-please-version`) for all subprojects, and `release-please-config.json` has one package (`.`), so all modules are released together.
- The release workflow runs the root `publish` task, so the new publications are released with the plugin.
- `vulpes-codegen` depends on `vulpes-format` with `api`, uses JavaPoet and EvalEx as `implementation`, and tests against vulpes-base and Minestom. Its public surface is wider than the facade: `Templates`, `Template`, `Dependency`, `EntityType`, `GeneratedSource`, `CodegenException` are public, while the generators (`ContentGenerator`, `KeysGenerator`, ...) are package-private.
- The plugin bundles `vulpes-codegen` through the `bundled` configuration into its shadow JAR and relocates Jackson, JavaPoet and EvalEx. Its POM lists no runtime dependencies.
- The plugin reads `Template.runtimeDependencies()` to add the runtime library of the selected template to a game's `implementation` configuration. The versions come from `versions.properties`, written by the plugin's build from the version catalog.

## Goals / Non-Goals

Goals: one entry point; artifacts that a generator service can depend on without copying code; no change to the plugin's users.

Non-goals: a second generator; a separate version line; changes to generated code; publishing test fixtures; a stable API beyond the facade.

## Decisions

### Plugin keeps shading vulpes-codegen
The plugin continues to bundle its own relocated copy of `vulpes-codegen` and does not depend on the published artifact.
- The plugin's published POM stays free of runtime dependencies, so a game's dependency graph does not change.
- Gradle loads plugins next to Gradle's own libraries. A normal dependency would put unrelocated JavaPoet, EvalEx and Jackson on that classpath, which is the clash the existing relocations prevent.
- The bundled copy is built from the same commit as the published one, so both produce the same output; a plugin test checks this (tasks 5.2).
Alternative considered: the plugin depends on `vulpes-codegen` as a normal dependency. Rejected: it adds transitive dependencies to the plugin's classpath and to its POM for no gain.

### One public entry point, internal rest
`vulpes-codegen` gets `net.onelitefeather.vulpes.codegen.VulpesCodegen` with static methods:
- `generate(ContentRelease content, CodegenOptions options)` returns `List<GeneratedSource>`;
- `write(ContentRelease content, CodegenOptions options, Path directory)` writes the sources below `directory` in their package layout. It creates files and does not delete anything; the caller clears the directory.
- `runtimeDependencies(CodegenTemplate template)` returns `List<Dependency>`, used by the plugin.

`CodegenOptions` is a record `(String packageName, CodegenTemplate template)`. `CodegenTemplate` is an enum with `MINESTOM` and `KEYS` and `fromName(String)`, which maps `minestom` and `keys` and rejects other names with the known ones listed.

`GeneratedSource`, `Dependency` and `CodegenException` move into the same public package because they appear in the signatures. Everything else moves to `net.onelitefeather.vulpes.codegen.internal` (Templates, Template, EntityType, the generators, the writers). Internal types stay `public` inside the internal package only where the facade needs them; that is not API.
Alternative considered: keep all classes in one package and document the public ones. Rejected: a package boundary is the only thing a reader of the sources can rely on.

### Versions owned by vulpes-codegen
The library versions for `runtimeDependencies` (`vulpes-base`, `adventure-key`) move from the plugin's build into `vulpes-codegen`'s build, which writes `versions.properties` as the plugin does today. The generator service does not use runtime dependencies; the plugin reads them through the facade. Both values come from the same version catalog, so the plugin and the library agree.
Alternative considered: pass the versions in `CodegenOptions`. Rejected: the generator service would have to supply values it never uses.

### Coordinates and POM
- Group `net.onelitefeather.vulpes`, artifacts `vulpes-format` and `vulpes-codegen`, version from the root build.
- Both modules apply `maven-publish` with the same repositories as the plugin (`onelitefeather-releases` or `-snapshots` by version, credentials from `ONELITEFEATHER_MAVEN_USERNAME`/`ONELITEFEATHER_MAVEN_PASSWORD`) and POM metadata (name, description, URL, the AGPL-3.0 license of the repository, SCM).
- Dependencies: `api` becomes POM compile scope, `implementation` becomes runtime scope, `compileOnly` and `test*` are not published. The resolved versions are written by Gradle into the POM.
- Test fixtures of `vulpes-format` (`java-test-fixtures`) are not published as an artifact. The code PR checks that the POM and module metadata contain no test-fixtures entry.
- The publication setup is shared by the two modules in the root build; the plugin keeps its own block.

### Test strategy
- Unit tests of the facade: fixtures from `vulpes-format` test fixtures, no file system outside `@TempDir`, no network, no clock.
- Golden tests stay unchanged; the facade output for `minestom` and `keys` is compared with the same golden files.
- Publication check (wired into `check`): an integration test (Gradle TestKit, `@TempDir`) runs `publishAllPublicationsToTestRepository` against a file repository in the temp directory, with `--offline` so that it uses only the cached dependencies, then parses both POMs and asserts coordinates, scopes and versions. It needs no credentials and no network.
- Plugin parity test: the functional test generates from the fixture data with the plugin and with the facade and compares the files.
- Public-surface test: lists the public types and members of the facade package; adding a public member fails the test until the list is updated on purpose.

### Compatibility
Covered by the promise in `specs/codegen-api`: the facade, its options and template names, `GeneratedSource`, `Dependency`, `CodegenException`, and the `vulpes-format` types in their signatures. The version line is shared with the plugin, so a breaking facade change makes the next plugin release breaking as well. This is accepted because the library and the plugin must be released together. Generated code is not covered by semantic versioning; changes to it are listed in the changelog by template.

## Risks / Trade-offs

- [The shaded copy in the plugin and the published library drift] → Same build, same commit; plugin parity test (task 5.2).
- [A breaking facade change forces a plugin major release] → Keep the facade small; the open question below asks whether separate version lines are wanted later.
- [Moving classes to `internal` makes the diff large] → The move is mechanical and comes in its own commit inside the code PR; no logic changes.
- [A wrong POM reaches the repository] → The publication test runs in `check` before a release can be cut.
- [Gradle publishes the test fixtures variant with `maven-publish`] → Excluded and checked, see above.

## Migration Plan

1. Merge this proposal. The code PR `feat(codegen): publish the code generator as a library` follows, branched from main after the proposal merge.
2. The code PR is merged. release-please opens the release PR; the release publishes plugin, `vulpes-format` and `vulpes-codegen` with the same version.
3. The generator service adds the library dependency at that released version (its own change).
Rollback: a bad POM is fixed by a patch release. The plugin keeps working with the bundled copy whatever happens to the published artifacts.

## Open Questions

1. Should `vulpes-codegen` get its own version line later, so that a facade change does not force a plugin major release? This change keeps one line.
2. How does the generator service consume a codegen version that is not released yet? The publish workflow only publishes on a release; snapshots are published only when someone runs the task by hand. This change does not add snapshot publication on pull requests.
