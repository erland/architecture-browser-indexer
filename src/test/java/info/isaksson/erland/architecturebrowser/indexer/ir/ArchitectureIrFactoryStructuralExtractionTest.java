package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrFactoryStructuralExtractionTest {
    @Test
    void includesStructuralExtractionEntitiesRelationshipsAndSummaryInDocument() {
        FileInventory inventory = new FileInventory(
            List.of(new FileInventoryEntry("src/app.ts", 10, "ts", "source", "typescript", false, List.of("typescript"))),
            1, 1, 0, Set.of("typescript"), Set.of("typescript")
        );
        String source = "import { x } from './lib';\nexport function run() { return x; }\n";
        ParseBatchResult parseBatchResult = new ParseBatchResult(
            List.of(new SourceParseResult(
                new SourceParseRequest(Path.of("src/app.ts"), "src/app.ts", ParseLanguage.TYPESCRIPT, source),
                ParseStatus.BACKEND_UNAVAILABLE,
                null,
                List.of(),
                Map.of())),
            Map.of(ParseLanguage.TYPESCRIPT, 1),
            Map.of(ParseStatus.BACKEND_UNAVAILABLE, 1)
        );
        StructuralExtractionResult extractionResult = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(parseBatchResult);

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-10T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatchResult,
            extractionResult
        );

        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "run".equals(entity.name())));
        assertTrue(document.relationships().stream().anyMatch(relationship -> "./lib".equals(relationship.label())));
        assertEquals("PARTIAL", document.runMetadata().outcome().name());
        assertTrue(document.metadata().containsKey("extractionSummary"));
    }
}
