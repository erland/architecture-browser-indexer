package info.isaksson.erland.architecturebrowser.indexer.publish;

import java.util.List;

public record SnapshotSourceFileReadResult(
    List<SnapshotSourceFileText> files,
    List<String> skippedRelativePaths,
    List<SnapshotSourceFileSkip> skippedFiles
) {
    public SnapshotSourceFileReadResult {
        files = files == null ? List.of() : List.copyOf(files);
        skippedRelativePaths = skippedRelativePaths == null ? List.of() : List.copyOf(skippedRelativePaths);
        skippedFiles = skippedFiles == null ? List.of() : List.copyOf(skippedFiles);
    }

    public SnapshotSourceFileReadResult(List<SnapshotSourceFileText> files, List<String> skippedRelativePaths) {
        this(
            files,
            skippedRelativePaths,
            skippedRelativePaths == null
                ? List.of()
                : skippedRelativePaths.stream()
                    .map(path -> new SnapshotSourceFileSkip(path, "skipped"))
                    .toList()
        );
    }
}
