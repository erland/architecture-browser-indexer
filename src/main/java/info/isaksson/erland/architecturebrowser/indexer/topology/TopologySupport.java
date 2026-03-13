package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TopologySupport {
    private TopologySupport() {
    }

    static LogicalScope directoryScope(String directoryPath, String parentScopeId) {
        String displayName = baseName(directoryPath);
        return new LogicalScope(
            IdUtils.scopeId("directory", directoryPath),
            ScopeKind.DIRECTORY,
            directoryPath,
            displayName,
            parentScopeId,
            List.of(new SourceReference(directoryPath, null, null, null, metadataOf("scopeKind", "directory"))),
            metadataOf("relativePath", directoryPath, "displayName", displayName)
        );
    }

    static LogicalScope moduleScope(String modulePath, String parentScopeId, String language) {
        return new LogicalScope(
            IdUtils.scopeId("module", modulePath),
            ScopeKind.MODULE,
            modulePath,
            modulePath,
            parentScopeId,
            List.of(new SourceReference(modulePath, null, null, null, metadataOf("scopeKind", "module", "language", language))),
            metadataOf("relativePath", modulePath, "language", language)
        );
    }

    static ArchitectureEntity moduleEntity(String modulePath, String scopeId, String language, String role) {
        return new ArchitectureEntity(
            IdUtils.externalEntityId("logical-module", modulePath),
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            modulePath,
            modulePath,
            scopeId,
            List.of(new SourceReference(modulePath, null, null, null, metadataOf("entityKind", "module", "language", language))),
            metadataOf("language", language, "logicalRole", role, "relativePath", modulePath)
        );
    }

    static ArchitectureEntity packageEntity(LogicalScope packageScope) {
        return new ArchitectureEntity(
            IdUtils.externalEntityId("logical-package", packageScope.name()),
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            packageScope.name(),
            packageScope.displayName(),
            packageScope.id(),
            packageScope.sourceRefs(),
            metadataOf("language", packageScope.metadata().getOrDefault("language", "unknown"), "logicalRole", "package")
        );
    }

    static ArchitectureRelationship contains(String fromId, String toId, String label, List<SourceReference> refs, Map<String, Object> metadata) {
        return new ArchitectureRelationship(
            IdUtils.relationshipId("topology-contains", fromId, toId, label == null ? "" : label),
            RelationshipKind.CONTAINS,
            fromId,
            toId,
            label,
            refs == null ? List.of() : refs,
            metadata == null ? Map.of() : metadata
        );
    }

    static ArchitectureRelationship uses(String fromId, String toId, String label, List<SourceReference> refs, Map<String, Object> metadata) {
        return new ArchitectureRelationship(
            IdUtils.relationshipId("topology-uses", fromId, toId, label == null ? "" : label),
            RelationshipKind.USES,
            fromId,
            toId,
            label,
            refs == null ? List.of() : refs,
            metadata == null ? Map.of() : metadata
        );
    }

    static Diagnostic warning(String code, String message, String path, List<SourceReference> refs) {
        return new Diagnostic(
            "diag:topology:" + IdUtils.stableToken(code + ":" + path + ":" + message),
            DiagnosticSeverity.WARNING,
            DiagnosticPhase.INTERPRETATION,
            code,
            message,
            false,
            path,
            null,
            null,
            refs == null ? List.of() : refs,
            Map.of()
        );
    }


    private static Map<String, Object> metadataOf(Object... keyValues) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (!(key instanceof String s) || s == null) {
                continue;
            }
            Object value = keyValues[i + 1];
            if (value != null) {
                metadata.put(s, value);
            }
        }
        return Map.copyOf(metadata);
    }


    static String baseName(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    static String primaryPath(ExtractedEntityFact entity) {
        return entity.sourceRefs().isEmpty() ? null : entity.sourceRefs().get(0).path();
    }
}
