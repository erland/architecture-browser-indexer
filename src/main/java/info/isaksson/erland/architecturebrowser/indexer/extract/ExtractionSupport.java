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
import info.isaksson.erland.architecturebrowser.indexer.naming.DisplayNamePolicy;
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
        String parentDirectory = parentDirectory(relativePath);
        String parentScopeId = parentDirectory == null ? repositoryScopeId : IdUtils.scopeId("directory", parentDirectory);
        String displayName = DisplayNamePolicy.scopeDisplayName(ScopeKind.FILE, relativePath, null);
        return new LogicalScope(
            IdUtils.scopeId("file", relativePath),
            ScopeKind.FILE,
            relativePath,
            displayName,
            parentScopeId,
            List.of(new SourceReference(relativePath, null, null, null, Map.of("scopeKind", "file", "displayName", displayName))),
            Map.of("relativePath", relativePath, "displayName", displayName)
        );
    }

    static LogicalScope packageScope(String repositoryScopeId, String packageName, String relativePath, String language) {
        String parentPackageName = parentPackageName(packageName);
        String parentScopeId = parentPackageName == null
            ? repositoryScopeId
            : IdUtils.scopeId(language + "-package", parentPackageName);
        return new LogicalScope(
            IdUtils.scopeId(language + "-package", packageName),
            ScopeKind.PACKAGE,
            packageName,
            DisplayNamePolicy.scopeDisplayName(ScopeKind.PACKAGE, packageName, language),
            parentScopeId,
            List.of(new SourceReference(relativePath, null, null, null, Map.of("scopeKind", "package", "language", language))),
            Map.of(
                "language", language,
                "packageName", packageName,
                "displayName", DisplayNamePolicy.scopeDisplayName(ScopeKind.PACKAGE, packageName, language),
                "parentPackageName", parentPackageName == null ? "" : parentPackageName
            )
        );
    }

    static ExtractedEntityFact fileModuleEntity(String scopeId, String relativePath, String language) {
        String displayName = DisplayNamePolicy.entityDisplayName(EntityKind.MODULE, relativePath, language);
        return new ExtractedEntityFact(
            IdUtils.fileEntityId(relativePath),
            EntityKind.MODULE,
            EntityOrigin.OBSERVED,
            relativePath,
            displayName,
            scopeId,
            List.of(new SourceReference(relativePath, null, null, null, Map.of("entityKind", "module", "language", language, "displayName", displayName))),
            Map.of("language", language, "relativePath", relativePath, "displayName", displayName)
        );
    }

    static ExtractedEntityFact externalDependencyEntity(String language, String qualifiedName, String relativePath, int line) {
        String displayName = DisplayNamePolicy.entityDisplayName(EntityKind.MODULE, qualifiedName, language);
        return new ExtractedEntityFact(
            IdUtils.externalEntityId(language, qualifiedName),
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            qualifiedName,
            displayName,
            null,
            List.of(sourceRef(relativePath, line, qualifiedName, Map.of("language", language, "external", true, "displayName", displayName))),
            Map.of("language", language, "qualifiedName", qualifiedName, "external", true, "displayName", displayName)
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

    static ExtractedEntityFact inferredTypeEntity(String language, EntityKind kind, String qualifiedName, String relativePath, int line, Map<String, Object> metadata) {
        String displayName = DisplayNamePolicy.entityDisplayName(kind, qualifiedName, language);
        java.util.Map<String, Object> merged = new java.util.LinkedHashMap<>();
        merged.put("language", language);
        merged.put("qualifiedName", qualifiedName);
        merged.put("external", true);
        merged.put("displayName", displayName);
        if (metadata != null) {
            merged.putAll(metadata);
        }
        return new ExtractedEntityFact(
            IdUtils.externalEntityId(language + "-type-" + kind.name().toLowerCase(java.util.Locale.ROOT), qualifiedName),
            kind,
            EntityOrigin.INFERRED,
            qualifiedName,
            displayName,
            null,
            List.of(sourceRef(relativePath, line, qualifiedName, Map.of("language", language, "external", true, "displayName", displayName))),
            java.util.Map.copyOf(merged)
        );
    }

    static ExtractedRelationshipFact typedRelationship(RelationshipKind kind, String prefix, String fromId, String toId, String label, SourceReference ref, String language) {
        return typedRelationship(kind, prefix, fromId, toId, label, ref, language, Map.of());
    }

    static ExtractedRelationshipFact typedRelationship(
        RelationshipKind kind,
        String prefix,
        String fromId,
        String toId,
        String label,
        SourceReference ref,
        String language,
        Map<String, Object> metadata
    ) {
        java.util.Map<String, Object> merged = new java.util.LinkedHashMap<>();
        merged.put("language", language);
        if (metadata != null) {
            merged.putAll(metadata);
        }
        return new ExtractedRelationshipFact(
            IdUtils.relationshipId(prefix, fromId, toId, label),
            kind,
            fromId,
            toId,
            label,
            List.of(ref),
            java.util.Map.copyOf(merged)
        );
    }

    static ExtractedRelationshipFact dependencyRelationship(String fromId, String toId, String label, SourceReference ref, String language) {
        return dependencyRelationship(fromId, toId, label, ref, language, Map.of());
    }

    static ExtractedRelationshipFact dependencyRelationship(
        String fromId,
        String toId,
        String label,
        SourceReference ref,
        String language,
        Map<String, Object> metadata
    ) {
        java.util.Map<String, Object> merged = new java.util.LinkedHashMap<>();
        merged.put("language", language);
        if (metadata != null) {
            merged.putAll(metadata);
        }
        String dependencySourceKey = metadata == null ? null : java.util.Objects.toString(metadata.get("dependencySource"), null);
        String relationshipLabelKey = (dependencySourceKey == null || dependencySourceKey.isBlank())
            ? label
            : label + ":" + dependencySourceKey;
        return new ExtractedRelationshipFact(
            IdUtils.relationshipId("depends-on", fromId, toId, relationshipLabelKey),
            RelationshipKind.DEPENDS_ON,
            fromId,
            toId,
            label,
            List.of(ref),
            java.util.Map.copyOf(merged)
        );
    }


    private static String parentDirectory(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || !relativePath.contains("/")) {
            return null;
        }
        return relativePath.substring(0, relativePath.lastIndexOf('/'));
    }

    private static String parentPackageName(String packageName) {
        if (packageName == null || packageName.isBlank() || !packageName.contains(".")) {
            return null;
        }
        return packageName.substring(0, packageName.lastIndexOf('.'));
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
