package net.onelitefeather.vulpes.format;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads content data JARs and their {@code META-INF/vulpes} metadata in format 1.
 */
public final class ContentReader {

    /** The only format version this reader understands. */
    public static final int SUPPORTED_FORMAT = 1;

    private static final String RELEASE_FILE = "release.json";
    private static final String SNAPSHOT_FILE = "snapshot.json";
    private static final String RELEASE_ENTRY = "META-INF/vulpes/" + RELEASE_FILE;
    private static final String SNAPSHOT_ENTRY = "META-INF/vulpes/" + SNAPSHOT_FILE;
    private static final ObjectMapper MAPPER = new ObjectMapper(
            JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());

    private ContentReader() {
    }

    /**
     * Reads a data JAR.
     *
     * @throws ContentFormatException if the JAR is unreadable or its metadata does not match format 1
     */
    public static ContentRelease read(Path jar) {
        String source = jar.getFileName().toString();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            byte[] release = entry(zip, source, RELEASE_ENTRY);
            byte[] snapshot = entry(zip, source, SNAPSHOT_ENTRY);
            return read(source, release, snapshot);
        } catch (IOException e) {
            throw new ContentFormatException(source + ": cannot read data JAR: " + e.getMessage(), e);
        }
    }

    /**
     * Reads the two metadata files.
     *
     * @param source name of the origin, used in error messages (e.g. the JAR file name)
     * @throws ContentFormatException if either file does not match format 1 or the revision does not match
     */
    public static ContentRelease read(String source, byte[] releaseJson, byte[] snapshotJson) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(releaseJson, "releaseJson");
        Objects.requireNonNull(snapshotJson, "snapshotJson");

        ObjectNode releaseNode = parse(source, RELEASE_FILE, releaseJson);
        checkFormat(source, RELEASE_FILE, releaseNode);
        Release release = map(source, RELEASE_FILE, releaseNode, ContentReader::release);

        String actual = "sha256:" + sha256Hex(snapshotJson);
        if (!actual.equals(release.revision())) {
            throw new ContentFormatException(source + ": " + RELEASE_FILE + ": $.revision: revision does not match "
                    + SNAPSHOT_FILE + ", expected " + release.revision() + ", actual " + actual);
        }

        ObjectNode snapshotNode = parse(source, SNAPSHOT_FILE, snapshotJson);
        checkFormat(source, SNAPSHOT_FILE, snapshotNode);
        Snapshot snapshot = map(source, SNAPSHOT_FILE, snapshotNode, ContentReader::snapshot);
        checkExpressionNames(source, snapshot);
        return new ContentRelease(release, snapshot);
    }

    private static byte[] entry(ZipFile zip, String source, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new ContentFormatException(source + ": missing " + name);
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    private static ObjectNode parse(String source, String file, byte[] json) {
        JsonNode node;
        try {
            node = MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new ContentFormatException(source + ": " + file + ": invalid JSON: " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new ContentFormatException(source + ": " + file + ": cannot read JSON: " + e.getMessage(), e);
        }
        if (!(node instanceof ObjectNode object)) {
            throw new ContentFormatException(source + ": " + file + ": $: expected JSON object");
        }
        return object;
    }

    private static void checkFormat(String source, String file, ObjectNode root) {
        JsonNode format = root.get("format");
        if (format != null && format.isIntegralNumber() && format.canConvertToInt()
                && format.intValue() != SUPPORTED_FORMAT) {
            throw new ContentFormatException(source + ": " + file + ": unsupported format " + format.intValue()
                    + ", supported: " + SUPPORTED_FORMAT);
        }
    }

    private static <T> T map(String source, String file, ObjectNode node, JsonObject.Mapper<T> mapper) {
        JsonObject object = new JsonObject(source, file, "$", node);
        T result = mapper.map(object);
        object.finish();
        return result;
    }

    private static Release release(JsonObject o) {
        int format = o.integer("format");
        String project = o.string("project");
        UUID projectId = o.uuid("projectId");
        String version = o.string("version");
        String revision = o.string("revision");
        List<Pack> packs = o.objectList("packs", p -> new Pack(p.string("id"), p.integer("resolution"),
                p.string("url"), p.string("sha1"), p.longValue("size")));
        Provenance provenance = o.object("provenance", p -> new Provenance(p.string("generator"),
                p.string("createdBy"), p.instant("createdAt")));
        return new Release(format, project, projectId, version, revision, packs, provenance);
    }

    private static Snapshot snapshot(JsonObject o) {
        int format = o.integer("format");
        UUID projectId = o.uuid("projectId");
        String namespace = o.string("namespace");
        List<Item> items = o.objectList("items", ContentReader::item);
        List<Attribute> attributes = o.objectList("attributes", ContentReader::attribute);
        List<FontSymbol> fonts = o.objectList("fonts", ContentReader::font);
        List<SoundEvent> sounds = o.objectList("sounds", ContentReader::soundEvent);
        List<Advancement> advancements = o.objectList("advancements", ContentReader::advancement);
        List<ExpressionFunction> functions = o.objectList("expressionFunctions", ContentReader::expressionFunction);
        List<Expression> expressions = o.objectList("expressions", ContentReader::expression);
        List<Asset> assets = o.objectList("assets", ContentReader::asset);
        PackSettings pack = o.object("pack", ContentReader::packSettings);
        return new Snapshot(format, projectId, namespace, items, attributes, fonts, sounds, advancements, functions,
                expressions, assets, pack);
    }

    private static Item item(JsonObject o) {
        return new Item(o.uuid("id"), o.stringList("previousKeys"), o.string("key"), o.string("material"),
                o.integer("amount"), o.rawJsonMap("components"));
    }

    private static Attribute attribute(JsonObject o) {
        return new Attribute(o.uuid("id"), o.stringList("previousKeys"), o.string("key"), o.string("displayName"),
                o.nullableString("translationKey"), o.number("defaultValue"), o.number("minimumValue"),
                o.number("maximumValue"), o.bool("synced"));
    }

    private static FontSymbol font(JsonObject o) {
        return new FontSymbol(o.uuid("id"), o.stringList("previousKeys"), o.string("key"), o.string("font"),
                o.string("type"), o.string("file"), o.integer("ascent"), o.integer("height"), o.stringList("chars"));
    }

    private static SoundEvent soundEvent(JsonObject o) {
        return new SoundEvent(o.uuid("id"), o.stringList("previousKeys"), o.string("key"),
                o.nullableString("subtitle"), o.bool("replace"), o.objectList("sounds", ContentReader::soundEntry));
    }

    private static SoundEntry soundEntry(JsonObject o) {
        return new SoundEntry(o.string("name"), o.enumValue("type", SoundEntryType.class), o.number("volume"),
                o.number("pitch"), o.integer("weight"), o.bool("stream"), o.integer("attenuationDistance"),
                o.bool("preload"));
    }

    private static Advancement advancement(JsonObject o) {
        return new Advancement(o.uuid("id"), o.stringList("previousKeys"), o.string("key"),
                o.nullableString("parent"), o.string("icon"), o.enumValue("frame", AdvancementFrame.class),
                o.rawJson("title"), o.rawJson("description"), o.nullableString("background"), o.number("x"),
                o.number("y"), o.bool("showToast"), o.bool("hidden"), o.bool("announceToChat"));
    }

    private static ExpressionFunction expressionFunction(JsonObject o) {
        return new ExpressionFunction(o.uuid("id"), o.stringList("previousNames"), o.string("name"),
                o.enumValue("kind", ExpressionFunctionKind.class), o.nullableString("description"),
                o.objectList("parameters", ContentReader::expressionParameter),
                o.enumValue("returnType", ExpressionType.class), o.nullableString("expression"));
    }

    private static Expression expression(JsonObject o) {
        return new Expression(o.uuid("id"), o.stringList("previousNames"), o.string("name"),
                o.nullableString("description"), o.objectList("variables", ContentReader::expressionParameter),
                o.enumValue("returnType", ExpressionType.class), o.string("expression"));
    }

    private static ExpressionParameter expressionParameter(JsonObject o) {
        return new ExpressionParameter(o.string("name"), o.enumValue("type", ExpressionType.class));
    }

    private static Asset asset(JsonObject o) {
        return new Asset(o.string("path"), o.string("sha1"), o.longValue("size"),
                o.enumValue("kind", AssetKind.class), o.nullableInteger("resolution"));
    }

    private static PackSettings packSettings(JsonObject o) {
        return new PackSettings(o.intList("resolutions"), o.rawJson("description"));
    }

    /**
     * A name is the identifier of an expression or an expression function, so the two lists must not share one.
     */
    private static void checkExpressionNames(String source, Snapshot snapshot) {
        Map<String, Integer> functionIndexByName = new HashMap<>();
        for (int j = 0; j < snapshot.expressionFunctions().size(); j++) {
            functionIndexByName.putIfAbsent(snapshot.expressionFunctions().get(j).name(), j);
        }
        for (int i = 0; i < snapshot.expressions().size(); i++) {
            String name = snapshot.expressions().get(i).name();
            Integer j = functionIndexByName.get(name);
            if (j != null) {
                throw new ContentFormatException(source + ": " + SNAPSHOT_FILE + ": $.expressions[" + i
                        + "].name: name '" + name + "' is also used by $.expressionFunctions[" + j + "].name");
            }
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
