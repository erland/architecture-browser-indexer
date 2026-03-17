package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ExtractionRelationshipSupport {
    private ExtractionRelationshipSupport() {
    }

    static ExtractedRelationshipFact typedRelationship(
        RelationshipKind relationshipKind,
        String relationshipType,
        String fromEntityId,
        String toEntityId,
        String label,
        SourceReference ref,
        String language,
        Map<String, Object> metadata
    ) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        merged.putIfAbsent("language", language);
        merged.putIfAbsent("relationshipType", relationshipType);
        String id = "rel:" + relationshipType + ':' + Integer.toHexString((fromEntityId + "->" + toEntityId + ':' + relationshipKind.name()).hashCode());
        return new ExtractedRelationshipFact(
            id,
            relationshipKind,
            fromEntityId,
            toEntityId,
            label,
            ref == null ? List.of() : List.of(ref),
            Map.copyOf(merged)
        );
    }

    static ExtractedRelationshipFact dependencyRelationship(
        String fromEntityId,
        String toEntityId,
        String label,
        SourceReference ref,
        String language,
        Map<String, Object> metadata
    ) {
        return typedRelationship(RelationshipKind.DEPENDS_ON, "dependsOn", fromEntityId, toEntityId, label, ref, language, metadata);
    }

    static ExtractedRelationshipFact containsRelationship(String fromEntityId, String toEntityId, SourceReference ref) {
        return typedRelationship(RelationshipKind.CONTAINS, "contains", fromEntityId, toEntityId, null, ref, String.valueOf(ref == null ? "unknown" : ref.metadata().getOrDefault("language", "unknown")), Map.of());
    }

    static SourceReference sourceRef(String relativePath, Integer line, String snippet, Map<String, Object> metadata) {
        return new SourceReference(relativePath, line, line, snippet, metadata == null ? Map.of() : Map.copyOf(metadata));
    }
}
