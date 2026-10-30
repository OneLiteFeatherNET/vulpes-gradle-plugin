# Tasks

## 1. Module and shared writer

- [ ] 1.1 Add the module `vulpes-codegen` with `com.palantir.javapoet`; move `JavaNames` and `ReleaseClassWriter` there unchanged
- [ ] 1.2 Port `JavaIdentifiers` with collision claims; write its tests first

## 2. Generators (test first, one type at a time)

- [ ] 2.1 Keys and aliases for every entity type
- [ ] 2.2 Items (material, amount, raw components)
- [ ] 2.3 Attributes
- [ ] 2.4 Sounds
- [ ] 2.5 Font symbols
- [ ] 2.6 Advancements
- [ ] 2.7 Expression function interface (port of the backend `ExpressionInterfaceGenerator`)
- [ ] 2.8 Resource pack infos and the content object

## 3. Templates

- [ ] 3.1 `Template` with `minestom` and `keys`, supported entity types and runtime dependencies
- [ ] 3.2 Compile tests against `vulpes-base` and `adventure-key`; golden files for the fixture snapshot

## 4. Plugin wiring

- [ ] 4.1 Write the failing TestKit tests: default template, unknown template, template change, runtime dependency per template, unsupported entity type
- [ ] 4.2 Add `vulpes.template`, pass it to `vulpesGenerate` and add the runtime dependency lazily
- [ ] 4.3 Relocate JavaPoet inside the plugin JAR

## 5. Verification and pull request

- [ ] 5.1 Run `./gradlew build`
- [ ] 5.2 Open the pull request titled `feat(codegen): generate content code from vulpes data jars`
