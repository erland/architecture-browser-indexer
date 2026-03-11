package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;

import java.util.Map;
import java.util.Optional;

public interface TopologyRelationshipResolver {
    Optional<ExtractedEntityFact> resolveInternalTarget(
        ExtractedRelationshipFact relationship,
        String fromPath,
        Map<String, ExtractedEntityFact> javaTypesByQualifiedName,
        Map<String, ExtractedEntityFact> fileModulesByPath
    );
}
