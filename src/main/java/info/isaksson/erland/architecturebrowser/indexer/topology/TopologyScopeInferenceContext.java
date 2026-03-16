package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;

import java.util.Map;

record TopologyScopeInferenceContext(
    Map<String, LogicalScope> packageScopesById,
    Map<String, String> packageScopeToEntityId,
    Map<String, String> fileDirectoryScopeIds,
    Map<String, String> fileModuleScopeIds
) {
}
