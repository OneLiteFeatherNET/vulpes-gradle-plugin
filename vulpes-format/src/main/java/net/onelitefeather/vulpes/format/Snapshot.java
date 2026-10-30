package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Content of {@code META-INF/vulpes/snapshot.json}: every entity of one project.
 */
public record Snapshot(int format, UUID projectId, String namespace, List<Item> items, List<Attribute> attributes,
                       List<FontSymbol> fonts, List<SoundEvent> sounds, List<Advancement> advancements,
                       List<ExpressionFunction> expressionFunctions, List<Expression> expressions, List<Asset> assets,
                       PackSettings pack) {

    public Snapshot {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(namespace, "namespace");
        items = List.copyOf(items);
        attributes = List.copyOf(attributes);
        fonts = List.copyOf(fonts);
        sounds = List.copyOf(sounds);
        advancements = List.copyOf(advancements);
        expressionFunctions = List.copyOf(expressionFunctions);
        expressions = List.copyOf(expressions);
        assets = List.copyOf(assets);
        Objects.requireNonNull(pack, "pack");
    }
}
