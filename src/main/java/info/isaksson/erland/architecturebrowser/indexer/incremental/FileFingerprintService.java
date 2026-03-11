package info.isaksson.erland.architecturebrowser.indexer.incremental;

import info.isaksson.erland.architecturebrowser.indexer.incremental.model.FileFingerprint;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FileFingerprintService {
    public static final String SNAPSHOT_SCHEMA_VERSION = "1";

    public IncrementalSnapshot createSnapshot(FileInventory inventory) {
        Map<String, FileFingerprint> filesByPath = new LinkedHashMap<>();
        for (FileInventoryEntry entry : inventory.entries()) {
            if (entry.ignored()) {
                continue;
            }
            filesByPath.put(entry.relativePath(), fingerprint(entry));
        }
        return new IncrementalSnapshot(
            SNAPSHOT_SCHEMA_VERSION,
            Instant.now(),
            filesByPath,
            Map.of(
                "indexedFileCount", inventory.indexedFiles(),
                "totalFileCount", inventory.totalFiles()
            )
        );
    }

    public FileFingerprint fingerprint(FileInventoryEntry entry) {
        return new FileFingerprint(
            entry.relativePath(),
            entry.size(),
            contentHash(entry),
            entry.detectedLanguage(),
            entry.type()
        );
    }

    private static String contentHash(FileInventoryEntry entry) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String seed = entry.relativePath() + "|" + entry.size() + "|" + entry.detectedLanguage() + "|" + entry.type();
            return HexFormat.of().formatHex(digest.digest(seed.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash file inventory entry", ex);
        }
    }
}
