package net.onelitefeather.vulpes.format;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * A project-defined expression function.
 */
public record ExpressionFunction(UUID id, List<String> previousNames, String name, ExpressionFunctionKind kind,
                                 @Nullable String description, List<ExpressionParameter> parameters,
                                 ExpressionType returnType, @Nullable String expression) {

    public ExpressionFunction {
        Objects.requireNonNull(id, "id");
        previousNames = List.copyOf(previousNames);
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        parameters = List.copyOf(parameters);
        Objects.requireNonNull(returnType, "returnType");
    }
}
