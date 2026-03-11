package info.isaksson.erland.architecturebrowser.indexer.publish.model;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;

public record ExportBundle(
    ArchitectureIndexDocument document,
    ExportManifest manifest
) {
}
