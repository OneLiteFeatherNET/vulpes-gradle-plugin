# Proposal

**Conventional Commits:** `feat(plugin): read vulpes content releases in the build`

## Why

A Vulpes content release is published as a data JAR `net.onelitefeather.vulpes.content:<project>:<version>` in Reposilite. It holds only `META-INF/vulpes/snapshot.json` and `META-INF/vulpes/release.json` (see the design document "2026-10-28 — Vulpes: Content-Releases ohne Git (Design)", sections "Ein Release, ein Daten-JAR, drei Abnehmer" and "6. Gradle-Plugin"). All Java code generation moves out of `vulpes-generator` into this Gradle plugin, so that generated code lives in the game's own build, Gradle handles versions, caching and verification, and Renovate updates content like any other dependency.

Three parties read or write the data JAR: the backend freezes the snapshot, the generator packages it, and the plugin consumes it. They need one written contract for both files, and the plugin needs a foundation that resolves the JAR, checks it against that contract and runs before `compileJava`. This change builds that foundation. Code generation for the entity types follows in `port-content-codegen`.

## What Changes

- **Contract, format 1.** JSON Schemas for `snapshot.json` and `release.json` become part of this repository (`contract/` in this change, later shipped in the `vulpes-format` module), with a fixture pair that validates against them. The plugin repository is the single home of the contract; backend and generator link to it and do not keep copies.
- **`vulpes-format` module.** Plain Java library that reads a data JAR into an immutable model, rejects unknown `format` values of either file, and checks that `revision` equals the SHA-256 of the bytes of `snapshot.json`. It has no Gradle dependency, so `vulpes-runtime` can reuse it later.
- **Plugin `net.onelitefeather.vulpes`.**
  - Dependency scope `vulpes` for data JARs; resolved non-transitively.
  - Extension `vulpes { packageName }` with a per-project override for builds that consume several content projects.
  - Task `vulpesGenerate` that reads every data JAR, writes into `build/generated/sources/vulpes/java/main` (cleared on every run), is registered as a source directory of the `main` source set and therefore runs before `compileJava`. It is cacheable and compatible with the configuration cache.
  - For each content project it generates a `<Project>Release` class with the project name, version, revision and one constant per resource pack (id, resolution, URL, SHA-1, size). Entity code generation is not part of this change.
- **Build.** Multi-project Gradle build (`vulpes-format`, `vulpes-gradle-plugin`), Java 17 bytecode for the plugin, publishing to the OneLiteFeather Reposilite as `net.onelitefeather.vulpes:vulpes-gradle-plugin` with the plugin marker.

## Capabilities

### New Capabilities
- `content-format`: The data JAR contract (format 1) and reading it into a validated model.
- `content-generation-task`: Plugin, `vulpes` dependency scope, extension and the `vulpesGenerate` task wiring including release constants.

### Modified Capabilities
<!-- None: new repository. -->

## Non-goals

- No entity code (items, attributes, fonts, sounds, advancements, expressions) and no templates. They follow in `port-content-codegen`, together with the inversion of `vulpes-base`.
- No `vulpesDiff`; it follows in `add-vulpes-diff`.
- No resource pack building and no network access from the plugin; the JAR comes from Gradle dependency resolution.
- No CI or release-please setup; that ships separately as `ci`/`build` changes without an OpenSpec proposal.
- No publishing to the Gradle Plugin Portal.

## Impact

- **Backend** (separate change there): the snapshot must follow format 1, including the new entity types, `id`/`previousKeys`, `amount`, lore as text components, the `pack` and `assets` blocks, and `revision` over the exact bytes.
- **Generator** (separate change there): writes `release.json` format 1 and packages both files into the data JAR.
- **Dependencies**: Jackson for JSON in `vulpes-format`, relocated inside the plugin JAR to avoid build classpath conflicts. JavaPoet is introduced with `port-content-codegen`, not here.
