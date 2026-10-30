package net.onelitefeather.vulpes.codegen;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import net.onelitefeather.vulpes.format.Expression;
import net.onelitefeather.vulpes.format.ExpressionFunction;
import net.onelitefeather.vulpes.format.ExpressionFunctionKind;
import net.onelitefeather.vulpes.format.ExpressionParameter;
import net.onelitefeather.vulpes.format.ExpressionType;
import net.onelitefeather.vulpes.format.Snapshot;

/**
 * Writes {@code <Project>Formulas}, which evaluates the formula functions and the expressions of a project with the
 * {@code ExpressionEngine} of vulpes-base. Interface functions are called through the constructor argument.
 */
final class FormulaGenerator {

    private static final String ENGINE_PACKAGE = "net.onelitefeather.vulpes.expression";
    private static final ClassName ENGINE = ClassName.get(ENGINE_PACKAGE, "ExpressionEngine");
    private static final ClassName FORMULA = ClassName.get(ENGINE_PACKAGE, "Formula");
    private static final ClassName NATIVE_SIGNATURE = ClassName.get(ENGINE_PACKAGE, "NativeSignature");
    private static final ClassName LIST = ClassName.get(List.class);
    private static final ClassName MAP = ClassName.get(Map.class);
    private static final int MAX_MAP_OF_PAIRS = 10;
    private static final Comparator<Entry> BY_NAME = Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER);

    private FormulaGenerator() {
    }

    /**
     * Generates {@code <Project>Formulas}, with the methods sorted by name ignoring case and the deprecated methods of
     * a renamed entry after its current method.
     *
     * @return the class, or empty if the project has no formula function and no expression
     * @throws CodegenException if a formula function has no source or two methods map to one name
     */
    static Optional<GeneratedSource> generate(GenerationContext context, Snapshot snapshot) {
        List<Entry> formulas = snapshot.expressionFunctions().stream()
                .filter(function -> function.kind() == ExpressionFunctionKind.FORMULA)
                .map(FormulaGenerator::formulaEntry)
                .sorted(BY_NAME)
                .toList();
        List<Entry> expressions = snapshot.expressions().stream().map(FormulaGenerator::expressionEntry).sorted(BY_NAME)
                .toList();
        if (formulas.isEmpty() && expressions.isEmpty()) {
            return Optional.empty();
        }
        List<ExpressionFunction> interfaces = snapshot.expressionFunctions().stream()
                .filter(function -> function.kind() == ExpressionFunctionKind.INTERFACE)
                .sorted(Comparator.comparing(ExpressionFunction::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        if (!interfaces.isEmpty()) {
            constructor.addParameter(context.className("ExpressionFunctions"), "functions");
        }
        constructor.addCode(initialization(formulas, expressions, interfaces));

        TypeSpec.Builder type = TypeSpec.classBuilder(context.className("Formulas").simpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(JavaSources.classJavadoc(context, "Formulas of the Vulpes content project {@code "
                        + context.project() + "}, evaluated by the expression engine of vulpes-base."))
                .addField(FieldSpec.builder(ENGINE, "engine", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(constructor.build());
        JavaIdentifiers.Claims methods = new JavaIdentifiers.Claims();
        List<Entry> all = new ArrayList<>(formulas);
        all.addAll(expressions);
        all.sort(BY_NAME);
        for (Entry entry : all) {
            methods.claim(ExpressionFunctionGenerator.methodName(entry.name()), entry.name());
            type.addMethod(method(entry));
            for (String previous : entry.previousNames()) {
                methods.claim(ExpressionFunctionGenerator.methodName(previous),
                        "previous name '" + previous + "' of '" + entry.name() + "'");
                type.addMethod(deprecatedMethod(entry, previous));
            }
        }
        return Optional.of(JavaSources.render(context.packageName(), type.build()));
    }

    private static Entry formulaEntry(ExpressionFunction function) {
        if (function.expression() == null) {
            throw new CodegenException("formula function '" + function.name() + "' has no expression");
        }
        return new Entry(function.name(), function.description(), function.parameters(), function.returnType(),
                function.expression(), function.previousNames());
    }

    private static Entry expressionEntry(Expression expression) {
        return new Entry(expression.name(), expression.description(), expression.variables(), expression.returnType(),
                expression.expression(), expression.previousNames());
    }

    private static CodeBlock initialization(List<Entry> formulas, List<Entry> expressions,
                                            List<ExpressionFunction> interfaces) {
        return CodeBlock.builder()
                .add("this.engine = $T.of(\n$>", ENGINE)
                .add("$L,\n", list(formulas.stream().map(FormulaGenerator::formulaCode).toList()))
                .add("$L,\n", list(expressions.stream().map(FormulaGenerator::formulaCode).toList()))
                .add("$L$<);\n", natives(interfaces))
                .build();
    }

    private static CodeBlock formulaCode(Entry entry) {
        List<CodeBlock> parameters = entry.parameters().stream()
                .map(parameter -> CodeBlock.of("$S", parameter.name()))
                .toList();
        return CodeBlock.of("new $T($S, $L, $S)", FORMULA, entry.name(), list(parameters), entry.source());
    }

    /**
     * The natives of the interface functions. Each native casts its arguments to the Java types of the parameters.
     */
    private static CodeBlock natives(List<ExpressionFunction> interfaces) {
        List<CodeBlock> pairs = new ArrayList<>();
        for (ExpressionFunction function : interfaces) {
            List<CodeBlock> casts = new ArrayList<>();
            for (int i = 0; i < function.parameters().size(); i++) {
                ExpressionParameter parameter = function.parameters().get(i);
                casts.add(CodeBlock.of("($T) arguments.get($L)",
                        ExpressionFunctionGenerator.javaType(parameter.type()).box(), i));
            }
            pairs.add(CodeBlock.of("$S, new $T($L, arguments -> functions.$L($L))", function.name(), NATIVE_SIGNATURE,
                    function.parameters().size(), ExpressionFunctionGenerator.methodName(function.name()),
                    CodeBlock.join(casts, ", ")));
        }
        if (pairs.size() <= MAX_MAP_OF_PAIRS) {
            return CodeBlock.builder().add("$T.of(", MAP).add(CodeBlock.join(pairs, ", ")).add(")").build();
        }
        List<CodeBlock> entries = pairs.stream().map(pair -> CodeBlock.of("$T.entry($L)", MAP, pair)).toList();
        return CodeBlock.builder().add("$T.ofEntries(", MAP).add(CodeBlock.join(entries, ", ")).add(")").build();
    }

    private static CodeBlock list(List<CodeBlock> items) {
        if (items.isEmpty()) {
            return CodeBlock.of("$T.of()", LIST);
        }
        return CodeBlock.builder().add("$T.of(", LIST).add(CodeBlock.join(items, ", ")).add(")").build();
    }

    private static MethodSpec method(Entry entry) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(ExpressionFunctionGenerator.methodName(entry.name()))
                .addModifiers(Modifier.PUBLIC)
                .returns(ExpressionFunctionGenerator.javaType(entry.returnType()))
                .addJavadoc("$L", ExpressionFunctionGenerator.javadoc(entry.description(), entry.name()));
        addParameters(method, entry.parameters());
        method.addStatement("return engine.$L($S$L)", evaluation(entry.returnType()), entry.name(),
                arguments(entry.parameters()));
        return method.build();
    }

    private static MethodSpec deprecatedMethod(Entry entry, String previous) {
        String current = ExpressionFunctionGenerator.methodName(entry.name());
        MethodSpec.Builder method = MethodSpec.methodBuilder(ExpressionFunctionGenerator.methodName(previous))
                .addModifiers(Modifier.PUBLIC)
                .returns(ExpressionFunctionGenerator.javaType(entry.returnType()))
                .addAnnotation(AnnotationSpec.builder(Deprecated.class).addMember("forRemoval", "true").build())
                .addJavadoc("@deprecated renamed to {@link #$L}, removed with the next major version", current);
        addParameters(method, entry.parameters());
        method.addStatement("return $L($L)", current, CodeBlock.join(entry.parameters().stream()
                .map(parameter -> CodeBlock.of("$L", ExpressionFunctionGenerator.parameterName(parameter.name())))
                .toList(), ", "));
        return method.build();
    }

    private static void addParameters(MethodSpec.Builder method, List<ExpressionParameter> parameters) {
        for (ExpressionParameter parameter : parameters) {
            method.addParameter(ParameterSpec.builder(ExpressionFunctionGenerator.javaType(parameter.type()),
                    ExpressionFunctionGenerator.parameterName(parameter.name())).build());
        }
    }

    private static CodeBlock arguments(List<ExpressionParameter> parameters) {
        CodeBlock.Builder arguments = CodeBlock.builder();
        for (ExpressionParameter parameter : parameters) {
            arguments.add(", $L", ExpressionFunctionGenerator.parameterName(parameter.name()));
        }
        return arguments.build();
    }

    private static String evaluation(ExpressionType type) {
        return switch (type) {
            case NUMBER -> "number";
            case BOOLEAN -> "bool";
            case STRING -> "string";
        };
    }

    /**
     * A formula function or an expression, reduced to what its method and its engine registration need.
     */
    private record Entry(String name, String description, List<ExpressionParameter> parameters,
                         ExpressionType returnType, String source, List<String> previousNames) {
    }
}
