package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;

import java.time.Instant;
import java.util.List;
import java.util.Map;

final class TestArchitectureDocuments {
    private TestArchitectureDocuments() {
    }

    static RunMetadata runMetadata() {
        return new RunMetadata(
            Instant.parse("2026-03-18T06:00:00Z"),
            Instant.parse("2026-03-18T06:01:00Z"),
            RunOutcome.SUCCESS,
            List.of(),
            Map.of(
                "indexedFileCount", 0,
                "totalFileCount", 0,
                "degradedFileCount", 0,
                "omittedPaths", List.of()
            )
        );
    }

    static RepositorySource repositorySource() {
        return new RepositorySource(
            "repo",
            "LOCAL_PATH",
            "/workspace/repo",
            null,
            null,
            null,
            Instant.parse("2026-03-18T06:00:00Z"),
            Map.of()
        );
    }

    static CompletenessMetadata completeness() {
        return new CompletenessMetadata(
            CompletenessStatus.COMPLETE,
            0,
            0,
            0,
            List.of(),
            List.of()
        );
    }
}
