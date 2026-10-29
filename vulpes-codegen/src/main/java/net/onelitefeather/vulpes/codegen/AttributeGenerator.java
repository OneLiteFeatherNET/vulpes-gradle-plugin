package net.onelitefeather.vulpes.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import java.util.List;
import net.onelitefeather.vulpes.format.Attribute;

/**
 * Writes the class of the custom attributes.
 */
final class AttributeGenerator {

    private static final ClassName CUSTOM_ATTRIBUTE = ClassName.get("net.onelitefeather.vulpes.attribute", "CustomAttribute");

    private AttributeGenerator() {
    }

    /**
     * Generates {@code <Project>Attributes}.
     *
     * @throws CodegenException if two keys share a constant name or a value is not finite
     */
    static GeneratedSource generate(GenerationContext context, List<Attribute> attributes) {
        ConstantTable table = ConstantTable.of(context.namespace(), attributes.stream().map(EntryKeys::of).toList());
        ClassName keys = context.className(EntityType.ATTRIBUTES.keysSuffix());
        List<EntryWriter.Built> built = attributes.stream().map(attribute -> {
            String name = table.entry(attribute.key()).name();
            String translationKey = attribute.translationKey() == null ? "null"
                    : JavaLiterals.string(attribute.translationKey());
            return new EntryWriter.Built(name, CodeBlock.of("$T.of($T.$L, $L, $L, $L, $L, $L, $L)", CUSTOM_ATTRIBUTE,
                    keys, name, JavaLiterals.string(attribute.displayName()), translationKey,
                    JavaLiterals.doubleValue(attribute.minimumValue()), JavaLiterals.doubleValue(attribute.defaultValue()),
                    JavaLiterals.doubleValue(attribute.maximumValue()), attribute.synced()));
        }).toList();
        return EntryWriter.write(context, EntityType.ATTRIBUTES, CUSTOM_ATTRIBUTE, table, built);
    }
}
