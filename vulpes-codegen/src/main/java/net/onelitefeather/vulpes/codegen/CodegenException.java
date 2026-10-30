package net.onelitefeather.vulpes.codegen;

/**
 * Thrown when the content cannot be turned into Java sources, for example when two keys share one constant name.
 */
public final class CodegenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a message that names the offending keys or types.
     */
    public CodegenException(String message) {
        super(message);
    }
}
