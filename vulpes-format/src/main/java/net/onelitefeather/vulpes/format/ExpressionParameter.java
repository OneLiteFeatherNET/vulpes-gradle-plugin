package net.onelitefeather.vulpes.format;

import java.util.Objects;

/**
 * A parameter of an expression function.
 */
public record ExpressionParameter(String name, ExpressionType type) {

    public ExpressionParameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}
