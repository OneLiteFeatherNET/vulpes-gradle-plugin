# Proposal

**Conventional Commits:** `feat(codegen): generate content code from vulpes data jars`

## Why

With `add-plugin-skeleton` the plugin reads data JARs and generates release constants, but the actual content code still comes from `vulpes-generator`, which reads the database and writes into a clone of `vulpes-base`. The design document ("2026-10-28 — Vulpes: Content-Releases ohne Git (Design)", sections "6. Gradle-Plugin" and "Templates") moves all code generation into the plugin: items, sounds, fonts, attributes, advancements and the expression function interface. `vulpes-generator` deletes its code generation once it lives here.

## What Changes

- New module `vulpes-codegen` (plain Java, no Gradle types) that turns a `ContentRelease` into Java sources with JavaPoet (`com.palantir.javapoet`, which supports records and sealed types). It ports the generators of `vulpes-generator` (`ItemGenerator`, `AttributeGenerator`, `SoundGenerator`, `FontGenerator`, `AdvancementGenerator`, `JavaIdentifiers`) and the expression interface generator of the backend proof of concept (`ExpressionInterfaceGenerator`).
- Identifiers come from the namespaced keys; two keys that map to the same constant name fail the build and name both keys.
- Renamed entries: for every key in `previousKeys` a `@Deprecated(forRemoval = true)` alias constant is generated, and the content object lists the alias keys.
- **Templates** as generator profiles, selected with `vulpes { template = "minestom" }` (default):
  - `minestom`: full code against `net.onelitefeather.vulpes:vulpes-base` (keys, constant entries, `<Project>Content implements VulpesContent`, resource pack infos, expression interface). The plugin adds the `vulpes-base` version it was built for to `implementation`.
  - `keys`: key constants, release constants and the expression interface only; the plugin adds `net.kyori:adventure-key`.
  - A template declares the entity types it supports; content with entities of an unsupported type fails with a clear message instead of producing partial code.
- `vulpesGenerate` delegates to the selected template; the `<Project>Release` class from `add-plugin-skeleton` moves into `vulpes-codegen` unchanged.

## Capabilities

### New Capabilities
- `content-codegen`: Java code for every entity type of the content contract (format 1).
- `content-templates`: Template selection, template dependencies and support checks.

### Modified Capabilities
- `content-generation-task`: `vulpesGenerate` generates the full template output instead of the release constants only.

## Non-goals

- No validation of item components against Minecraft codecs at build time; component JSON passes through to `ItemComponents.apply` at runtime. The backend validates before a release is frozen.
- No `sounds.json` or font JSON; those are pack content and stay in the generator.
- No Paper template; it can be added as another profile later.

## Impact

- **Depends on** `add-plugin-skeleton` and on `vulpes-base` with `invert-content-registration` and `publish-as-vulpes-base`.
- **vulpes-generator** removes its code generation after this change is merged (change `remove-git-and-codegen` there).
- **Dependencies**: `com.palantir.javapoet` (relocated inside the plugin JAR like Jackson).
