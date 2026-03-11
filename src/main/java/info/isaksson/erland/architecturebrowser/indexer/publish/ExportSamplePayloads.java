package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExportSamplePayloads {
    private ExportSamplePayloads() {
    }

    public static ArchitectureIndexDocument minimalSample(String producerVersion) {
        return ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-11T12:30:00Z")),
            producerVersion,
            new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of()),
            List.of()
        );
    }
}
