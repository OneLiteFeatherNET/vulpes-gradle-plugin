package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * A named formula of the project that is evaluated with its variables and has a return type.
 */
public record Expression(UUID id, List<String> previousNames, String name, @Nullable String description,
                         List<ExpressionParameter> variables, ExpressionType returnType, String expression) {

    public Expression {
        Objects.requireNonNull(id, "id");
        previousNames = List.copyOf(previousNames);
        Objects.requireNonNull(name, "name");
        variables = List.copyOf(variables);
        Objects.requireNonNull(returnType, "returnType");
        Objects.requireNonNull(expression, "expression");
    }
}
