package info.isaksson.erland.architecturebrowser.indexer.extract.model;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.List;
import java.util.Map;

public record ExtractedRelationshipFact(
    String id,
    RelationshipKind kind,
    String fromEntityId,
    String toEntityId,
    String label,
    List<SourceReference> sourceRefs,
    Map<String, Object> metadata
) {
    public ExtractedRelationshipFact {
        sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
