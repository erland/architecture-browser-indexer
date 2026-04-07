package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapshotSourceFileReferenceCollectorTest {

    @Test
    void collectsUniqueReferencedPathsAcrossExportedObjectsInStableOrder() {
        ArchitectureIndexDocument document = new ArchitectureIndexDocument(
            "1.0",
            "test-indexer",
            new RunMetadata(
                Instant.parse("2026-04-06T10:15:00Z"),
                Instant.parse("2026-04-06T10:16:00Z"),
                RunOutcome.SUCCESS,
                List.of("java"),
                Map.of()
            ),
            RepositorySource.localPath("repo-1", "/tmp/repo", Instant.parse("2026-04-06T10:14:00Z")),
            List.of(new LogicalScope(
                "scope:module:app",
                ScopeKind.MODULE,
                "App",
                null,
                null,
                List.of(sourceRef("src/main/java/demo/App.java")),
                Map.of()
            )),
            List.of(
                new ArchitectureEntity(
                    "entity:app",
                    EntityKind.CLASS,
                    EntityOrigin.OBSERVED,
                    "demo.App",
                    "demo.App",
                    null,
                    List.of(sourceRef("src\\main\\java\\demo\\App.java"), sourceRef("src/main/resources/application.yml")),
                    Map.of()
                )
            ),
            List.of(
                new ArchitectureRelationship(
                    "rel:1",
                    RelationshipKind.USES,
                    "entity:app",
                    "entity:cfg",
                    "uses",
                    List.of(sourceRef("src/main/resources/application.yml"), sourceRef("../escape.txt")),
                    Map.of()
                )
            ),
            List.of(
                new Diagnostic(
                    "diag:1",
                    DiagnosticSeverity.WARNING,
                    DiagnosticPhase.PUBLICATION,
                    "D001",
                    "something",
                    false,
                    "src/main/java/demo/App.java",
                    null,
                    null,
                    List.of(sourceRef("pom.xml"), sourceRef("   ")),
                    Map.of()
                )
            ),
            new CompletenessMetadata(CompletenessStatus.COMPLETE, 1, 1, 0, List.of(), List.of()),
            Map.of()
        );

        List<String> referencedPaths = new SnapshotSourceFileReferenceCollector().collectReferencedRelativePaths(document);

        assertEquals(List.of(
            "src/main/java/demo/App.java",
            "src/main/resources/application.yml",
            "pom.xml"
        ), referencedPaths);
    }

    private static SourceReference sourceRef(String path) {
        return new SourceReference(path, 1, 1, null, Map.of());
    }
}
