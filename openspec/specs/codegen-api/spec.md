# codegen-api Specification

## Purpose
TBD - created by archiving change publish-codegen-library. Update Purpose after archive.

## Requirements

### Requirement: Generation entry point
`vulpes-codegen` SHALL offer one public entry point that takes a content release, as read by `vulpes-format`, and options, and returns the generated Java sources. The same entry point SHALL write the sources to a directory in their package layout.

#### Scenario: Generate sources in memory
- **WHEN** a content release and options with package `net.example.content` and template `minestom` are passed
- **THEN** the sources of the release are returned, each with its package and class name

#### Scenario: Write sources to a directory
- **WHEN** the same release is written to an empty directory
- **THEN** each source is written to its path below the package directory, and no other file is created

### Requirement: Options
The options SHALL name the package of the generated classes and one template, `minestom` or `keys`. An invalid package name or an unknown template name SHALL be rejected before any source is generated, and the rejection SHALL name the invalid value and, for a template, the known names.

#### Scenario: Unknown template
- **WHEN** the template `fancy` is given
- **THEN** the call fails and names `fancy` and the known templates `keys` and `minestom`

#### Scenario: Invalid package
- **WHEN** the package name is `net..example`
- **THEN** the call fails and names the package

### Requirement: Output equals the plugin output
For the same content release, package and template, the sources returned by the entry point SHALL be identical to the sources the Vulpes plugin generates.

#### Scenario: Parity with the plugin
- **WHEN** the plugin and the library generate from the same release with the same package and template
- **THEN** every source file has the same name and the same bytes

### Requirement: Failures
A release that cannot be generated, for example because it uses a type the template does not support, SHALL fail with `CodegenException`, whose message names the project.

#### Scenario: Unsupported entity for keys
- **WHEN** a release with item entries is generated with template `keys`
- **THEN** the call fails with `CodegenException` and names the project

### Requirement: Compatibility promise
Only the entry point, its options, its template names, `GeneratedSource`, `Dependency` and `CodegenException`, and the `vulpes-format` types in their signatures, are covered by semantic versioning. Everything else in `vulpes-codegen` is internal and MAY change in any release. A breaking change of a covered type SHALL be announced with `!` in its Conventional Commit and a `BREAKING CHANGE:` footer. Generated code MAY change between releases; the change SHALL be listed in the changelog.

#### Scenario: Internal class changes
- **WHEN** an internal class of `vulpes-codegen` is renamed in a release
- **THEN** the release is not a breaking change of the library

#### Scenario: Entry point signature changes
- **WHEN** a parameter of the entry point is removed
- **THEN** the change is marked as breaking in the changelog

### Requirement: Generated code is listed in the changelog
When a release changes the sources that the library generates for an existing template, the changelog SHALL name the template.

#### Scenario: Generated output changes
- **WHEN** a release changes the generated code of the `minestom` template
- **THEN** the changelog entry names `minestom`
