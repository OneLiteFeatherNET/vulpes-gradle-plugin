# Tasks

## 1. Build and publication

- [x] 1.1 Add `maven-publish` to `vulpes-format` and `vulpes-codegen` with the plugin's repositories, POM metadata and license; keep the plugin's publication unchanged
- [x] 1.2 Map the dependency configurations to POM scopes: `api` compile, `implementation` runtime; exclude test-fixtures and compile-only libraries from the POM
- [x] 1.3 Move the library versions (`vulpes-base`, `adventure-key`) from the plugin build to `vulpes-codegen`, which writes `versions.properties`; remove the plugin's `writeVersions`
- [x] 1.4 Verify with `publishToMavenLocal` that both POMs have the expected coordinates and scopes (see 5.1)

## 2. Facade

- [x] 2.1 Write the failing facade tests: `generate` returns the golden sources for `minestom` and `keys`; `write` creates files in package layout and nothing else; unknown template names are rejected naming the known ones; invalid package names are rejected; a `keys` release with items fails with `CodegenException`
- [x] 2.2 Add `VulpesCodegen`, `CodegenOptions`, `CodegenTemplate`, and move `GeneratedSource`, `Dependency` and `CodegenException` into the facade package
- [x] 2.3 Move the remaining classes of `vulpes-codegen` to `net.onelitefeather.vulpes.codegen.internal`, unchanged in behaviour; keep all golden and generator tests green
- [x] 2.4 Add the public-surface test that lists the public types and members of the facade package

## 3. Plugin

- [x] 3.1 Write the failing functional test: the plugin and the facade generate identical files from the same fixture release
- [x] 3.2 Switch `VulpesGenerateTask`, `VulpesTemplates` and `VulpesPlugin` to `VulpesCodegen`; keep the shaded bundling and the relocations

## 4. Documentation

- [x] 4.1 Add the README section "Using the code generator as a library": coordinates, the facade in a short example, the compatibility promise and the changelog rule for generated code

## 5. Verification

- [x] 5.1 Write the publication test (TestKit, `@TempDir`, `--offline`): publish to a file repository in the temp directory, parse both POMs and assert coordinates, scopes, resolved versions and the absence of test and compile-only dependencies; wire it into `check`
- [x] 5.2 Confirm that the plugin's shaded `vulpes-codegen` and the published library generate the same sources in the parity test (3.1)
- [x] 5.3 Run `./gradlew build` once under `flock ~/.cache/claude-heavy.lock`, and `publishToMavenLocal` to check the POMs by hand
- [x] 5.4 Open the code pull request `feat(codegen): publish the code generator as a library`
