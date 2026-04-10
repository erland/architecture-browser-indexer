package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class FrontendRouteEmissionSupport {
    private final FrontendRouteEntityFactory routeEntityFactory = new FrontendRouteEntityFactory();
    private final FrontendNavigationSourceResolver navigationSourceResolver = new FrontendNavigationSourceResolver();

    void emit(
        ExtractionAccumulator accumulator,
        String relativePath,
        List<FrontendRouteCandidate> candidates,
        List<FrontendNavigationCandidate> navigationCandidates,
        Map<String, ExtractedEntityFact> namedEntities,
        FrontendRoutePathNormalizationSupport normalizationSupport
    ) {
        if (accumulator == null || ((candidates == null || candidates.isEmpty()) && (navigationCandidates == null || navigationCandidates.isEmpty()))) {
            return;
        }
        Map<FrontendRouteCandidate, ExtractedEntityFact> routeEntities = new LinkedHashMap<>();
        emitDeclaredRoutes(accumulator, relativePath, candidates, namedEntities, normalizationSupport, routeEntities);
        emitNavigationCandidates(accumulator, relativePath, navigationCandidates, namedEntities);
    }

    private void emitDeclaredRoutes(
        ExtractionAccumulator accumulator,
        String relativePath,
        List<FrontendRouteCandidate> candidates,
        Map<String, ExtractedEntityFact> namedEntities,
        FrontendRoutePathNormalizationSupport normalizationSupport,
        Map<FrontendRouteCandidate, ExtractedEntityFact> routeEntities
    ) {
        if (candidates == null) {
            return;
        }
        for (FrontendRouteCandidate candidate : candidates) {
            ExtractedEntityFact routeEntity = routeEntityFactory.routeEntity(candidate.framework(), relativePath, candidate.path(), candidate.fullPath(), candidate.startLine(),
                candidate.snippet(), candidate.declarationKind(), candidate.redirectTarget());
            accumulator.addEntity(routeEntity);
            routeEntities.put(candidate, routeEntity);
        }
        for (int i = 0; i < candidates.size(); i++) {
            FrontendRouteCandidate candidate = candidates.get(i);
            ExtractedEntityFact routeEntity = routeEntities.get(candidate);
            emitParentRelationship(accumulator, relativePath, candidates, normalizationSupport, routeEntities, i, candidate, routeEntity);
            emitTargetRelationships(accumulator, relativePath, candidate, routeEntity, namedEntities);
            emitRedirectRelationship(accumulator, relativePath, candidate, routeEntity);
        }
    }

    private void emitParentRelationship(ExtractionAccumulator accumulator, String relativePath, List<FrontendRouteCandidate> candidates,
                                        FrontendRoutePathNormalizationSupport normalizationSupport,
                                        Map<FrontendRouteCandidate, ExtractedEntityFact> routeEntities,
                                        int index, FrontendRouteCandidate candidate, ExtractedEntityFact routeEntity) {
        FrontendRouteCandidate parent = normalizationSupport.findParent(candidate, candidates, index);
        if (parent == null) {
            return;
        }
        ExtractedEntityFact parentEntity = routeEntities.get(parent);
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            routeEntity.id(),
            parentEntity.id(),
            candidate.path(),
            routeRef(candidate, relativePath),
            "typescript",
            relationshipMetadata(candidate.framework(), "childOf", candidate.path(), normalizationSupport.fullRoutePath(candidate, parent), true,
                Map.of(
                    "routeSourceKind", "declared-route",
                    "routeDeclarationKind", candidate.declarationKind(),
                    "parentRouteEntityId", parentEntity.id(),
                    "parentRoutePath", parent.path(),
                    "parentRouteFullPath", parent.fullPath()
                ))
        ));
    }

    private void emitTargetRelationships(ExtractionAccumulator accumulator, String relativePath, FrontendRouteCandidate candidate,
                                         ExtractedEntityFact routeEntity, Map<String, ExtractedEntityFact> namedEntities) {
        for (String targetName : candidate.targets()) {
            addRouteTargetRelationship(accumulator, candidate, routeEntity, targetName, namedEntities, relativePath, "targets", "route-target", EntityKind.UI_MODULE,
                Map.of("routeSourceKind", "declared-route", "routeDeclarationKind", candidate.declarationKind()));
        }
        for (String lazyTarget : candidate.lazyLoads()) {
            addRouteTargetRelationship(accumulator, candidate, routeEntity, lazyTarget, namedEntities, relativePath, "lazyLoads", "route-lazy-target", EntityKind.MODULE,
                Map.of("routeSourceKind", "declared-route", "routeDeclarationKind", candidate.declarationKind()));
        }
        for (String guard : candidate.guards()) {
            addRouteTargetRelationship(accumulator, candidate, routeEntity, guard, namedEntities, relativePath, "guards", "route-guard", EntityKind.CLASS,
                Map.of("routeSourceKind", "declared-route", "routeDeclarationKind", candidate.declarationKind(), "guardReference", guard));
        }
        for (String resolver : candidate.resolvers()) {
            addRouteTargetRelationship(accumulator, candidate, routeEntity, resolver, namedEntities, relativePath, "resolves", "route-resolver", EntityKind.CLASS,
                Map.of("routeSourceKind", "declared-route", "routeDeclarationKind", candidate.declarationKind()));
        }
    }

    private void emitRedirectRelationship(ExtractionAccumulator accumulator, String relativePath, FrontendRouteCandidate candidate,
                                          ExtractedEntityFact routeEntity) {
        if (!candidate.redirectTarget().isBlank()) {
            addRouteLiteralRelationship(accumulator, candidate, routeEntity, candidate.redirectTarget(), relativePath, "redirects", "route-redirect-target");
        }
    }

    private void emitNavigationCandidates(ExtractionAccumulator accumulator, String relativePath, List<FrontendNavigationCandidate> navigationCandidates,
                                          Map<String, ExtractedEntityFact> namedEntities) {
        if (navigationCandidates == null || navigationCandidates.isEmpty()) {
            return;
        }
        for (FrontendNavigationCandidate candidate : navigationCandidates) {
            ExtractedEntityFact sourceEntity = navigationSourceResolver.findNavigationSourceEntity(namedEntities, relativePath, candidate.startLine(), candidate.framework());
            if (sourceEntity == null) {
                continue;
            }
            ExtractedEntityFact targetRouteEntity = routeEntityFactory.inferredRouteEntity(candidate.framework(), candidate.targetLiteral(), relativePath,
                candidate.startLine(), candidate.sourceKind());
            accumulator.addEntity(targetRouteEntity);
            Map<String, Object> extra = Map.of(
                "routeSourceKind", candidate.sourceKind(),
                "navigationTargetLiteral", targetRouteEntity.metadata().get("routeFullPath"),
                "navigationLiteral", candidate.targetLiteral()
            );
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                sourceEntity.id(),
                targetRouteEntity.id(),
                Objects.toString(targetRouteEntity.metadata().get("routeFullPath"), candidate.targetLiteral()),
                navigationRef(candidate, relativePath),
                "typescript",
                relationshipMetadata(candidate.framework(), candidate.sourceKind().equals("link") ? "linksToRoute" : "navigatesToRoute",
                    candidate.targetLiteral(), Objects.toString(targetRouteEntity.metadata().get("routeFullPath"), candidate.targetLiteral()), true, extra)
            ));
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
        EntityKind fallbackKind,
        Map<String, Object> extraMetadata
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
            relationshipMetadata(candidate.framework(), frameworkRelationship, candidate.path(), candidate.fullPath(), resolved, extraMetadata)
        ));
    }

    private void addRouteLiteralRelationship(
        ExtractionAccumulator accumulator,
        FrontendRouteCandidate candidate,
        ExtractedEntityFact routeEntity,
        String targetLiteral,
        String relativePath,
        String frameworkRelationship,
        String targetClassification
    ) {
        ExtractedEntityFact targetRouteEntity = routeEntityFactory.inferredRouteEntity(candidate.framework(), targetLiteral, relativePath, candidate.startLine(), frameworkRelationship);
        accumulator.addEntity(targetRouteEntity);
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            routeEntity.id(),
            targetRouteEntity.id(),
            Objects.toString(targetRouteEntity.metadata().get("routeFullPath"), targetLiteral),
            routeRef(candidate, relativePath),
            "typescript",
            relationshipMetadata(candidate.framework(), frameworkRelationship, candidate.path(), candidate.fullPath(), true,
                Map.of(
                    "routeSourceKind", "redirect",
                    "routeDeclarationKind", candidate.declarationKind(),
                    "redirectTargetLiteral", targetLiteral,
                    "targetClassification", targetClassification,
                    "navigationTargetLiteral", Objects.toString(targetRouteEntity.metadata().get("routeFullPath"), targetLiteral)
                ))
        ));
    }

    private Map<String, Object> relationshipMetadata(String framework, String frameworkRelationship, String path, String fullPath, boolean resolved,
                                                     Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", framework);
        metadata.put("frameworkRelationship", frameworkRelationship);
        metadata.put("dependencySource", framework + ":route-" + frameworkRelationship);
        metadata.put("dependencyCategory", "framework");
        metadata.put("routePath", path);
        metadata.put("routeFullPath", fullPath);
        metadata.put("resolvedFromRouteExtraction", resolved);
        metadata.put("targetClassification", "route-target");
        if (extraMetadata != null && !extraMetadata.isEmpty()) {
            metadata.putAll(extraMetadata);
        }
        return Map.copyOf(metadata);
    }

    private SourceReference routeRef(FrontendRouteCandidate candidate, String relativePath) {
        return ExtractionSupport.sourceRef(relativePath, candidate.startLine(), candidate.snippet(), Map.of(
            "language", "typescript",
            "framework", candidate.framework(),
            "routePath", candidate.path(),
            "routeFullPath", candidate.fullPath(),
            "routeDeclarationKind", candidate.declarationKind(),
            "routeSourceKind", "declared-route"
        ));
    }

    private SourceReference navigationRef(FrontendNavigationCandidate candidate, String relativePath) {
        return ExtractionSupport.sourceRef(relativePath, candidate.startLine(), candidate.snippet(), Map.of(
            "language", "typescript",
            "framework", candidate.framework(),
            "routeSourceKind", candidate.sourceKind(),
            "navigationTargetLiteral", routeEntityFactory.normalizeLiteralPath(candidate.targetLiteral())
        ));
    }
}
