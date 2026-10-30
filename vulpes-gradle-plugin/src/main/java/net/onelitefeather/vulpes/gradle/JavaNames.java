package net.onelitefeather.vulpes.gradle;

import java.util.Locale;
import javax.lang.model.SourceVersion;

/**
 * Maps Vulpes project and pack names to Java identifiers.
 */
final class JavaNames {

    private static final String PACK_PREFIX = "PACK_";

    private JavaNames() {
    }

    /**
     * Package segment of a project name, e.g. {@code my-game} becomes {@code my_game}.
     */
    static String packageSegment(String project) {
        String segment = project.replace('-', '_');
        return SourceVersion.isKeyword(segment) ? segment + "_" : segment;
    }

    /**
     * Type name of a project name, e.g. {@code my-game} becomes {@code MyGame}.
     */
    static String typeName(String project) {
        StringBuilder name = new StringBuilder();
        for (String segment : project.split("-")) {
            if (!segment.isEmpty()) {
                name.append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
            }
        }
        return name.toString();
    }

    /**
     * Constant name of a pack, e.g. {@code 16x} becomes {@code PACK_16X}.
     */
    static String constantName(String packId) {
        return PACK_PREFIX + packId.replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
