package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.onelitefeather.vulpes.format.Expression;
import net.onelitefeather.vulpes.format.ExpressionFunctionKind;
import net.onelitefeather.vulpes.format.ExpressionParameter;
import net.onelitefeather.vulpes.format.ExpressionType;
import net.onelitefeather.vulpes.format.Snapshot;
import org.junit.jupiter.api.Test;

final class FormulaGeneratorTest {

    @Test
    void formulaFunctionBecomesTypedMethodEvaluatedByEngine() {
        String source = generate(TestContents.snapshot()
                .function(TestContents.formula("armorDamage", ExpressionType.NUMBER, "base * (1 - armor / 100)",
                        TestContents.parameter("base", ExpressionType.NUMBER),
                        TestContents.parameter("armor", ExpressionType.NUMBER)))
                .build());

        assertTrue(source.contains("public BigDecimal armorDamage(BigDecimal base, BigDecimal armor) {"),
                "typed method of the formula:\n" + source);
        assertTrue(source.contains("return engine.number(\"armorDamage\", base, armor);"),
                "the method evaluates the formula by its identifier:\n" + source);
        assertTrue(source.contains("new Formula(\"armorDamage\", List.of(\"base\", \"armor\"), "
                + "\"base * (1 - armor / 100)\")"), "the formula source is registered with its parameters:\n" + source);
    }

    @Test
    void expressionBecomesTypedMethodWithItsVariables() {
        String source = generate(TestContents.snapshot()
                .expression(TestContents.expression("maxHealth", ExpressionType.NUMBER, "20 + level * armorDamage(2, 0)",
                        TestContents.parameter("level", ExpressionType.NUMBER)))
                .build());

        assertTrue(source.contains("public BigDecimal maxHealth(BigDecimal level) {"),
                "typed method of the expression:\n" + source);
        assertTrue(source.contains("return engine.number(\"maxHealth\", level);"), "evaluates the expression:\n" + source);
        assertTrue(source.contains("new Formula(\"maxHealth\", List.of(\"level\"), \"20 + level * armorDamage(2, 0)\")"),
                "the expression is registered as an expression:\n" + source);
    }

    @Test
    void booleanAndStringResultsUseBoolAndString() {
        String source = generate(TestContents.snapshot()
                .expression(TestContents.expression("isAlive", ExpressionType.BOOLEAN, "health > 0"))
                .expression(TestContents.expression("label", ExpressionType.STRING, "\"Hi\""))
                .build());

        assertTrue(source.contains("public boolean isAlive() {") && source.contains("return engine.bool(\"isAlive\");"),
                "boolean result:\n" + source);
        assertTrue(source.contains("public String label() {") && source.contains("return engine.string(\"label\");"),
                "string result:\n" + source);
    }

    @Test
    void renamedExpressionKeepsDeprecatedMethodCallingCurrentMethod() {
        Expression renamed = new Expression(TestContents.id("maxHealth"), List.of("oldHealth"), "maxHealth", null,
                List.of(TestContents.parameter("level", ExpressionType.NUMBER)), ExpressionType.NUMBER, "20 + level");

        String source = generate(TestContents.snapshot().expression(renamed).build());

        assertTrue(source.contains("@Deprecated(") && source.contains("forRemoval = true"),
                "the old name is deprecated for removal:\n" + source);
        assertTrue(source.contains("public BigDecimal oldHealth(BigDecimal level) {"), "deprecated method:\n" + source);
        assertTrue(source.contains("return maxHealth(level);"), "the deprecated method delegates:\n" + source);
    }

    @Test
    void constructorTakesExpressionFunctionsWhenProjectHasInterfaceFunctions() {
        String source = generate(TestContents.snapshot()
                .function(TestContents.function("inArena", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN,
                        TestContents.parameter("arena", ExpressionType.STRING)))
                .expression(TestContents.expression("maxHealth", ExpressionType.NUMBER, "20"))
                .build());

        assertTrue(source.contains("public CygnusFormulas(CygnusExpressionFunctions functions) {"),
                "constructor takes the interface:\n" + source);
        assertTrue(source.contains("\"inArena\", new NativeSignature(1, arguments -> functions.inArena("
                + "(String) arguments.get(0)))"), "native function calls the interface method:\n" + source);
    }

    @Test
    void constructorHasNoArgumentsWithoutInterfaceFunctions() {
        String source = generate(TestContents.snapshot()
                .expression(TestContents.expression("maxHealth", ExpressionType.NUMBER, "20"))
                .build());

        assertTrue(source.contains("public CygnusFormulas() {"), "constructor without arguments:\n" + source);
        assertFalse(source.contains("ExpressionFunctions"), "no interface without interface functions:\n" + source);
    }

    @Test
    void interfaceFunctionContributesNoTypedMethod() {
        String source = generate(TestContents.snapshot()
                .function(TestContents.function("inArena", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN))
                .function(TestContents.formula("doubleIt", ExpressionType.NUMBER, "x * 2",
                        TestContents.parameter("x", ExpressionType.NUMBER)))
                .build());

        assertFalse(source.contains("public boolean inArena("), "the game implements the interface function:\n" + source);
    }

    @Test
    void noClassWithoutFormulaFunctionOrExpression() {
        Optional<GeneratedSource> source = FormulaGenerator.generate(TestContents.CONTEXT, TestContents.snapshot()
                .function(TestContents.function("inArena", ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN))
                .build());

        assertTrue(source.isEmpty(), "no formulas class without formulas or expressions");
    }

    @Test
    void classIsNamedAfterProject() {
        GeneratedSource source = FormulaGenerator.generate(TestContents.CONTEXT, TestContents.snapshot()
                .expression(TestContents.expression("maxHealth", ExpressionType.NUMBER, "20"))
                .build()).orElseThrow();

        assertEquals("CygnusFormulas", source.className(), "class name");
    }

    @Test
    void nativesUseMapOfEntriesAboveTenFunctions() {
        TestContents.Builder builder = TestContents.snapshot();
        for (int i = 0; i < 11; i++) {
            builder.function(TestContents.function("check" + i, ExpressionFunctionKind.INTERFACE, ExpressionType.BOOLEAN));
        }

        String source = generate(builder.expression(TestContents.expression("maxHealth", ExpressionType.NUMBER, "20"))
                .build());

        assertTrue(source.contains("Map.ofEntries("), "more than ten natives need Map.ofEntries:\n" + source);
    }

    @Test
    void methodNameCollisionFailsWithCodegenException() {
        Snapshot snapshot = TestContents.snapshot()
                .expression(TestContents.expression("a_b", ExpressionType.NUMBER, "1"))
                .expression(TestContents.expression("aB", ExpressionType.NUMBER, "2"))
                .build();

        assertThrows(CodegenException.class, () -> FormulaGenerator.generate(TestContents.CONTEXT, snapshot),
                "two expressions with one method name must fail");
    }

    @Test
    void formulaWithoutSourceFailsWithCodegenException() {
        Snapshot snapshot = TestContents.snapshot()
                .function(TestContents.function("doubleIt", ExpressionFunctionKind.FORMULA, ExpressionType.NUMBER,
                        TestContents.parameter("x", ExpressionType.NUMBER)))
                .build();

        assertThrows(CodegenException.class, () -> FormulaGenerator.generate(TestContents.CONTEXT, snapshot),
                "a formula without source cannot be evaluated");
    }

    @Test
    void methodsAreSortedByNameIgnoringCase() {
        String source = generate(TestContents.snapshot()
                .expression(TestContents.expression("zeta", ExpressionType.NUMBER, "1"))
                .expression(TestContents.expression("Alpha", ExpressionType.NUMBER, "2"))
                .build());

        assertTrue(source.indexOf("alpha(") >= 0 && source.indexOf("alpha(") < source.indexOf("zeta("),
                "methods sorted ignoring case:\n" + source);
    }

    @Test
    void keywordParameterNameGetsSuffix() {
        String source = generate(TestContents.snapshot()
                .expression(TestContents.expression("check", ExpressionType.BOOLEAN, "true",
                        TestContents.parameter("class", ExpressionType.STRING)))
                .build());

        assertTrue(source.contains("public boolean check(String class_) {"), "parameter keyword suffix:\n" + source);
    }

    private static String generate(Snapshot snapshot) {
        return FormulaGenerator.generate(TestContents.CONTEXT, snapshot)
                .orElseThrow(() -> new AssertionError("expected a formulas class")).source();
    }
}
