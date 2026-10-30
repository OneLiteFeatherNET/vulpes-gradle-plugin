package net.onelitefeather.vulpes.format.diff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Renders a {@link DiffReport} as JSON with sorted object keys.
 */
final class DiffJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private DiffJson() {
    }

    static String render(DiffReport report) {
        try {
            return MAPPER.writeValueAsString(report) + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot render diff report as JSON", e);
        }
    }
}
