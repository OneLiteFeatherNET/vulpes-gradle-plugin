# Proposal

**Conventional Commits:** `fix(plugin): expose the template runtime library as api`

## Why

A game is often split into modules. One module, `content`, applies the plugin and generates the code from the content projects. Another module, `game`, depends on `content` and uses the generated classes. Today the plugin adds the runtime library of the selected template (`vulpes-base` for `minestom`, `adventure-key` for `keys`) to the `implementation` configuration. That is correct for the module itself, but not for its consumers: the generated classes expose these types in their public API. `CygnusItems.PAGE_1` is a `CustomItem`, `CygnusContent` implements `VulpesContent`, and the key constants are `Key` values.

A developer who builds such a split game sees `:game:compileJava` fail with "package net.onelitefeather.vulpes does not exist" and "class file for net.onelitefeather.vulpes.item.CustomItem not found". The `content` module compiles, the game module does not, and the fix is one hand-written `api("net.onelitefeather.vulpes:vulpes-base:<version>")` line that the plugin should have written itself.

## What Changes

- When the project applies `java-library`, the plugin adds the runtime library of the selected template to the `api` configuration instead of `implementation`. Consumers of the module then compile against the generated types without declaring the library themselves.
- When only `java` is applied, there is no `api` configuration and the library stays in `implementation`, as today.
- The version stays the one the plugin was built for, for both templates.
- The change applies to both templates: `minestom` (`vulpes-base`) and `keys` (`adventure-key`).
- The README sentence that says vulpes-base is added "as a runtime dependency" is updated, and a short note explains multi-module builds.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `content-templates`: the runtime library of the selected template goes to `api` when the project applies `java-library`, and to `implementation` otherwise.

## Decisions

- **`api` only when `java-library` is applied.** The `api` configuration exists only with `java-library`. Plain `java` projects have no consumers that compile against the module through `api`, so they keep `implementation`. A project that applies neither plugin already fails in the plugin's own check.
- **Non-breaking.** For the module itself, `api` is a superset of `implementation`: the library is still on its compile and runtime classpath. Only consumers gain the library on their compile classpath, which makes a previously failing build compile.
- **Minestom and Adventure stay the game's responsibility.** `vulpes-base` does not bring Minestom or the Adventure API. The game keeps providing them, for example with `compileOnly`. This change does not add them to `api`, because they are not part of the plugin's dependency.

## Non-goals

- No change to the version of the runtime library, and no new DSL for the configuration.
- No change to the generated code.
- No change to the Minestom or Adventure dependencies, which stay the game's responsibility.
- No change for projects that apply only `java`.

## Risks

- A consumer that already declares `vulpes-base` itself sees the library on both `api` and its own declaration. Gradle resolves the same coordinate once, so this is not a conflict. Mitigation: the functional test covers a consumer with and without its own declaration.
- A project that applies `java-library` and wants the library out of its public API cannot opt out. Mitigation: this is accepted, because the generated types are part of the public API of the module by design.

## Impact

- `vulpes-gradle-plugin`: the runtime dependency is added to `api` when `java-library` is applied, in `VulpesPlugin`; functional test with a library module and a consumer module.
- README: the sentence about the runtime dependency and a note on multi-module builds.
- No change to `vulpes-format`, `vulpes-codegen` or the template runtime.
- Depends on the `content-templates` requirement "Runtime dependencies" as it is in `openspec/specs/` on main.
