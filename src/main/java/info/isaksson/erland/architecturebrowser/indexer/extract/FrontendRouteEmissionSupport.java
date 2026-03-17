package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FrontendRouteEmissionSupport {

    void emit(
        ExtractionAccumulator accumulator,
        String relativePath,
        List<FrontendRouteCandidate> candidates,
        Map<String, ExtractedEntityFact> namedEntities,
        FrontendRoutePathNormalizationSupport normalizationSupport
    ) {
        if (accumulator == null || candidates == null || candidates.isEmpty()) {
            return;
        }
        Map<FrontendRouteCandidate, ExtractedEntityFact> routeEntities = new LinkedHashMap<>();
        for (FrontendRouteCandidate candidate : candidates) {
            ExtractedEntityFact routeEntity = routeEntity(candidate.framework(), relativePath, candidate.path(), candidate.fullPath(), candidate.startLine(), candidate.snippet());
            accumulator.addEntity(routeEntity);
            routeEntities.put(candidate, routeEntity);
        }
        for (int i = 0; i < candidates.size(); i++) {
            FrontendRouteCandidate candidate = candidates.get(i);
            ExtractedEntityFact routeEntity = routeEntities.get(candidate);
            FrontendRouteCandidate parent = normalizationSupport.findParent(candidate, candidates, i);
            if (parent != null) {
                ExtractedEntityFact parentEntity = routeEntities.get(parent);
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    routeEntity.id(),
                    parentEntity.id(),
                    candidate.path(),
                    routeRef(candidate, relativePath),
                    "typescript",
                    relationshipMetadata(candidate.framework(), "childOf", candidate.path(), normalizationSupport.fullRoutePath(candidate, parent), true)
                ));
            }
            for (String targetName : candidate.targets()) {
                addRouteTargetRelationship(accumulator, candidate, routeEntity, targetName, namedEntities, relativePath, "targets", "route-target", EntityKind.UI_MODULE);
            }
            for (String lazyTarget : candidate.lazyLoads()) {
                addRouteTargetRelationship(accumulator, candidate, routeEntity, lazyTarget, namedEntities, relativePath, "lazyLoads", "route-lazy-target", EntityKind.MODULE);
            }
            for (String guard : candidate.guards()) {
                addRouteTargetRelationship(accumulator, candidate, routeEntity, guard, namedEntities, relativePath, "guards", "route-guard", EntityKind.CLASS);
            }
            for (String resolver : candidate.resolvers()) {
                addRouteTargetRelationship(accumulator, candidate, routeEntity, resolver, namedEntities, relativePath, "resolves", "route-resolver", EntityKind.CLASS);
            }
        }
    }

    private void addRouteTargetRelationship(
        ExtractionAccumulator accumulator,
        FrontendRouteCandidate candidate,
        ExtractedEntityFact routeEntity,
        String targetName,
        Map<String, ExtractedEntityFact> namedEntities,
        String relativePath,
        String frameworkRelationship,
        String targetClassification,
        EntityKind fallbackKind
    ) {
        if (targetName == null || targetName.isBlank()) {
            return;
        }
        ExtractedEntityFact targetEntity = namedEntities.get(targetName);
        boolean resolved = targetEntity != null;
        if (!resolved) {
            targetEntity = ExtractionSupport.inferredTypeEntity(
                candidate.framework(),
                fallbackKind,
                targetName,
                relativePath,
                candidate.startLine(),
                Map.of(
                    "framework", candidate.framework(),
                    "targetClassification", targetClassification,
                    "resolution", "inferred-frontend-route-target",
                    "external", false,
                    "inferredInternal", true
                )
            );
            accumulator.addEntity(targetEntity);
        }
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            routeEntity.id(),
            targetEntity.id(),
            targetName,
            routeRef(candidate, relativePath),
            "typescript",
            relationshipMetadata(candidate.framework(), frameworkRelationship, candidate.path(), candidate.fullPath(), resolved)
        ));
    }

    private ExtractedEntityFact routeEntity(String framework, String relativePath, String routePath, String fullPath, int line, String snippet) {
        String qualifiedName = framework + "-route:" + fullPath;
        return ExtractionSupport.inferredTypeEntity(
            framework,
            EntityKind.UI_MODULE,
            qualifiedName,
            relativePath,
            line,
            Map.of(
                "framework", framework,
                "route", true,
                "routePath", routePath,
                "routeFullPath", fullPath,
                "entityRole", "route",
                "targetClassification", "route-node",
                "external", false,
                "inferredInternal", true,
                "routeSnippet", snippet == null ? "" : snippet
            )
        );
    }

    private Map<String, Object> relationshipMetadata(String framework, String frameworkRelationship, String path, String fullPath, boolean resolved) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", framework);
        metadata.put("frameworkRelationship", frameworkRelationship);
        metadata.put("dependencySource", framework + ":route-" + frameworkRelationship);
        metadata.put("dependencyCategory", "framework");
        metadata.put("routePath", path);
        metadata.put("routeFullPath", fullPath);
        metadata.put("resolvedFromRouteExtraction", resolved);
        metadata.put("targetClassification", "route-target");
        return Map.copyOf(metadata);
    }

    private SourceReference routeRef(FrontendRouteCandidate candidate, String relativePath) {
        return ExtractionSupport.sourceRef(relativePath, candidate.startLine(), candidate.snippet(), Map.of(
            "language", "typescript",
            "framework", candidate.framework(),
            "routePath", candidate.path(),
            "routeFullPath", candidate.fullPath()
        ));
    }
}
