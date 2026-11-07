package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CodegenTemplateTest {

    @Test
    void mapsMinestomName() {
        assertEquals(CodegenTemplate.MINESTOM, CodegenTemplate.fromName("minestom"), "template of minestom");
    }

    @Test
    void mapsKeysName() {
        assertEquals(CodegenTemplate.KEYS, CodegenTemplate.fromName("keys"), "template of keys");
    }

    @Test
    void rejectsUnknownNameNamingItAndTheKnownTemplates() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CodegenTemplate.fromName("fancy"));

        assertTrue(exception.getMessage().contains("'fancy'"), "message must name fancy: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("keys, minestom"),
                "message must list the known templates: " + exception.getMessage());
    }
}
