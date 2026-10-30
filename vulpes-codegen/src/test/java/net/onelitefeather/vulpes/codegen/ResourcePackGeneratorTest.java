package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ResourcePackGeneratorTest {

    private static final String SHA1_16X = "3b1e5c0d9a8f7e6d5c4b3a291807f6e5d4c3b2a1";
    private static final String URL_16X = "https://cdn.onelitefeather.net/packs/cygnus/16x.zip";

    @Test
    void declaresInfoWithNameBasedIdUriAndHash() {
        String id = UUID.nameUUIDFromBytes(SHA1_16X.getBytes(UTF_8)).toString();

        String source = ResourcePackGenerator.generate(TestContents.CONTEXT, List.of(TestContents.pack("16x", 16,
                URL_16X, SHA1_16X))).source();

        assertTrue(source.contains("public static final ResourcePackInfo PACK_16X = ResourcePackInfo.resourcePackInfo("
                + "UUID.fromString(\"" + id + "\"), URI.create(\"" + URL_16X + "\"), \"" + SHA1_16X + "\");"),
                "pack info:\n" + source);
    }

    @Test
    void listsPacksInReleaseOrder() {
        String source = ResourcePackGenerator.generate(TestContents.CONTEXT, List.of(
                TestContents.pack("32x", 32, "https://x/32", "a07d"),
                TestContents.pack("16x", 16, "https://x/16", "3b1e"))).source();

        assertTrue(source.contains("public static final List<ResourcePackInfo> ALL = List.of(PACK_32X, PACK_16X);"),
                "ALL in release order:\n" + source);
    }

    @Test
    void emptyPacksHaveEmptyList() {
        String source = ResourcePackGenerator.generate(TestContents.CONTEXT, List.of()).source();

        assertTrue(source.contains("public static final List<ResourcePackInfo> ALL = List.of();"), "empty ALL:\n" + source);
    }
}
