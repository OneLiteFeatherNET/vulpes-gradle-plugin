# Tasks

## 1. Build

- [ ] 1.1 Set up the Gradle build (wrapper 9.8.2, Kotlin DSL, version catalog) with the modules `vulpes-format` and `vulpes-gradle-plugin`; plugin bytecode targets Java 17
- [ ] 1.2 Configure publishing of `net.onelitefeather.vulpes:vulpes-gradle-plugin` and the plugin marker to the OneLiteFeather Reposilite

## 2. Contract and reader (`vulpes-format`)

- [ ] 2.1 Move the schemas and fixtures from `contract/` into `vulpes-format` resources
- [ ] 2.2 Write the failing contract tests: fixtures validate against the schemas, every list is sorted, the revision matches
- [ ] 2.3 Write the failing reader tests: valid JAR, missing entry, unknown format in either file, revision mismatch, unknown property with JSON path
- [ ] 2.4 Implement the records and the strict reader until the tests pass

## 3. Plugin (`vulpes-gradle-plugin`)

- [ ] 3.1 Write the failing TestKit tests: generated class compiles, up to date, configuration cache reused, build cache hit after `clean`, unknown format fails, wrong project name fails, ambiguous package fails, removed packs disappear
- [ ] 3.2 Add the `vulpes` dependency scope, the resolvable configuration and the extension with per-project packages
- [ ] 3.3 Implement `vulpesGenerate` with the `<Project>Release` class and wire it into the `main` source set
- [ ] 3.4 Relocate Jackson inside the plugin JAR

## 4. Verification and pull request

- [ ] 4.1 Run `./gradlew build` with tests
- [ ] 4.2 Open the pull request titled `feat(plugin): read vulpes content releases in the build`
