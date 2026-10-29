package net.onelitefeather.vulpes.codegen;

/**
 * Java literals of generated values. Non-ASCII characters are written as unicode escapes, so the generated sources
 * compile whatever the encoding of the consuming build is.
 */
final class JavaLiterals {

    private JavaLiterals() {
    }

    /**
     * A Java string literal. Line breaks and tabs use their named escapes, since a unicode escape of a line
     * terminator would end the literal.
     */
    static String string(String value) {
        StringBuilder literal = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> literal.append("\\\"");
                case '\\' -> literal.append("\\\\");
                case '\b' -> literal.append("\\b");
                case '\t' -> literal.append("\\t");
                case '\n' -> literal.append("\\n");
                case '\f' -> literal.append("\\f");
                case '\r' -> literal.append("\\r");
                default -> {
                    if (c < 0x20 || c > 0x7e) {
                        literal.append(String.format("\\u%04x", (int) c));
                    } else {
                        literal.append(c);
                    }
                }
            }
        }
        return literal.append('"').toString();
    }

    /**
     * A double literal of a finite value.
     *
     * @throws CodegenException if the value is not finite
     */
    static String doubleValue(double value) {
        if (!Double.isFinite(value)) {
            throw new CodegenException("value " + value + " is not finite and has no Java literal");
        }
        return Double.toString(value);
    }

    /**
     * A float literal of a value that is finite as float.
     *
     * @throws CodegenException if the value is not finite as float
     */
    static String floatValue(double value) {
        float converted = (float) value;
        if (!Float.isFinite(converted)) {
            throw new CodegenException("value " + value + " is not finite as float and has no Java literal");
        }
        return Float.toString(converted) + "f";
    }
}
