package info.isaksson.erland.architecturebrowser.indexer.extract.model;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.List;
import java.util.Map;

public record ExtractedEntityFact(
    String id,
    EntityKind kind,
    EntityOrigin origin,
    String name,
    String displayName,
    String scopeId,
    List<SourceReference> sourceRefs,
    Map<String, Object> metadata
) {
    public ExtractedEntityFact {
        sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
