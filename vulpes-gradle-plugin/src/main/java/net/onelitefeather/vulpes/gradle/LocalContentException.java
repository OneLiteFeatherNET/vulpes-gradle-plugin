package net.onelitefeather.vulpes.gradle;

/**
 * Signals local content that cannot be used: a partial directory, a failing check or a forbidden local override.
 */
final class LocalContentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    LocalContentException(String message) {
        super(message);
    }

    LocalContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
