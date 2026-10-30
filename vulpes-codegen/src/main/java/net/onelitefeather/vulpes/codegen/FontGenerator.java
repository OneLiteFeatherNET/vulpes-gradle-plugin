package net.onelitefeather.vulpes.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import java.util.List;
import java.util.stream.Collectors;
import net.onelitefeather.vulpes.format.FontSymbol;

/**
 * Writes the class of the custom font symbols.
 */
final class FontGenerator {

    private static final ClassName CUSTOM_FONT_SYMBOL = ClassName.get("net.onelitefeather.vulpes.font", "CustomFontSymbol");
    private static final ClassName FONT_SYMBOL = ClassName.get("net.onelitefeather.vulpes.font", "FontSymbol");
    private static final ClassName LIST = ClassName.get(List.class);

    private FontGenerator() {
    }

    /**
     * Generates {@code <Project>FontSymbols}.
     *
     * @throws CodegenException if two keys share a constant name or a key is malformed
     */
    static GeneratedSource generate(GenerationContext context, List<FontSymbol> fonts) {
        ConstantTable table = ConstantTable.of(context.namespace(), fonts.stream().map(EntryKeys::of).toList());
        ClassName keys = context.className(EntityType.FONTS.keysSuffix());
        List<EntryWriter.Built> built = fonts.stream().map(font -> {
            String name = table.entry(font.key()).name();
            String chars = font.chars().isEmpty() ? "" : font.chars().stream()
                    .map(JavaLiterals::string)
                    .collect(Collectors.joining(", "));
            return new EntryWriter.Built(name, CodeBlock.of("$T.of($T.$L, $L, new $T($L, $L, $L, $L, $T.of($L)))",
                    CUSTOM_FONT_SYMBOL, keys, name, KeysGenerator.keyInitializer(font.font()), FONT_SYMBOL,
                    JavaLiterals.string(font.file()), JavaLiterals.string(font.type()), font.ascent(), font.height(),
                    LIST, chars));
        }).toList();
        return EntryWriter.write(context, EntityType.FONTS, CUSTOM_FONT_SYMBOL, table, built);
    }
}
