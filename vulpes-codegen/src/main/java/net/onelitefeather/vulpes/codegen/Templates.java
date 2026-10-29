package net.onelitefeather.vulpes.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.onelitefeather.vulpes.format.ContentRelease;
import net.onelitefeather.vulpes.format.Snapshot;

/**
 * The templates of the plugin, by name: {@code minestom} generates the full code against vulpes-base, {@code keys}
 * generates the key constants only and needs adventure-key.
 */
public final class Templates {

    private static final String VULPES_BASE_GROUP = "net.onelitefeather.vulpes";
    private static final String ADVENTURE_KEY_GROUP = "net.kyori";

    private Templates() {
    }

    /**
     * The {@code minestom} template, built against the given version of vulpes-base.
     */
    public static Template minestom(String vulpesBaseVersion) {
        return new ContentTemplate("minestom", true,
                List.of(new Dependency(VULPES_BASE_GROUP, "vulpes-base", vulpesBaseVersion)));
    }

    /**
     * The {@code keys} template, which needs adventure-key in the given version only.
     */
    public static Template keys(String adventureKeyVersion) {
        return new ContentTemplate("keys", false,
                List.of(new Dependency(ADVENTURE_KEY_GROUP, "adventure-key", adventureKeyVersion)));
    }

    /**
     * All templates by name, sorted by name.
     */
    public static Map<String, Template> all(String vulpesBaseVersion, String adventureKeyVersion) {
        Map<String, Template> templates = new TreeMap<>();
        for (Template template : List.of(minestom(vulpesBaseVersion), keys(adventureKeyVersion))) {
            templates.put(template.name(), template);
        }
        return Collections.unmodifiableMap(templates);
    }

    /**
     * Generates the sources of a content release with the given template.
     *
     * @throws CodegenException if the snapshot has entries of a type the template does not support
     */
    public static List<GeneratedSource> generate(Template template, ContentRelease content, String packageName) {
        Set<EntityType> unsupported = EnumSet.noneOf(EntityType.class);
        for (EntityType type : EntityType.values()) {
            if (!template.supportedTypes().contains(type) && isPresent(content.snapshot(), type)) {
                unsupported.add(type);
            }
        }
        if (!unsupported.isEmpty()) {
            throw new CodegenException("template '" + template.name() + "' does not support "
                    + unsupported.stream().map(EntityType::label).collect(Collectors.joining(", "))
                    + " of project '" + content.release().project() + "'");
        }
        return template.generate(content, packageName);
    }

    private static boolean isPresent(Snapshot snapshot, EntityType type) {
        return switch (type) {
            case ITEMS -> !snapshot.items().isEmpty();
            case ATTRIBUTES -> !snapshot.attributes().isEmpty();
            case FONTS -> !snapshot.fonts().isEmpty();
            case SOUNDS -> !snapshot.sounds().isEmpty();
            case ADVANCEMENTS -> !snapshot.advancements().isEmpty();
            case EXPRESSION_FUNCTIONS -> !snapshot.expressionFunctions().isEmpty();
        };
    }

    /**
     * A template that writes the release class, the key classes and the interface of the expression functions, and
     * for {@code minestom} also the entry classes, the content object and the resource packs.
     */
    private static final class ContentTemplate implements Template {

        private final String name;
        private final boolean entries;
        private final List<Dependency> dependencies;

        ContentTemplate(String name, boolean entries, List<Dependency> dependencies) {
            this.name = name;
            this.entries = entries;
            this.dependencies = List.copyOf(dependencies);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Set<EntityType> supportedTypes() {
            return EnumSet.allOf(EntityType.class);
        }

        @Override
        public List<Dependency> runtimeDependencies() {
            return dependencies;
        }

        @Override
        public List<GeneratedSource> generate(ContentRelease content, String packageName) {
            var release = content.release();
            Snapshot snapshot = content.snapshot();
            GenerationContext context = new GenerationContext(packageName, release.project(), release.version(),
                    snapshot.namespace());
            List<GeneratedSource> sources = new ArrayList<>();
            sources.add(new GeneratedSource(packageName, ReleaseClassWriter.className(release.project()),
                    ReleaseClassWriter.write(packageName, release)));
            sources.add(KeysGenerator.generate(context, EntityType.ITEMS,
                    snapshot.items().stream().map(EntryKeys::of).toList()));
            sources.add(KeysGenerator.generate(context, EntityType.ATTRIBUTES,
                    snapshot.attributes().stream().map(EntryKeys::of).toList()));
            sources.add(KeysGenerator.generate(context, EntityType.FONTS,
                    snapshot.fonts().stream().map(EntryKeys::of).toList()));
            sources.add(KeysGenerator.generate(context, EntityType.SOUNDS,
                    snapshot.sounds().stream().map(EntryKeys::of).toList()));
            sources.add(KeysGenerator.generate(context, EntityType.ADVANCEMENTS,
                    snapshot.advancements().stream().map(EntryKeys::of).toList()));
            if (entries) {
                sources.add(ItemGenerator.generate(context, snapshot.items()));
                sources.add(AttributeGenerator.generate(context, snapshot.attributes()));
                sources.add(FontGenerator.generate(context, snapshot.fonts()));
                sources.add(SoundGenerator.generate(context, snapshot.sounds()));
                sources.add(AdvancementGenerator.generate(context, snapshot.advancements()));
                sources.add(ContentGenerator.generate(context, snapshot));
                sources.add(ResourcePackGenerator.generate(context, release.packs()));
            }
            ExpressionFunctionGenerator.generate(context, snapshot.expressionFunctions()).ifPresent(sources::add);
            return List.copyOf(sources);
        }
    }
}
