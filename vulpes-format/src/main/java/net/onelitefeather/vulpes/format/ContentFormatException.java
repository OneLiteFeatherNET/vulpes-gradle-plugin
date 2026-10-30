package net.onelitefeather.vulpes.format;

/**
 * Signals a data JAR or metadata file that does not match the format 1 contract.
 */
public final class ContentFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ContentFormatException(String message) {
        super(message);
    }

    public ContentFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
