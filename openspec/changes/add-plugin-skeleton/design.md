# Design

## Context

Today `vulpes-generator` reads the database and writes Java sources into a clone of `vulpes-base`. The design document replaces that with an immutable data JAR per release and moves code generation into a Gradle plugin that runs in the game's build. The contract was agreed between the plugin and generator sessions and approved on 2026-10-29.

## Contract (format 1)

The normative definition is the pair of JSON Schemas in `contract/`; `contract/fixtures/` holds a valid example pair whose `revision` is the real hash of the fixture snapshot.

- **Serialization.** UTF-8, sorted object keys, no insignificant whitespace. Every list is sorted (entities by `key`, expression functions by `name`, assets by `path`, packs by `id`, sound entries by `name`). Every field is present; absent values are an explicit `null`. Defaults are resolved by the backend; consumers never invent values.
- **`snapshot.json`.** `format`, `projectId`, `namespace`, the entity lists `items`, `attributes`, `fonts`, `sounds`, `advancements`, `expressionFunctions`, plus `assets` and `pack`, which only the generator uses. Each entity carries a stable `id` (UUID) and `previousKeys` (keys it had earlier in the current major version), so renames can be detected and deprecated aliases generated. Rename rule (decided 2026-10-29): a rename that keeps the old key in `previousKeys` is a minor change; the next major version empties `previousKeys` and thereby removes the aliases. A removed key is a major change. References to client resources are Minecraft resource locations; `assets[]` maps them to pack paths with SHA-1 and source resolution.
- **`release.json`.** `format`, `project` (= artifactId), `projectId`, `version` (`X.Y.Z` or `X.Y.Z-dev.N`), `revision` (`sha256:` + SHA-256 of the bytes of `snapshot.json`), `packs[]` (`id`, `resolution`, `url`, `sha1`, `size`) and `provenance` (generator version, creator, freeze time).
- **Rules beyond JSON Schema.** Keys, ids, asset paths, pack ids and function names are unique; every resource location an entity uses has an `assets[]` entry (except sound entries of type `event`). The backend enforces these before freezing; the plugin checks uniqueness of what it turns into identifiers.

### Versioning the contract

Both files carry `format`. A reader rejects any value it does not know and names the formats it supports. Additive changes still bump the format, because consumers validate strictly (`additionalProperties: false`). The plugin version declares which formats it reads.

## Decisions

### Two modules
`vulpes-format` holds the model and the reader without Gradle types. It is unit-tested in milliseconds and can be reused by `vulpes-runtime`, which reads the same format. `vulpes-gradle-plugin` holds only Gradle wiring. Code generation (`port-content-codegen`) will add a third module, `vulpes-codegen`, between them.

### Reading
The reader opens the JAR as a zip, reads both entries as bytes, hashes the snapshot bytes before parsing, then parses with Jackson into records. Unknown properties fail (strict), so a newer format cannot be half-read. Errors name the JAR, the file and the JSON path.

Alternative considered: validate against the JSON Schema at build time. Rejected for the plugin runtime (extra dependency, slower) but used in tests, which validate the fixtures against the schemas and check list ordering.

### Dependency scope and resolution
`configurations.dependencyScope("vulpes")` plus a resolvable `vulpesData` that extends it, non-transitive, with a `Usage`/`Category` attribute set so a data JAR is never mistaken for a library. The task receives the files via `ArtifactCollection` so it knows each artifact's module coordinates; it fails if `release.json.project` differs from the artifactId.

### Packages
Default package per content project is `net.onelitefeather.vulpes.content.<project>` (the project name sanitized to a Java identifier). `vulpes { packageName = "…" }` sets it when exactly one content project is declared; with several, the build fails and asks for `vulpes { project("cygnus") { packageName = "…" } }`. This keeps the example from the design document working and makes the multi-project case explicit.

### Task
`vulpesGenerate` is `@CacheableTask`. Inputs: the data JARs as `@Classpath` (content hashing, timestamps ignored), the package mapping as `@Input`, the plugin version as `@Input`. Output: `build/generated/sources/vulpes/java/main` as `@OutputDirectory`, deleted at the start of every run so removed content disappears. The directory is added with `sourceSets.main.java.srcDir(task.flatMap { it.outputDirectory })`, which also wires the task before `compileJava` and makes IDEs pick it up. No `Project` access at execution time.

### Release constants
Per content project one final class `<Project>Release` (e.g. `CygnusRelease`) with `PROJECT`, `VERSION`, `REVISION`, a nested record `Pack(String id, int resolution, String url, String sha1, long size)`, one constant per pack named after its id (`PACK_16X`) and `PACKS` as an unmodifiable list. `provenance.createdAt` is not emitted, so re-packaging the same content does not change the code. The class has no dependency on Adventure or Minestom; templates build `ResourcePackInfo` from it later.

## Testing (F.I.R.S.T.)

- `vulpes-format` unit tests build JARs in memory or in `@TempDir`: reads the fixture pair; rejects unknown `format` in each file; rejects a revision mismatch; rejects unknown properties; reports missing entries. A contract test validates the fixtures against the schemas and asserts that every list in the fixtures is sorted.
- Plugin functional tests use Gradle TestKit with a `file://` Maven repository in `@TempDir` that contains a generated data JAR: generated class compiles; second run is up to date; configuration cache is reused; result comes from the build cache after `clean`; unknown format fails with a clear message; several content projects without explicit packages fail. These are integration tests: slower, but independent and repeatable.

## Risks

- **Backend lag.** Format 1 needs backend work. Until then fixtures are the only source; the plugin is usable as soon as the generator publishes a format-1 JAR.
- **Jackson on the build classpath.** Mitigated by relocation in the plugin JAR.
