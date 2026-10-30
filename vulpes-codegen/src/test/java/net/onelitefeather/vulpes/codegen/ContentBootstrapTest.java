package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
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

    @Test
    void armorDamageEvaluatesFormulaWithEngine(@TempDir Path output) throws Exception {
        try (URLClassLoader loader = compileFixture(output)) {
            Object formulas = formulas(loader);

            BigDecimal damage = (BigDecimal) formulas.getClass()
                    .getMethod("armorDamage", BigDecimal.class, BigDecimal.class)
                    .invoke(formulas, new BigDecimal("100"), new BigDecimal("25"));

            assertEquals(0, new BigDecimal("75").compareTo(damage), "armorDamage(100, 25) must be 75, was " + damage);
        }
    }

    @Test
    void maxHealthEvaluatesExpressionThroughFormula(@TempDir Path output) throws Exception {
        try (URLClassLoader loader = compileFixture(output)) {
            Object formulas = formulas(loader);

            BigDecimal health = (BigDecimal) formulas.getClass()
                    .getMethod("maxHealth", BigDecimal.class)
                    .invoke(formulas, new BigDecimal("3"));

            assertEquals(0, new BigDecimal("26").compareTo(health), "maxHealth(3) = 20 + 3 * armorDamage(2, 0) = 26, was " + health);
        }
    }

    /**
     * Compiles the generated minestom sources of the fixture and returns a loader for their classes.
     */
    private static URLClassLoader compileFixture(Path output) throws IOException {
        String diagnostics = GeneratedSourcesCompileTest.compile(
                Templates.generate(Templates.minestom("0.0.0"), TestContents.fixture(), TestContents.PACKAGE),
                GeneratedSourcesCompileTest.classPath(entry -> true), output);
        assertTrue(diagnostics.isEmpty(), "generated sources must compile:\n" + diagnostics);
        return new URLClassLoader(new URL[]{output.toUri().toURL()}, ContentBootstrapTest.class.getClassLoader());
    }

    /**
     * Creates {@code CygnusFormulas} with a generated interface implementation that has no interface functions to call.
     */
    private static Object formulas(URLClassLoader loader) throws ReflectiveOperationException {
        Class<?> functionsType = loader.loadClass(TestContents.PACKAGE + ".CygnusExpressionFunctions");
        Object functions = Proxy.newProxyInstance(loader, new Class<?>[]{functionsType}, (proxy, method, args) -> {
            throw new UnsupportedOperationException("the fixture formulas call no interface function: " + method);
        });
        return loader.loadClass(TestContents.PACKAGE + ".CygnusFormulas").getConstructor(functionsType).newInstance(functions);
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
