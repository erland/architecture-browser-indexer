package info.isaksson.erland.architecturebrowser.indexer.ir;

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

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ArchitectureIrFactory {
    private ArchitectureIrFactory() {
    }

    public static ArchitectureIndexDocument createPlaceholderDocument(RepositorySource source, String indexerVersion) {
        Instant generatedAt = Instant.now();
        SourceReference controllerSource = new SourceReference(
            source.path() == null ? "src/main/java/com/example/DemoController.java" : "src/main/java/com/example/DemoController.java",
            1,
            42,
            "class DemoController",
            Map.of("symbolKind", "class")
        );

        LogicalScope repositoryScope = new LogicalScope(
            "scope:repo",
            ScopeKind.REPOSITORY,
            source.repositoryId(),
            source.repositoryId(),
            null,
            List.of(controllerSource),
            Map.of()
        );
        LogicalScope packageScope = new LogicalScope(
            "scope:package:com.example",
            ScopeKind.PACKAGE,
            "com.example",
            "com.example",
            repositoryScope.id(),
            List.of(controllerSource),
            Map.of("language", "java")
        );

        ArchitectureEntity observedClass = new ArchitectureEntity(
            "entity:class:demo-controller",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "DemoController",
            "com.example.DemoController",
            packageScope.id(),
            List.of(controllerSource),
            Map.of("language", "java")
        );
        ArchitectureEntity inferredEndpoint = new ArchitectureEntity(
            "entity:endpoint:demo-controller",
            EntityKind.ENDPOINT,
            EntityOrigin.INFERRED,
            "Demo endpoint",
            "GET /demo",
            packageScope.id(),
            List.of(controllerSource),
            Map.of("httpMethod", "GET", "path", "/demo")
        );

        ArchitectureRelationship exposing = new ArchitectureRelationship(
            "rel:demo-controller:exposes:endpoint",
            RelationshipKind.EXPOSES,
            observedClass.id(),
            inferredEndpoint.id(),
            "controller exposes endpoint",
            List.of(controllerSource),
            Map.of()
        );

        Diagnostic diagnostic = new Diagnostic(
            "diag:step2-placeholder",
            DiagnosticSeverity.INFO,
            DiagnosticPhase.PUBLICATION,
            "placeholder.ir",
            "Placeholder IR emitted by Step 2 baseline",
            false,
            controllerSource.path(),
            packageScope.id(),
            observedClass.id(),
            List.of(controllerSource),
            Map.of()
        );

        CompletenessMetadata completeness = new CompletenessMetadata(
            CompletenessStatus.COMPLETE,
            0,
            0,
            0,
            List.of(),
            List.of("Placeholder payload produced before acquisition and scanning are implemented")
        );

        RunMetadata runMetadata = new RunMetadata(
            generatedAt,
            generatedAt,
            RunOutcome.SUCCESS,
            List.of("java"),
            Map.of("mode", "cli-placeholder")
        );

        return new ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            indexerVersion,
            runMetadata,
            source,
            List.of(repositoryScope, packageScope),
            List.of(observedClass, inferredEndpoint),
            List.of(exposing),
            List.of(diagnostic),
            completeness,
            Map.of()
        );
    }
}
