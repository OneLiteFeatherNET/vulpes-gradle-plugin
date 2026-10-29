package net.onelitefeather.vulpes.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import java.util.List;
import net.onelitefeather.vulpes.format.SoundEvent;

/**
 * Writes the class of the custom sounds. The sound files themselves are pack content and not generated here.
 */
final class SoundGenerator {

    private static final ClassName CUSTOM_SOUND = ClassName.get("net.onelitefeather.vulpes.sound", "CustomSound");

    private SoundGenerator() {
    }

    /**
     * Generates {@code <Project>Sounds}. Every sound uses the default attenuation, the contract has no range.
     *
     * @throws CodegenException if two keys share a constant name
     */
    static GeneratedSource generate(GenerationContext context, List<SoundEvent> sounds) {
        ConstantTable table = ConstantTable.of(context.namespace(), sounds.stream().map(EntryKeys::of).toList());
        ClassName keys = context.className(EntityType.SOUNDS.keysSuffix());
        List<EntryWriter.Built> built = sounds.stream()
                .map(sound -> new EntryWriter.Built(table.entry(sound.key()).name(),
                        CodeBlock.of("$T.of($T.$L, null)", CUSTOM_SOUND, keys, table.entry(sound.key()).name())))
                .toList();
        return EntryWriter.write(context, EntityType.SOUNDS, CUSTOM_SOUND, table, built);
    }
}
