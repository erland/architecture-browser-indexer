package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Stable top-level export contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchitectureIndexDocument(
    String schemaVersion,
    String indexerVersion,
    RunMetadata runMetadata,
    RepositorySource source,
    List<LogicalScope> scopes,
    List<ArchitectureEntity> entities,
    List<ArchitectureRelationship> relationships,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ArchitectureViewpoint> viewpoints,
    List<Diagnostic> diagnostics,
    CompletenessMetadata completeness,
    Map<String, Object> metadata
) {
    public ArchitectureIndexDocument(
        String schemaVersion,
        String indexerVersion,
        RunMetadata runMetadata,
        RepositorySource source,
        List<LogicalScope> scopes,
        List<ArchitectureEntity> entities,
        List<ArchitectureRelationship> relationships,
        List<Diagnostic> diagnostics,
        CompletenessMetadata completeness,
        Map<String, Object> metadata
    ) {
        this(schemaVersion, indexerVersion, runMetadata, source, scopes, entities, relationships, null, diagnostics, completeness, metadata);
    }

    public ArchitectureIndexDocument {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
        entities = entities == null ? List.of() : List.copyOf(entities);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        viewpoints = viewpoints == null ? null : List.copyOf(viewpoints);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
