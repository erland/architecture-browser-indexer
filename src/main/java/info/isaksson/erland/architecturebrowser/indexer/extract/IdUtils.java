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
        return "scope:" + kind + ":" + stableToken(value);
    }

    public static String stableToken(String input) {
        CRC32 crc32 = new CRC32();
        crc32.update(input.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc32.getValue()).toLowerCase(Locale.ROOT);
    }
}
