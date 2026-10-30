package net.onelitefeather.vulpes.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Typed, strict access to one JSON object. Every property must be read; {@link #finish()} rejects the rest.
 */
final class JsonObject {

    private static final ObjectMapper NODES = new ObjectMapper();
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @FunctionalInterface
    interface Mapper<T> {
        T map(JsonObject object);
    }

    private final String source;
    private final String file;
    private final String path;
    private final ObjectNode node;
    private final Set<String> consumed = new HashSet<>();

    JsonObject(String source, String file, String path, ObjectNode node) {
        this.source = source;
        this.file = file;
        this.path = path;
        this.node = node;
    }

    String string(String name) {
        return text(childPath(name), value(name, false));
    }

    String nullableString(String name) {
        JsonNode value = value(name, true);
        return value == null ? null : text(childPath(name), value);
    }

    int integer(String name) {
        return intValue(childPath(name), value(name, false));
    }

    Integer nullableInteger(String name) {
        JsonNode value = value(name, true);
        return value == null ? null : intValue(childPath(name), value);
    }

    long longValue(String name) {
        String at = childPath(name);
        JsonNode value = value(name, false);
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw failAt(at, "expected integer, found " + kind(value));
        }
        return value.longValue();
    }

    double number(String name) {
        String at = childPath(name);
        JsonNode value = value(name, false);
        if (!value.isNumber()) {
            throw failAt(at, "expected number, found " + kind(value));
        }
        return value.doubleValue();
    }

    boolean bool(String name) {
        String at = childPath(name);
        JsonNode value = value(name, false);
        if (!value.isBoolean()) {
            throw failAt(at, "expected boolean, found " + kind(value));
        }
        return value.booleanValue();
    }

    UUID uuid(String name) {
        String at = childPath(name);
        String text = text(at, value(name, false));
        if (!UUID_PATTERN.matcher(text).matches()) {
            throw failAt(at, "expected UUID, found " + text);
        }
        return UUID.fromString(text);
    }

    Instant instant(String name) {
        String at = childPath(name);
        String text = text(at, value(name, false));
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            throw failAt(at, "expected ISO-8601 instant, found " + text);
        }
    }

    List<String> stringList(String name) {
        ArrayNode array = array(name);
        List<String> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            result.add(text(elementPath(name, i), array.get(i)));
        }
        return result;
    }

    List<Integer> intList(String name) {
        ArrayNode array = array(name);
        List<Integer> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            result.add(intValue(elementPath(name, i), array.get(i)));
        }
        return result;
    }

    <T> List<T> objectList(String name, Mapper<T> mapper) {
        ArrayNode array = array(name);
        List<T> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            result.add(mapChild(elementPath(name, i), array.get(i), mapper));
        }
        return result;
    }

    <T> T object(String name, Mapper<T> mapper) {
        return mapChild(childPath(name), value(name, false), mapper);
    }

    <E extends Enum<E>> E enumValue(String name, Class<E> type) {
        String at = childPath(name);
        String text = text(at, value(name, false));
        for (E constant : type.getEnumConstants()) {
            if (constant.name().toLowerCase(Locale.ROOT).equals(text)) {
                return constant;
            }
        }
        List<String> allowed = Arrays.stream(type.getEnumConstants())
                .map(constant -> constant.name().toLowerCase(Locale.ROOT))
                .toList();
        throw failAt(at, "expected one of " + allowed + ", found " + text);
    }

    /** Compact JSON text of any value, object keys sorted. */
    String rawJson(String name) {
        return canonical(value(name, false)).toString();
    }

    SortedMap<String, String> rawJsonMap(String name) {
        String at = childPath(name);
        JsonNode value = value(name, false);
        if (!value.isObject()) {
            throw failAt(at, "expected object, found " + kind(value));
        }
        SortedMap<String, String> result = new TreeMap<>();
        value.properties().forEach(entry -> result.put(entry.getKey(), canonical(entry.getValue()).toString()));
        return Collections.unmodifiableSortedMap(result);
    }

    /** Rejects every property that was not read. */
    void finish() {
        node.fieldNames().forEachRemaining(name -> {
            if (!consumed.contains(name)) {
                throw failAt(childPath(name), "unknown property");
            }
        });
    }

    private JsonNode value(String name, boolean nullable) {
        consumed.add(name);
        JsonNode value = node.get(name);
        if (value == null) {
            throw failAt(childPath(name), "missing property");
        }
        if (value.isNull()) {
            if (nullable) {
                return null;
            }
            throw failAt(childPath(name), "must not be null");
        }
        return value;
    }

    private ArrayNode array(String name) {
        String at = childPath(name);
        JsonNode value = value(name, false);
        if (!(value instanceof ArrayNode array)) {
            throw failAt(at, "expected array, found " + kind(value));
        }
        return array;
    }

    private <T> T mapChild(String at, JsonNode value, Mapper<T> mapper) {
        if (!(value instanceof ObjectNode child)) {
            throw failAt(at, "expected object, found " + kind(value));
        }
        JsonObject object = new JsonObject(source, file, at, child);
        T result = mapper.map(object);
        object.finish();
        return result;
    }

    private String text(String at, JsonNode value) {
        if (!value.isTextual()) {
            throw failAt(at, "expected string, found " + kind(value));
        }
        return value.textValue();
    }

    private int intValue(String at, JsonNode value) {
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw failAt(at, "expected integer, found " + kind(value));
        }
        return value.intValue();
    }

    private String childPath(String name) {
        return path + "." + name;
    }

    private String elementPath(String name, int index) {
        return childPath(name) + "[" + index + "]";
    }

    private ContentFormatException failAt(String at, String problem) {
        return new ContentFormatException(source + ": " + file + ": " + at + ": " + problem);
    }

    private static String kind(JsonNode value) {
        if (value.isNull()) {
            return "null";
        }
        if (value.isTextual()) {
            return "string";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isNumber()) {
            return "number";
        }
        if (value.isArray()) {
            return "array";
        }
        if (value.isObject()) {
            return "object";
        }
        return "value";
    }

    private static JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            SortedMap<String, JsonNode> fields = new TreeMap<>();
            node.properties().forEach(entry -> fields.put(entry.getKey(), entry.getValue()));
            ObjectNode result = NODES.createObjectNode();
            fields.forEach((key, value) -> result.set(key, canonical(value)));
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = NODES.createArrayNode();
            node.forEach(element -> result.add(canonical(element)));
            return result;
        }
        return node;
    }
}
