package net.onelitefeather.vulpes.codegen.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.onelitefeather.vulpes.codegen.GeneratedSource;
import net.onelitefeather.vulpes.format.ContentRelease;
import org.junit.jupiter.api.Test;

final class SpaceGlyphGeneratorTest {

    private static final String VULPES_BASE = "0.0.0";
    private static final String ADVENTURE_KEY = "5.2.0";
    private static final String CLASS_NAME = "CygnusSpaceGlyphs";
    private static final String FONT_DECLARATION = "public static final Key FONT = Key.key(\"space\", \"default\");";
    private static final List<String> CONSTANT_NAMES = List.of("NEGATIVE_1", "NEGATIVE_2", "NEGATIVE_3", "NEGATIVE_4",
            "NEGATIVE_5", "NEGATIVE_6", "NEGATIVE_7", "NEGATIVE_8", "NEGATIVE_16", "NEGATIVE_32", "NEGATIVE_64",
            "NEGATIVE_128", "NEGATIVE_256", "ZERO", "POSITIVE_1", "POSITIVE_2", "POSITIVE_3", "POSITIVE_4",
            "POSITIVE_5", "POSITIVE_6", "POSITIVE_7", "POSITIVE_8", "POSITIVE_16", "POSITIVE_32", "POSITIVE_64",
            "POSITIVE_128", "POSITIVE_256");
    private static final Pattern STRING_CONSTANT = Pattern.compile(
            "public static final String ([A-Z0-9_]+) = ");
    private static final Pattern WIDTH_ENTRY = Pattern.compile("\"(\\d+)\"\\s*:\\s*(-?\\d+)");
    private static final Pattern SPACE_DEFAULT = Pattern.compile("\"space:default\"\\s*:\\s*\\{([^}]*)}");

    @Test
    void snapshotWithoutFontsStillGeneratesAllGlyphConstants() {
        ContentRelease content = TestContents.content(TestContents.empty());

        GeneratedSource source = sourceNamed(
                Templates.generate(Templates.minestom(VULPES_BASE), content, TestContents.PACKAGE));

        assertEquals(CONSTANT_NAMES, constantNames(source.source()), "glyph constants:\n" + source.source());
        assertTrue(source.source().contains(FONT_DECLARATION), "font key:\n" + source.source());
    }

    @Test
    void keysTemplateAlsoGeneratesSpaceGlyphs() {
        GeneratedSource source = sourceNamed(
                Templates.generate(Templates.keys(ADVENTURE_KEY), TestContents.fixture(), TestContents.PACKAGE));

        assertEquals(CONSTANT_NAMES, constantNames(source.source()), "glyph constants:\n" + source.source());
        assertTrue(source.source().contains(FONT_DECLARATION), "font key:\n" + source.source());
    }

    @Test
    void glyphAdvanceMatchesSpaceFontWidthsOfVulpesBase() {
        Map<Integer, Integer> widths = spaceDefaultWidths();

        for (SpaceGlyphGenerator.Glyph glyph : SpaceGlyphGenerator.GLYPHS) {
            int codepoint = glyph.character();
            String hex = String.format("U+%04X", codepoint);
            assertTrue(widths.containsKey(codepoint), hex + " of " + glyph.name() + " is missing in space:default");
            assertEquals(widths.get(codepoint), glyph.advance(),
                    hex + " has the advance " + widths.get(codepoint) + " in space:default but "
                            + glyph.name() + " states " + glyph.advance());
            assertEquals(nameOf(glyph.advance()), glyph.name(), hex + " must be named after its advance");
        }
    }

    private static GeneratedSource sourceNamed(List<GeneratedSource> sources) {
        return sources.stream().filter(source -> source.className().equals(CLASS_NAME)).findFirst()
                .orElseThrow(() -> new AssertionError(CLASS_NAME + " missing in "
                        + sources.stream().map(GeneratedSource::className).toList()));
    }

    private static List<String> constantNames(String source) {
        List<String> names = new ArrayList<>();
        Matcher matcher = STRING_CONSTANT.matcher(source);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static Map<Integer, Integer> spaceDefaultWidths() {
        String json = readResource("font/font-widths.json");
        Matcher space = SPACE_DEFAULT.matcher(json);
        assertTrue(space.find(), "space:default must be in font/font-widths.json");
        Matcher entry = WIDTH_ENTRY.matcher(space.group(1));
        Map<Integer, Integer> widths = new HashMap<>();
        while (entry.find()) {
            widths.put(Integer.parseInt(entry.group(1)), Integer.parseInt(entry.group(2)));
        }
        return widths;
    }

    private static String readResource(String name) {
        try (InputStream input = SpaceGlyphGeneratorTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, name + " must be on the test classpath");
            return new String(input.readAllBytes(), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The constant name a glyph of the given advance has: NEGATIVE_n, ZERO or POSITIVE_n.
     */
    private static String nameOf(int advance) {
        if (advance == 0) {
            return "ZERO";
        }
        return (advance < 0 ? "NEGATIVE_" : "POSITIVE_") + Math.abs(advance);
    }
}
