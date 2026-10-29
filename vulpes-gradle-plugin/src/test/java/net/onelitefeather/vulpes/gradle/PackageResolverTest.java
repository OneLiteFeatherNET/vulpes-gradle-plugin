package net.onelitefeather.vulpes.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PackageResolverTest {

    @Test
    void defaultPackageUsesSanitizedProjectName() {
        assertEquals(Map.of("my-game", "net.onelitefeather.vulpes.content.my_game"),
                PackageResolver.resolve(List.of("my-game"), null, Map.of()), "default package");
    }

    @Test
    void singleProjectUsesPackageName() {
        assertEquals(Map.of("cygnus", "net.onelitefeather.cygnus.content"),
                PackageResolver.resolve(List.of("cygnus"), "net.onelitefeather.cygnus.content", Map.of()),
                "package name of the only project");
    }

    @Test
    void projectOverrideWinsOverPackageName() {
        assertEquals(Map.of("cygnus", "a.cygnus"),
                PackageResolver.resolve(List.of("cygnus"), "net.onelitefeather.cygnus.content",
                        Map.of("cygnus", "a.cygnus")), "override of the only project");
    }

    @Test
    void projectOverrideWinsOverDefaultPackage() {
        assertEquals(Map.of("cygnus", "a.cygnus"),
                PackageResolver.resolve(List.of("cygnus"), null, Map.of("cygnus", "a.cygnus")),
                "override replaces the default package");
    }

    @Test
    void severalProjectsGetOwnDefaultPackages() {
        assertEquals(Map.of("cygnus", "net.onelitefeather.vulpes.content.cygnus",
                        "manis", "net.onelitefeather.vulpes.content.manis"),
                PackageResolver.resolve(List.of("cygnus", "manis"), null, Map.of()),
                "default packages of two projects");
    }

    @Test
    void severalProjectsWithPackageNameFailAndNameProjectSetting() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PackageResolver.resolve(List.of("cygnus", "manis"), "x.y", Map.of()));

        assertTrue(e.getMessage().contains("vulpes.project(\"<name>\").packageName"),
                "message must name the per-project setting but was: " + e.getMessage());
    }

    @Test
    void overrideForUndeclaredProjectFailsAndNamesDeclaredProjects() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PackageResolver.resolve(List.of("cygnus"), null, Map.of("manis", "a.manis")));

        assertTrue(e.getMessage().contains("manis") && e.getMessage().contains("cygnus"),
                "message must name the undeclared and the declared project but was: " + e.getMessage());
    }
}
