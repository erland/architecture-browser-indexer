package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.CRC32;

final class IdUtils {
    private IdUtils() {
    }

    static String fileEntityId(String relativePath) {
        return "entity:file:" + stableToken(relativePath);
    }

    static String scopedEntityId(String language, String relativePath, String name, int line) {
        return "entity:" + language + ":" + stableToken(relativePath + ":" + name + ":" + line);
    }

    static String externalEntityId(String language, String qualifiedName) {
        return "entity:external:" + language + ":" + stableToken(qualifiedName);
    }

    static String relationshipId(String prefix, String fromId, String toId, String label) {
        return "rel:" + prefix + ":" + stableToken(fromId + ":" + toId + ":" + label);
    }

    static String scopeId(String kind, String value) {
        return "scope:" + kind + ":" + stableToken(value);
    }

    static String stableToken(String input) {
        CRC32 crc32 = new CRC32();
        crc32.update(input.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc32.getValue()).toLowerCase(Locale.ROOT);
    }
}
