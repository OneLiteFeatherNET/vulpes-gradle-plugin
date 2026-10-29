package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.onelitefeather.vulpes.format.ExpressionFunction;
import net.onelitefeather.vulpes.format.ExpressionFunctionKind;
import net.onelitefeather.vulpes.format.ExpressionType;
import org.junit.jupiter.api.Test;

final class ExpressionFunctionGeneratorTest {

    @Test
    void interfaceFunctionBecomesAbstractMethodWithMappedTypes() {
        ExpressionFunction damage = TestContents.function("damage_bonus", ExpressionFunctionKind.INTERFACE,
                ExpressionType.NUMBER, TestContents.parameter("attack", ExpressionType.NUMBER),
                TestContents.parameter("flag", ExpressionType.BOOLEAN));

        String source = generate(damage);

        assertTrue(source.contains("    BigDecimal damageBonus(BigDecimal attack, boolean flag);"),
                "NUMBER maps to BigDecimal, BOOLEAN to boolean:\n" + source);
    }

    @Test
    void stringMapsToString() {
        String source = generate(TestContents.function("label", ExpressionFunctionKind.INTERFACE, ExpressionType.STRING,
                TestContents.parameter("text", ExpressionType.STRING)));

        assertTrue(source.contains("    String label(String text);"), "STRING maps to String:\n" + source);
    }

    @Test
    void formulaFunctionGetsNoMethod() {
        String source = generate(TestContents.function("double_it", ExpressionFunctionKind.FORMULA, ExpressionType.NUMBER,
                TestContents.parameter("x", ExpressionType.NUMBER)), TestContents.function("in_arena",
                ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN));

        assertFalse(source.contains("doubleIt"), "a formula is implemented by the backend:\n" + source);
        assertTrue(source.contains("boolean inArena();"), "the interface function remains:\n" + source);
    }

    @Test
    void noInterfaceFunctionsGenerateNoInterface() {
        Optional<GeneratedSource> source = ExpressionFunctionGenerator.generate(TestContents.CONTEXT, List.of(
                TestContents.function("double_it", ExpressionFunctionKind.FORMULA, ExpressionType.NUMBER)));

        assertTrue(source.isEmpty(), "no interface without interface functions");
    }

    @Test
    void interfaceIsNamedAfterProjectAndDeclaresNoBigDecimalImportWithoutNumbers() {
        String source = generate(TestContents.function("is_ready", ExpressionFunctionKind.INTERFACE,
                ExpressionType.BOOLEAN, TestContents.parameter("count", ExpressionType.BOOLEAN)));

        assertTrue(source.contains("public interface CygnusExpressionFunctions {"), "interface name:\n" + source);
        assertFalse(source.contains("BigDecimal"), "BigDecimal only when used:\n" + source);
    }

    @Test
    void numberTypesImportBigDecimal() {
        String source = generate(TestContents.function("damage", ExpressionFunctionKind.INTERFACE, ExpressionType.NUMBER));

        assertTrue(source.contains("import java.math.BigDecimal;"), "BigDecimal import:\n" + source);
    }

    @Test
    void descriptionBecomesJavadocWithFunctionIdentifier() {
        ExpressionFunction damage = new ExpressionFunction(TestContents.id("damage"), List.of(), "damage_bonus",
                ExpressionFunctionKind.INTERFACE, "Bonus from gear.", List.of(), ExpressionType.NUMBER, null);

        String source = generate(damage);

        assertTrue(source.contains("     * Bonus from gear."), "description as javadoc:\n" + source);
        assertTrue(source.contains("     * <p>Formula identifier: {@code damage_bonus}"), "identifier:\n" + source);
    }

    @Test
    void commentEndAndTagMarkerCannotCloseOrTagJavadoc() {
        ExpressionFunction damage = new ExpressionFunction(TestContents.id("damage"), List.of(), "damage",
                ExpressionFunctionKind.INTERFACE, "Ends */ here, @param x", List.of(), ExpressionType.NUMBER, null);

        String source = generate(damage);

        assertFalse(source.contains("Ends */"), "the comment end must be escaped:\n" + source);
        assertTrue(source.contains("Ends *&#47; here, &#64;param x"), "escaped text:\n" + source);
    }

    @Test
    void methodNamesAreCamelCaseFromWordsAndKeepCamelCaseNames() {
        assertTrue(generate(TestContents.function("HIT_COUNT", ExpressionFunctionKind.INTERFACE, ExpressionType.NUMBER))
                .contains("BigDecimal hitCount();"), "upper case words become camel case");
        assertTrue(generate(TestContents.function("inArena", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN))
                .contains("boolean inArena();"), "a camel case name is kept");
        assertTrue(generate(TestContents.function("class", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN))
                .contains("boolean classFunction();"), "a Java keyword gets a suffix");
    }

    @Test
    void methodsSortedByNameIgnoringCase() {
        String source = generate(TestContents.function("zeta", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN),
                TestContents.function("Alpha", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN));

        assertTrue(source.indexOf("alpha()") >= 0 && source.indexOf("alpha()") < source.indexOf("zeta()"),
                "methods sorted ignoring case:\n" + source);
    }

    @Test
    void keywordParameterNameGetsSuffix() {
        String source = generate(TestContents.function("check", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN,
                TestContents.parameter("class", ExpressionType.STRING)));

        assertTrue(source.contains("boolean check(String class_);"), "parameter keyword suffix:\n" + source);
    }

    @Test
    void methodNamesThatCollideFail() {
        assertThrows(CodegenException.class, () -> generate(
                TestContents.function("a_b", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN),
                TestContents.function("aB", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN)),
                "two functions with one method name must fail");
    }

    @Test
    void classNameIsFixedToProject() {
        assertEquals("CygnusExpressionFunctions", ExpressionFunctionGenerator.generate(TestContents.CONTEXT, List.of(
                TestContents.function("x", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN))).orElseThrow()
                .className(), "class name");
    }

    private static String generate(ExpressionFunction... functions) {
        return ExpressionFunctionGenerator.generate(TestContents.CONTEXT, List.of(functions))
                .orElseThrow(() -> new AssertionError("expected an interface")).source();
    }
}
