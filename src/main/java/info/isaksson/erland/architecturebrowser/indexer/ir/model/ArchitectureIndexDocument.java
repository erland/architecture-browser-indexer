package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchitectureIndexDocument(
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
}
