package net.onelitefeather.vulpes.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import net.onelitefeather.vulpes.format.ExpressionFunction;
import net.onelitefeather.vulpes.format.ExpressionFunctionKind;
import net.onelitefeather.vulpes.format.ExpressionType;

/**
 * Writes the interface the game implements for the expression functions of kind {@code interface}. Formula functions
 * are evaluated from their expression and get no method.
 */
final class ExpressionFunctionGenerator {

    private ExpressionFunctionGenerator() {
    }

    /**
     * Generates {@code <Project>ExpressionFunctions} with one method per interface function, sorted by name ignoring
     * case.
     *
     * @return the interface, or empty if the project has no interface function
     * @throws CodegenException if two functions map to one method name
     */
    static Optional<GeneratedSource> generate(GenerationContext context, List<ExpressionFunction> functions) {
        List<ExpressionFunction> interfaces = functions.stream()
                .filter(function -> function.kind() == ExpressionFunctionKind.INTERFACE)
                .sorted(Comparator.comparing(ExpressionFunction::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (interfaces.isEmpty()) {
            return Optional.empty();
        }
        TypeSpec.Builder type = TypeSpec.interfaceBuilder(context.prefix() + "ExpressionFunctions")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(JavaSources.classJavadoc(context, "Runtime implementations of the interface functions of "
                        + "the Vulpes content project {@code " + context.project() + "}."));
        JavaIdentifiers.Claims methods = new JavaIdentifiers.Claims();
        for (ExpressionFunction function : interfaces) {
            methods.claim(methodName(function.name()), function.name());
            type.addMethod(method(function));
        }
        return Optional.of(JavaSources.render(context.packageName(), type.build()));
    }

    private static MethodSpec method(ExpressionFunction function) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodName(function.name()))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(javaType(function.returnType()));
        method.addJavadoc("$L", javadoc(function));
        for (var parameter : function.parameters()) {
            method.addParameter(ParameterSpec.builder(javaType(parameter.type()), parameterName(parameter.name())).build());
        }
        return method.build();
    }

    private static String javadoc(ExpressionFunction function) {
        StringBuilder text = new StringBuilder();
        if (function.description() != null && !function.description().isBlank()) {
            text.append(JavaSources.javadocText(function.description().strip())).append("\n\n");
        }
        return text.append("<p>Formula identifier: {@code ").append(JavaSources.javadocText(function.name()))
                .append("}").toString();
    }

    /**
     * The Java type that carries the values of an expression type.
     */
    static TypeName javaType(ExpressionType type) {
        return switch (type) {
            case NUMBER -> TypeName.get(BigDecimal.class);
            case BOOLEAN -> TypeName.BOOLEAN;
            case STRING -> ClassName.get(String.class);
        };
    }

    /**
     * Converts a function name to a camel case method name. Underscores separate words; a word written in upper case
     * is lower cased, every word after the first starts with a capital letter. A name that is a Java keyword gets the
     * suffix {@code Function}.
     */
    static String methodName(String functionName) {
        StringBuilder name = new StringBuilder();
        for (String word : functionName.split("_+")) {
            if (word.isEmpty()) {
                continue;
            }
            String part = word.equals(word.toUpperCase(Locale.ROOT)) ? word.toLowerCase(Locale.ROOT) : word;
            char first = name.isEmpty() ? Character.toLowerCase(part.charAt(0)) : Character.toUpperCase(part.charAt(0));
            name.append(first).append(part.substring(1));
        }
        String result = name.isEmpty() ? "function" : name.toString();
        if (Character.isDigit(result.charAt(0))) {
            result = "_" + result;
        }
        return SourceVersion.isKeyword(result) ? result + "Function" : result;
    }

    private static String parameterName(String name) {
        return SourceVersion.isKeyword(name) ? name + "_" : name;
    }
}
