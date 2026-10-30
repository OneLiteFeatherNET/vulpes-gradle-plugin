package net.onelitefeather.vulpes.codegen;

/**
 * The entity types of a content snapshot that a template can generate code for.
 */
public enum EntityType {

    ITEMS("items", "ItemKeys", "Items"),
    ATTRIBUTES("attributes", "AttributeKeys", "Attributes"),
    FONTS("fonts", "FontSymbolKeys", "FontSymbols"),
    SOUNDS("sounds", "SoundKeys", "Sounds"),
    ADVANCEMENTS("advancements", "AdvancementKeys", "Advancements"),
    EXPRESSION_FUNCTIONS("expressionFunctions", null, "ExpressionFunctions");

    private final String label;
    private final String keysSuffix;
    private final String entriesSuffix;

    EntityType(String label, String keysSuffix, String entriesSuffix) {
        this.label = label;
        this.keysSuffix = keysSuffix;
        this.entriesSuffix = entriesSuffix;
    }

    /**
     * Name of the type as used in the data JAR, for example {@code expressionFunctions}.
     */
    public String label() {
        return label;
    }

    /**
     * Simple name suffix of the class of the key constants, without the project prefix; {@code null} if none.
     */
    String keysSuffix() {
        return keysSuffix;
    }

    /**
     * Simple name suffix of the class of the built entries, without the project prefix.
     */
    String entriesSuffix() {
        return entriesSuffix;
    }
}
