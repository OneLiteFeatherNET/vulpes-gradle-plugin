package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.onelitefeather.vulpes.format.ContentRelease;
import org.junit.jupiter.api.Test;

final class TemplatesTest {

    private static final String VULPES_BASE = "0.0.0";
    private static final String ADVENTURE_KEY = "5.2.0";

    @Test
    void knowsExactlyTheKeysAndMinestomTemplates() {
        assertEquals(Set.of("keys", "minestom"), Templates.all(VULPES_BASE, ADVENTURE_KEY).keySet(), "template names");
    }

    @Test
    void minestomNeedsVulpesBaseInTheGivenVersion() {
        assertEquals(List.of(new Dependency("net.onelitefeather.vulpes", "vulpes-base", "0.0.0")),
                Templates.minestom(VULPES_BASE).runtimeDependencies(), "runtime dependency of minestom");
    }

    @Test
    void keysNeedsAdventureKeyInTheGivenVersion() {
        assertEquals(List.of(new Dependency("net.kyori", "adventure-key", "5.2.0")),
                Templates.keys(ADVENTURE_KEY).runtimeDependencies(), "runtime dependency of keys");
    }

    @Test
    void bothTemplatesSupportEveryEntityType() {
        assertEquals(EnumSet.allOf(EntityType.class), Templates.minestom(VULPES_BASE).supportedTypes(), "minestom");
        assertEquals(EnumSet.allOf(EntityType.class), Templates.keys(ADVENTURE_KEY).supportedTypes(), "keys");
    }

    @Test
    void minestomGeneratesEntriesContentAndResourcePacks() {
        List<String> names = Templates.generate(Templates.minestom(VULPES_BASE), TestContents.fixture(),
                TestContents.PACKAGE).stream().map(GeneratedSource::className).toList();

        assertTrue(names.containsAll(List.of("CygnusRelease", "CygnusItemKeys", "CygnusItems", "CygnusContent",
                "CygnusResourcePacks", "CygnusExpressionFunctions")), "classes of minestom: " + names);
    }

    @Test
    void keysGeneratesOnlyKeysReleaseAndExpressionFunctions() {
        List<GeneratedSource> sources = Templates.generate(Templates.keys(ADVENTURE_KEY), TestContents.fixture(),
                TestContents.PACKAGE);

        List<String> names = sources.stream().map(GeneratedSource::className).toList();
        assertEquals(List.of("CygnusRelease", "CygnusItemKeys", "CygnusAttributeKeys", "CygnusFontSymbolKeys",
                "CygnusSoundKeys", "CygnusAdvancementKeys", "CygnusExpressionFunctions"), names, "classes of keys");
        for (GeneratedSource source : sources) {
            assertFalse(source.source().contains("import net.onelitefeather.vulpes"),
                    source.className() + " must not reference vulpes-base:\n" + source.source());
        }
    }

    @Test
    void unsupportedEntityTypeFailsNamingTemplateAndType() {
        CodegenException exception = assertThrows(CodegenException.class,
                () -> Templates.generate(itemsOnly(), TestContents.fixture(), TestContents.PACKAGE),
                "a template without sounds must refuse a snapshot with sounds");

        assertTrue(exception.getMessage().contains("items-only") && exception.getMessage().contains("sounds"),
                "message must name the template and the type: " + exception.getMessage());
    }

    @Test
    void sameInputGeneratesIdenticalSources() {
        ContentRelease content = TestContents.fixture();

        List<GeneratedSource> first = Templates.generate(Templates.minestom(VULPES_BASE), content, TestContents.PACKAGE);
        List<GeneratedSource> second = Templates.generate(Templates.minestom(VULPES_BASE), content, TestContents.PACKAGE);

        assertEquals(first, second, "equal input must produce equal sources");
    }

    @Test
    void releaseClassIsUnchangedFromReleaseWriter() {
        ContentRelease content = TestContents.fixture();

        GeneratedSource release = Templates.generate(Templates.keys(ADVENTURE_KEY), content, TestContents.PACKAGE).get(0);

        assertEquals(ReleaseClassWriter.write(TestContents.PACKAGE, content.release()), release.source(),
                "the release class is the one of the release writer");
    }

    private static Template itemsOnly() {
        return new Template() {
            @Override
            public String name() {
                return "items-only";
            }

            @Override
            public Set<EntityType> supportedTypes() {
                return EnumSet.of(EntityType.ITEMS);
            }

            @Override
            public List<Dependency> runtimeDependencies() {
                return List.of();
            }

            @Override
            public List<GeneratedSource> generate(ContentRelease content, String packageName) {
                return List.of();
            }
        };
    }
}
