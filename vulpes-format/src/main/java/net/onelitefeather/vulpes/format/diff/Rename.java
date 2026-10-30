package net.onelitefeather.vulpes.format.diff;

/**
 * An entity that kept its id and got another key.
 *
 * @param aliased whether {@code fromKey} is listed in the {@code previousKeys} of the new entity
 */
public record Rename(String fromKey, String toKey, boolean aliased) {
}
