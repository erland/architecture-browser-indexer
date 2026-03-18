package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Stable top-level export contract.
 *
 * Step 1 baseline note: future canonical viewpoint descriptors belong at the document level
 * next to the current stable graph core and must be introduced additively with coordinated
 * schema/version updates once the shape is finalized.
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
    List<Diagnostic> diagnostics,
    CompletenessMetadata completeness,
    Map<String, Object> metadata
) {
}
