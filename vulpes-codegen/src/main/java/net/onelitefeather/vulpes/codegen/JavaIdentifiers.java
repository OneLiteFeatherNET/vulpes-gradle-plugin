package net.onelitefeather.vulpes.codegen;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.lang.model.SourceVersion;

/**
 * Turns keys into Java identifiers that compile.
 * <p>
 * Keys may contain characters that are illegal in Java, such as {@code -} or {@code .}, and may start with a digit.
 * Two keys that end up with the same identifier are rejected with a {@link CodegenException} that names both keys.
 */
final class JavaIdentifiers {

    private static final String UNDERSCORE = "_";

    private JavaIdentifiers() {
    }

    /**
     * Returns a constant name for the given text, for example {@code my-item.v2} becomes {@code MY_ITEM_V2}.
     *
     * @throws CodegenException if the text contains no letter or digit
     */
    static String constantName(String text) {
        String identifier = text.toUpperCase(Locale.ROOT).replaceAll("[^A-Za-z0-9_]", UNDERSCORE)
                .replaceAll("_+", UNDERSCORE);
        if (identifier.replace(UNDERSCORE, "").isEmpty()) {
            throw new CodegenException("key '" + text + "' contains no letter or digit and cannot become a Java identifier");
        }
        if (Character.isDigit(identifier.charAt(0))) {
            identifier = UNDERSCORE + identifier;
        }
        if (SourceVersion.isKeyword(identifier)) {
            identifier = identifier + UNDERSCORE;
        }
        return identifier;
    }

    /**
     * Tracks the identifiers of one generated class and rejects two keys that map to the same identifier.
     */
    static final class Claims {

        private final Map<String, String> keyByIdentifier = new HashMap<>();

        /**
         * Claims the identifier for the given key.
         *
         * @throws CodegenException if another key already owns the identifier
         */
        String claim(String identifier, String key) {
            String owner = keyByIdentifier.putIfAbsent(identifier, key);
            if (owner != null && !owner.equals(key)) {
                throw new CodegenException("keys '" + owner + "' and '" + key
                        + "' both map to the Java identifier '" + identifier + "'");
            }
            return identifier;
        }
    }
}
