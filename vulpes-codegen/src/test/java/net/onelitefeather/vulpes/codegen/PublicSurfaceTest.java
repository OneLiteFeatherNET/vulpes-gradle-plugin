package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Pins the public surface of the facade package. Adding or changing a public member fails this test until the
 * expected list is updated on purpose, because everything listed here is covered by the compatibility promise.
 */
final class PublicSurfaceTest {

    private static final Set<String> PUBLIC_TYPES = Set.of("CodegenException", "CodegenOptions", "CodegenTemplate",
            "Dependency", "GeneratedSource", "VulpesCodegen");

    @Test
    void packageContainsExactlyTheFacadeTypes() throws IOException {
        Path packageDirectory = packageDirectory();

        Set<String> publicTypes;
        try (Stream<Path> files = Files.list(packageDirectory)) {
            publicTypes = files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".class") && !name.contains("$"))
                    .map(name -> name.substring(0, name.length() - ".class".length()))
                    .filter(name -> Modifier.isPublic(classNamed(name).getModifiers()))
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        assertEquals(PUBLIC_TYPES, publicTypes, "public types of net.onelitefeather.vulpes.codegen");
    }

    @Test
    void vulpesCodegenHasExactlyItsThreeStaticMethods() {
        assertEquals(sorted(List.of(
                        "public static java.util.List generate(net.onelitefeather.vulpes.format.ContentRelease,"
                                + " net.onelitefeather.vulpes.codegen.CodegenOptions)",
                        "public static java.util.List runtimeDependencies(net.onelitefeather.vulpes.codegen.CodegenTemplate)",
                        "public static void write(net.onelitefeather.vulpes.format.ContentRelease,"
                                + " net.onelitefeather.vulpes.codegen.CodegenOptions, java.nio.file.Path)")),
                publicMembers(VulpesCodegen.class), "public members of VulpesCodegen");
    }

    @Test
    void codegenOptionsHasTheComponentsOnly() {
        assertEquals(sorted(List.of(
                        "public final boolean equals(java.lang.Object)",
                        "public final int hashCode()",
                        "public java.lang.String packageName()",
                        "public final java.lang.String toString()",
                        "public net.onelitefeather.vulpes.codegen.CodegenTemplate template()",
                        "public net.onelitefeather.vulpes.codegen.CodegenOptions(java.lang.String,"
                                + " net.onelitefeather.vulpes.codegen.CodegenTemplate)")),
                publicMembers(CodegenOptions.class), "public members of CodegenOptions");
    }

    @Test
    void codegenTemplateHasTheTwoConstantsAndFromName() {
        assertEquals(sorted(List.of(
                        "public static net.onelitefeather.vulpes.codegen.CodegenTemplate fromName(java.lang.String)",
                        "public static final net.onelitefeather.vulpes.codegen.CodegenTemplate KEYS",
                        "public static final net.onelitefeather.vulpes.codegen.CodegenTemplate MINESTOM",
                        "public static net.onelitefeather.vulpes.codegen.CodegenTemplate valueOf(java.lang.String)",
                        "public static net.onelitefeather.vulpes.codegen.CodegenTemplate[] values()")),
                publicMembers(CodegenTemplate.class), "public members of CodegenTemplate");
    }

    @Test
    void generatedSourceHasTheThreeComponents() {
        assertEquals(sorted(List.of(
                        "public final boolean equals(java.lang.Object)",
                        "public final int hashCode()",
                        "public java.lang.String className()",
                        "public java.lang.String packageName()",
                        "public java.lang.String source()",
                        "public final java.lang.String toString()",
                        "public net.onelitefeather.vulpes.codegen.GeneratedSource(java.lang.String,"
                                + " java.lang.String, java.lang.String)")),
                publicMembers(GeneratedSource.class), "public members of GeneratedSource");
    }

    @Test
    void dependencyHasTheThreeComponents() {
        assertEquals(sorted(List.of(
                        "public final boolean equals(java.lang.Object)",
                        "public final int hashCode()",
                        "public java.lang.String group()",
                        "public java.lang.String name()",
                        "public final java.lang.String toString()",
                        "public java.lang.String version()",
                        "public net.onelitefeather.vulpes.codegen.Dependency(java.lang.String, java.lang.String,"
                                + " java.lang.String)")),
                publicMembers(Dependency.class), "public members of Dependency");
    }

    @Test
    void codegenExceptionHasOneMessageConstructor() {
        assertEquals(sorted(List.of("public net.onelitefeather.vulpes.codegen.CodegenException(java.lang.String)")),
                publicMembers(CodegenException.class), "public members of CodegenException");
    }

    /** The directory of the compiled classes of this package, next to {@link VulpesCodegen}. */
    private static Path packageDirectory() {
        try {
            return Path.of(VulpesCodegen.class.getResource("VulpesCodegen.class").toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("cannot locate the classes of VulpesCodegen", e);
        }
    }

    private static Class<?> classNamed(String simpleName) {
        try {
            return Class.forName("net.onelitefeather.vulpes.codegen." + simpleName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("class of the package not loadable: " + simpleName, e);
        }
    }

    private static List<String> sorted(List<String> members) {
        return members.stream().sorted().toList();
    }

    /** Public constructors, methods and fields as strings, sorted, without synthetic members. */
    private static List<String> publicMembers(Class<?> type) {
        List<String> members = new ArrayList<>();
        for (Constructor<?> constructor : type.getConstructors()) {
            members.add(Modifier.toString(constructor.getModifiers()) + " " + type.getName() + "("
                    + parameters(constructor.getParameterTypes()) + ")");
        }
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                members.add(Modifier.toString(method.getModifiers()) + " " + method.getReturnType().getTypeName() + " "
                        + method.getName() + "(" + parameters(method.getParameterTypes()) + ")");
            }
        }
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && !field.isSynthetic()) {
                members.add(Modifier.toString(field.getModifiers()) + " " + field.getType().getTypeName() + " "
                        + field.getName());
            }
        }
        return members.stream().sorted().toList();
    }

    private static String parameters(Class<?>[] types) {
        return Arrays.stream(types).map(Class::getTypeName).collect(Collectors.joining(", "));
    }
}
