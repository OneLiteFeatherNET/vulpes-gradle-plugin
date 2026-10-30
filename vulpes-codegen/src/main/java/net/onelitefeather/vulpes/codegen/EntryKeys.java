package net.onelitefeather.vulpes.codegen;

import java.util.List;
import net.onelitefeather.vulpes.format.Advancement;
import net.onelitefeather.vulpes.format.Attribute;
import net.onelitefeather.vulpes.format.FontSymbol;
import net.onelitefeather.vulpes.format.Item;
import net.onelitefeather.vulpes.format.SoundEvent;

/**
 * The key of one entry and the keys it was renamed from.
 *
 * @param key          the current key of the entry
 * @param previousKeys the keys the entry had before, each of them becomes a deprecated alias
 */
record EntryKeys(String key, List<String> previousKeys) {

    static EntryKeys of(Item item) {
        return new EntryKeys(item.key(), item.previousKeys());
    }

    static EntryKeys of(Attribute attribute) {
        return new EntryKeys(attribute.key(), attribute.previousKeys());
    }

    static EntryKeys of(FontSymbol font) {
        return new EntryKeys(font.key(), font.previousKeys());
    }

    static EntryKeys of(SoundEvent sound) {
        return new EntryKeys(sound.key(), sound.previousKeys());
    }

    static EntryKeys of(Advancement advancement) {
        return new EntryKeys(advancement.key(), advancement.previousKeys());
    }
}
