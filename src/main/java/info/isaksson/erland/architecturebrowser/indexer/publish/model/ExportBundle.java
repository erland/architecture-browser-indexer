package info.isaksson.erland.architecturebrowser.indexer.publish.model;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;

public record ExportBundle(
    ArchitectureIndexDocument document,
    ExportManifest manifest,
    ExportSnapshotSourceFiles snapshotSourceFiles
) {
    public ExportBundle {
        snapshotSourceFiles = snapshotSourceFiles == null
            ? new ExportSnapshotSourceFiles("snapshot-source-files/v1", java.util.List.of(), java.util.Map.of())
            : snapshotSourceFiles;
    }
}
