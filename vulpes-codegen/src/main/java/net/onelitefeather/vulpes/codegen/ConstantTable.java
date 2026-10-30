package net.onelitefeather.vulpes.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Java constant names of the entries of one type, and the aliases of their renamed keys. Entries and aliases
 * share one set of claims, so no two keys of a type get the same constant name.
 */
final class ConstantTable {

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final List<Alias> aliases = new ArrayList<>();

    private ConstantTable() {
    }

    /**
     * Builds the table of the entries in the given order.
     *
     * @throws CodegenException if a key occurs twice, an alias collides with an entry or another alias, or two keys
     *                          share one constant name; the message names the keys
     */
    static ConstantTable of(String namespace, List<EntryKeys> keys) {
        ConstantTable table = new ConstantTable();
        JavaIdentifiers.Claims claims = new JavaIdentifiers.Claims();
        for (EntryKeys entry : keys) {
            if (table.entries.containsKey(entry.key())) {
                throw new CodegenException("key '" + entry.key() + "' occurs more than once");
            }
            String name = claims.claim(identifier(namespace, entry.key()), entry.key());
            table.entries.put(entry.key(), new Entry(entry.key(), name));
        }
        Map<String, Alias> aliases = new TreeMap<>();
        for (EntryKeys entry : keys) {
            for (String previous : entry.previousKeys()) {
                if (table.entries.containsKey(previous)) {
                    throw new CodegenException("alias '" + previous + "' collides with the entry of the same key");
                }
                Alias existing = aliases.get(previous);
                if (existing != null) {
                    throw new CodegenException("alias '" + previous + "' is declared by '" + existing.target().key()
                            + "' and by '" + entry.key() + "'");
                }
                String name = claims.claim(identifier(namespace, previous), previous);
                aliases.put(previous, new Alias(previous, name, table.entries.get(entry.key())));
            }
        }
        table.aliases.addAll(aliases.values());
        return table;
    }

    /**
     * The entry of the given key, or {@code null} if the table has none.
     */
    Entry entry(String key) {
        return entries.get(key);
    }

    /**
     * The entries in the order they were given.
     */
    Collection<Entry> entries() {
        return entries.values();
    }

    /**
     * The aliases sorted by their key.
     */
    List<Alias> aliases() {
        return aliases;
    }

    /**
     * The namespace part of a key {@code namespace:path}.
     *
     * @throws CodegenException if the key has no namespace
     */
    static String namespaceOf(String key) {
        int colon = key.indexOf(':');
        if (colon <= 0) {
            throw new CodegenException("key '" + key + "' has no namespace");
        }
        return key.substring(0, colon);
    }

    /**
     * The path part of a key {@code namespace:path}.
     *
     * @throws CodegenException if the key has no namespace
     */
    static String pathOf(String key) {
        namespaceOf(key);
        return key.substring(key.indexOf(':') + 1);
    }

    /**
     * The constant name of a key: the path, prefixed with the namespace unless the namespace is the given one.
     */
    private static String identifier(String namespace, String key) {
        String keyNamespace = namespaceOf(key);
        String path = pathOf(key);
        return JavaIdentifiers.constantName(keyNamespace.equals(namespace) ? path : keyNamespace + "_" + path);
    }

    /**
     * A current entry with its constant name.
     *
     * @param key  the key of the entry
     * @param name the constant name
     */
    record Entry(String key, String name) {
    }

    /**
     * A deprecated alias for a renamed key, pointing to the current entry.
     *
     * @param key    the old key
     * @param name   the constant name of the alias
     * @param target the current entry
     */
    record Alias(String key, String name, Entry target) {
    }
}
