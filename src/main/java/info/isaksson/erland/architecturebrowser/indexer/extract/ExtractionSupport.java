package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;

import java.util.List;
import java.util.Map;

final class ExtractionSupport {
    private ExtractionSupport() {
    }

    static SourceReference sourceRef(String path, int line, String snippet, Map<String, Object> metadata) {
        return new SourceReference(path, line, line, snippet, metadata);
    }

    static LogicalScope fileScope(String repositoryScopeId, String relativePath) {
        return new LogicalScope(
            IdUtils.scopeId("file", relativePath),
            ScopeKind.FILE,
            relativePath,
            relativePath,
            repositoryScopeId,
            List.of(new SourceReference(relativePath, null, null, null, Map.of("scopeKind", "file"))),
            Map.of("relativePath", relativePath)
        );
    }

    static LogicalScope packageScope(String repositoryScopeId, String packageName, String relativePath, String language) {
        return new LogicalScope(
            IdUtils.scopeId(language + "-package", packageName),
            ScopeKind.PACKAGE,
            packageName,
            packageName,
            repositoryScopeId,
            List.of(new SourceReference(relativePath, null, null, null, Map.of("scopeKind", "package", "language", language))),
            Map.of("language", language)
        );
    }

    static ExtractedEntityFact fileModuleEntity(String scopeId, String relativePath, String language) {
        return new ExtractedEntityFact(
            IdUtils.fileEntityId(relativePath),
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            relativePath,
            relativePath,
            scopeId,
            List.of(new SourceReference(relativePath, null, null, null, Map.of("entityKind", "module", "language", language))),
            Map.of("language", language, "relativePath", relativePath)
        );
    }

    static ExtractedEntityFact externalDependencyEntity(String language, String qualifiedName, String relativePath, int line) {
        return new ExtractedEntityFact(
            IdUtils.externalEntityId(language, qualifiedName),
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            qualifiedName,
            qualifiedName,
            null,
            List.of(sourceRef(relativePath, line, qualifiedName, Map.of("language", language, "external", true))),
            Map.of("language", language, "qualifiedName", qualifiedName, "external", true)
        );
    }

    static ExtractedRelationshipFact containsRelationship(String fileEntityId, String memberEntityId, SourceReference ref) {
        return new ExtractedRelationshipFact(
            IdUtils.relationshipId("contains", fileEntityId, memberEntityId, "contains"),
            RelationshipKind.CONTAINS,
            fileEntityId,
            memberEntityId,
            "contains",
            List.of(ref),
            Map.of()
        );
    }

    static ExtractedRelationshipFact dependencyRelationship(String fromId, String toId, String label, SourceReference ref, String language) {
        return new ExtractedRelationshipFact(
            IdUtils.relationshipId("depends-on", fromId, toId, label),
            RelationshipKind.DEPENDS_ON,
            fromId,
            toId,
            label,
            List.of(ref),
            Map.of("language", language)
        );
    }

    static Diagnostic extractionWarning(SourceParseResult parseResult, String code, String message) {
        return new Diagnostic(
            "diag:extract:" + IdUtils.stableToken(parseResult.request().relativePath() + ":" + code + ":" + message),
            DiagnosticSeverity.WARNING,
            DiagnosticPhase.EXTRACTION,
            code,
            message,
            false,
            parseResult.request().relativePath(),
            null,
            null,
            List.of(new SourceReference(parseResult.request().relativePath(), null, null, null,
                Map.of("language", parseResult.request().language().inventoryKey()))),
            Map.of("parseStatus", parseResult.status().name())
        );
    }
}
