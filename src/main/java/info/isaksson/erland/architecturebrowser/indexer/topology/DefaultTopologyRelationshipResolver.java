package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class DefaultTopologyRelationshipResolver implements TopologyRelationshipResolver {
    @Override
    public Optional<ExtractedEntityFact> resolveInternalTarget(
        ExtractedRelationshipFact relationship,
        String fromPath,
        Map<String, ExtractedEntityFact> javaTypesByQualifiedName,
        Map<String, ExtractedEntityFact> fileModulesByPath
    ) {
        String label = relationship.label();
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        if (javaTypesByQualifiedName.containsKey(label)) {
            return Optional.of(javaTypesByQualifiedName.get(label));
        }
        if (label.startsWith("./") || label.startsWith("../")) {
            String resolved = resolveRelativePath(fromPath, label);
            if (resolved != null) {
                for (String candidate : List.of(resolved, resolved + ".ts", resolved + ".tsx", resolved + "/index.ts", resolved + "/index.tsx")) {
                    if (fileModulesByPath.containsKey(candidate)) {
                        return Optional.of(fileModulesByPath.get(candidate));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static String resolveRelativePath(String fromPath, String importLabel) {
        try {
            Path base = Path.of(fromPath).getParent();
            if (base == null) {
                return null;
            }
            return base.resolve(importLabel).normalize().toString().replace("\\", "/");
        } catch (Exception ex) {
            return null;
        }
    }
}
