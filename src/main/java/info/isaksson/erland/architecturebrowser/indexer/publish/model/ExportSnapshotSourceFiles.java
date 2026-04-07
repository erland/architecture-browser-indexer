package info.isaksson.erland.architecturebrowser.indexer.publish.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot-scoped collection of source-file artifacts exported alongside the architecture index.
 */
public record ExportSnapshotSourceFiles(
    String contractVersion,
    List<ExportSnapshotSourceFile> files,
    Map<String, Object> metadata
) {
    public ExportSnapshotSourceFiles {
        contractVersion = normalizeContractVersion(contractVersion);
        files = normalizeFiles(files);
        metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
    }

    private static String normalizeContractVersion(String value) {
        if (value == null || value.isBlank()) {
            return "snapshot-source-files/v1";
        }
        return value.trim();
    }

    private static List<ExportSnapshotSourceFile> normalizeFiles(List<ExportSnapshotSourceFile> value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, ExportSnapshotSourceFile> uniqueByPath = new LinkedHashMap<>();
        for (ExportSnapshotSourceFile file : value) {
            if (file == null) {
                continue;
            }
            uniqueByPath.putIfAbsent(file.relativePath(), file);
        }
        return List.copyOf(uniqueByPath.values());
    }
}
