package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CodegenOptionsTest {

    @Test
    void rejectsPackageWithEmptySegmentNamingIt() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new CodegenOptions("net..example", CodegenTemplate.MINESTOM));

        assertTrue(exception.getMessage().contains("net..example"),
                "message must name the invalid package: " + exception.getMessage());
    }

    @Test
    void rejectsPackageNamedByKeywordNamingIt() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new CodegenOptions("net.class.example", CodegenTemplate.KEYS));

        assertTrue(exception.getMessage().contains("net.class.example"),
                "message must name the invalid package: " + exception.getMessage());
    }

    @Test
    void keepsPackageAndTemplate() {
        CodegenOptions options = new CodegenOptions("net.example.content", CodegenTemplate.KEYS);

        assertEquals("net.example.content", options.packageName(), "package name");
        assertEquals(CodegenTemplate.KEYS, options.template(), "template");
    }
}
