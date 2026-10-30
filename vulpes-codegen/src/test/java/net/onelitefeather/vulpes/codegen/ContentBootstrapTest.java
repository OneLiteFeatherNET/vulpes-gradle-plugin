package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.vulpes.Vulpes;
import net.onelitefeather.vulpes.VulpesContent;
import net.onelitefeather.vulpes.VulpesRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ContentBootstrapTest {

    @BeforeAll
    static void initMinestomRegistries() {
        MinecraftServer.init();
    }

    @Test
    void bootstrapRegistersEveryItemOfTheFixture(@TempDir Path output) throws Exception {
        VulpesRegistries registries = bootstrapFixture(output);

        for (String key : List.of("cygnus:iron_sword", "cygnus:ration")) {
            assertTrue(registries.items().contains(Key.key(key)), "item registry must contain " + key);
        }
    }

    @Test
    void renamedItemResolvesToCurrentItem(@TempDir Path output) throws Exception {
        VulpesRegistries registries = bootstrapFixture(output);

        assertEquals(Key.key("cygnus:iron_sword"), registries.items().get(Key.key("cygnus:old_sword")).key(),
                "the alias must resolve to the current item");
    }

    /**
     * Bootstraps the generated content of the fixture. The enchantments of the fixture item are left out: vulpes-base
     * decodes components without the registries of a server, and the enchantment codec needs them.
     */
    private static VulpesRegistries bootstrapFixture(Path output) throws IOException, ReflectiveOperationException {
        GeneratedSourcesCompileTest.compile(
                Templates.generate(Templates.minestom("0.0.0"), TestContents.fixture(),
                        TestContents.PACKAGE),
                GeneratedSourcesCompileTest.classPath(entry -> true), output);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{output.toUri().toURL()},
                ContentBootstrapTest.class.getClassLoader())) {
            Class<?> contentClass = loader.loadClass(TestContents.PACKAGE + ".CygnusContent");
            VulpesContent content = (VulpesContent) contentClass.getField("INSTANCE").get(null);
            return Vulpes.bootstrap(content);
        }
    }
}
