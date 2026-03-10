package info.isaksson.erland.architecturebrowser.indexer.scan;

import java.util.Set;

public record InventoryScanOptions(
    Set<String> ignoredDirectoryNames,
    Set<String> ignoredFileNames,
    long maxMarkerReadBytes
) {
    public static InventoryScanOptions defaults() {
        return new InventoryScanOptions(
            Set.of(".git", ".idea", ".gradle", "node_modules", "target", "build", "dist", "out"),
            Set.of(".DS_Store"),
            16384L
        );
    }
}
