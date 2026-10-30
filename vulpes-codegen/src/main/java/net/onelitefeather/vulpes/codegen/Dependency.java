package net.onelitefeather.vulpes.codegen;

/**
 * A Maven coordinate of a library the generated code needs at runtime.
 *
 * @param group   the group id
 * @param name    the artifact id
 * @param version the version the generator was built for
 */
public record Dependency(String group, String name, String version) {
}
