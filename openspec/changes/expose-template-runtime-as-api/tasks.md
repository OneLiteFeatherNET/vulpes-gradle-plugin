# Tasks

## 1. Failing functional test

- [ ] 1.1 Write the failing functional test (TestKit, multi-project, `@TempDir`, no network): a library module applies the plugin with `java-library` and the `minestom` template, a consumer module depends on it with `implementation(project(":content"))` and uses a generated entry type without declaring `vulpes-base`; the consumer's `compileJava` fails before the change
- [ ] 1.2 Extend the test of 1.1 (or add one next to it): with `java-library` and the `keys` template, the `api` configuration of the library contains `net.kyori:adventure-key`
- [ ] 1.3 Add a test for a project that applies only `java`: `implementation` contains `vulpes-base`, and no `api` configuration is touched

## 2. Implementation

- [ ] 2.1 In `VulpesPlugin`, add the runtime library of the selected template to `api` when `java-library` is applied, and to `implementation` otherwise; keep the version the plugin was built for and the template selection unchanged
- [ ] 2.2 Make the tests of 1.1 to 1.3 green

## 3. Documentation

- [ ] 3.1 Update the README sentence in the `minestom` template description that says vulpes-base is added "as a runtime dependency", so that it names the `api` configuration with `java-library` and `implementation` with `java`
- [ ] 3.2 Add a short note on multi-module builds to the README: the module that applies the plugin exposes the runtime library to its consumers, and the game still provides Minestom and the Adventure API

## 4. Pull request

- [ ] 4.1 Open the code pull request titled `fix(plugin): expose the template runtime library as api`
