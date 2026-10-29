package net.onelitefeather.vulpes.gradle;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Decides the Java package of each content project.
 */
final class PackageResolver {

    private static final String DEFAULT_PACKAGE_PREFIX = "net.onelitefeather.vulpes.content.";

    private PackageResolver() {
    }

    /**
     * Resolves the package of every declared content project.
     *
     * @param projects declared content project names
     * @param packageName {@code vulpes.packageName}, or {@code null}
     * @param overrides {@code vulpes.project(name).packageName} by project name
     * @return package by project name, sorted by project name
     * @throws IllegalArgumentException if the settings are ambiguous or an override names an undeclared project
     */
    static Map<String, String> resolve(Collection<String> projects, String packageName,
                                       Map<String, String> overrides) {
        Set<String> declared = new TreeSet<>(projects);
        for (String name : overrides.keySet()) {
            if (!declared.contains(name)) {
                throw new IllegalArgumentException("vulpes.project(\"" + name + "\") does not match a declared "
                        + "content project, declared: " + String.join(", ", declared));
            }
        }
        if (packageName != null && declared.size() > 1) {
            throw new IllegalArgumentException("vulpes.packageName cannot be used with several content projects ("
                    + String.join(", ", declared) + "); set vulpes.project(\"<name>\").packageName for each project");
        }

        Map<String, String> packages = new TreeMap<>();
        for (String project : declared) {
            String resolved = overrides.get(project);
            if (resolved == null) {
                resolved = packageName != null ? packageName : DEFAULT_PACKAGE_PREFIX + JavaNames.packageSegment(project);
            }
            packages.put(project, resolved);
        }
        return packages;
    }
}
