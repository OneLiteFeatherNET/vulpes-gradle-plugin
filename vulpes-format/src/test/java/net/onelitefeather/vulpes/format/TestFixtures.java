package net.onelitefeather.vulpes.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class TestFixtures {

    private TestFixtures() {
    }

    static byte[] bytes(String resource) {
        try (InputStream in = TestFixtures.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + resource);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static JsonNode tree(byte[] json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static byte[] write(JsonNode node) {
        try {
            return new ObjectMapper().writeValueAsBytes(node);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
