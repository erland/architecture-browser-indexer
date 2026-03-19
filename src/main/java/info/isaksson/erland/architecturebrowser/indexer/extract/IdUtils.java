package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.CRC32;

public final class IdUtils {
    private IdUtils() {
    }

    public static String fileEntityId(String relativePath) {
        return "entity:file:" + stableToken(relativePath);
    }

    public static String scopedEntityId(String language, String relativePath, String name, int line) {
        return "entity:" + language + ":" + stableToken(relativePath + ":" + name + ":" + line);
    }

    public static String externalEntityId(String language, String qualifiedName) {
        return "entity:external:" + language + ":" + stableToken(qualifiedName);
    }

    public static String relationshipId(String prefix, String fromId, String toId, String label) {
        return "rel:" + prefix + ":" + stableToken(fromId + ":" + toId + ":" + label);
    }

    public static String scopeId(String kind, String value) {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("scopeId kind must not be null/blank");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("scopeId value must not be null/blank");
        }
        return "scope:" + kind + ":" + stableToken(value);
    }

    public static String stableToken(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("stableToken input must not be null/blank");
        }
        CRC32 crc32 = new CRC32();
        crc32.update(input.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc32.getValue()).toLowerCase(Locale.ROOT);
    }
}
