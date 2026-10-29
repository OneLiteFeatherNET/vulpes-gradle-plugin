package net.onelitefeather.vulpes.format.diff;

/**
 * The smallest semantic version bump a set of changes requires, in ascending order.
 */
public enum VersionBump {
    NONE,
    PATCH,
    MINOR,
    MAJOR
}
