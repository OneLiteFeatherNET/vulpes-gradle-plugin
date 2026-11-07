package net.onelitefeather.vulpes.codegen.internal;

import static com.palantir.javapoet.TypeSpec.classBuilder;

import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import javax.lang.model.element.Modifier;
import net.onelitefeather.vulpes.codegen.GeneratedSource;

/**
 * Writes the class of the glyphs of the space font: one constant per glyph, named after the advance it moves the
 * text cursor by. The codepoints are fixed here and checked against the width table of vulpes-base by a test.
 */
final class SpaceGlyphGenerator {

    /**
     * The key of the space font, which every resource pack contains.
     */
    static final String FONT_KEY = "space:default";

    /**
     * The glyphs of the space font: the constant name, the codepoint and the advance in pixels.
     */
    static final List<Glyph> GLYPHS = List.of(
            new Glyph("NEGATIVE_1", '\uF001', -1),
            new Glyph("NEGATIVE_2", '\uF002', -2),
            new Glyph("NEGATIVE_3", '\uF003', -3),
            new Glyph("NEGATIVE_4", '\uF004', -4),
            new Glyph("NEGATIVE_5", '\uF005', -5),
            new Glyph("NEGATIVE_6", '\uF006', -6),
            new Glyph("NEGATIVE_7", '\uF007', -7),
            new Glyph("NEGATIVE_8", '\uF008', -8),
            new Glyph("NEGATIVE_16", '\uF009', -16),
            new Glyph("NEGATIVE_32", '\uF00A', -32),
            new Glyph("NEGATIVE_64", '\uF00B', -64),
            new Glyph("NEGATIVE_128", '\uF00C', -128),
            new Glyph("NEGATIVE_256", '\uF00D', -256),
            new Glyph("ZERO", '\uF00E', 0),
            new Glyph("POSITIVE_1", '\uF00F', 1),
            new Glyph("POSITIVE_2", '\uF010', 2),
            new Glyph("POSITIVE_3", '\uF011', 3),
            new Glyph("POSITIVE_4", '\uF012', 4),
            new Glyph("POSITIVE_5", '\uF013', 5),
            new Glyph("POSITIVE_6", '\uF014', 6),
            new Glyph("POSITIVE_7", '\uF015', 7),
            new Glyph("POSITIVE_8", '\uF016', 8),
            new Glyph("POSITIVE_16", '\uF017', 16),
            new Glyph("POSITIVE_32", '\uF018', 32),
            new Glyph("POSITIVE_64", '\uF019', 64),
            new Glyph("POSITIVE_128", '\uF01A', 128),
            new Glyph("POSITIVE_256", '\uF01B', 256));

    private SpaceGlyphGenerator() {
    }

    /**
     * Generates {@code <Project>SpaceGlyphs}. It needs no snapshot data, so every project gets it.
     */
    static GeneratedSource generate(GenerationContext context) {
        TypeSpec.Builder glyphs = classBuilder(context.prefix() + "SpaceGlyphs")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(JavaSources.classJavadoc(context, "Glyphs of the space font {@code " + FONT_KEY
                        + "}, which move the text cursor by the number of pixels in their name."));
        glyphs.addField(FieldSpec.builder(KeysGenerator.KEY, "FONT", Modifier.PUBLIC, Modifier.STATIC,
                Modifier.FINAL).initializer(KeysGenerator.keyInitializer(FONT_KEY)).build());
        for (Glyph glyph : GLYPHS) {
            glyphs.addField(FieldSpec.builder(String.class, glyph.name(), Modifier.PUBLIC, Modifier.STATIC,
                    Modifier.FINAL).initializer("$L", JavaLiterals.string(String.valueOf(glyph.character())))
                    .build());
        }
        glyphs.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        return JavaSources.render(context.packageName(), glyphs.build());
    }

    /**
     * A glyph of the space font.
     *
     * @param name      the constant name, which states the advance
     * @param character the private-use character of the glyph
     * @param advance   the pixels the glyph moves the text cursor by
     */
    record Glyph(String name, char character, int advance) {
    }
}
