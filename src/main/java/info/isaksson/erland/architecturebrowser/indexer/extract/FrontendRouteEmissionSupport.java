package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class FrontendRouteEmissionSupport {

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
        if (candidates != null) {
            for (FrontendRouteCandidate candidate : candidates) {
                ExtractedEntityFact routeEntity = routeEntity(candidate.framework(), relativePath, candidate.path(), candidate.fullPath(), candidate.startLine(), candidate.snippet(),
                    candidate.declarationKind(), candidate.redirectTarget());
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
                if (!candidate.redirectTarget().isBlank()) {
                    addRouteLiteralRelationship(accumulator, candidate, routeEntity, candidate.redirectTarget(), relativePath, "redirects", "route-redirect-target");
                }
            }
        }
        if (navigationCandidates != null && !navigationCandidates.isEmpty()) {
            for (FrontendNavigationCandidate candidate : navigationCandidates) {
                ExtractedEntityFact sourceEntity = findNavigationSourceEntity(namedEntities, relativePath, candidate.startLine(), candidate.framework());
                if (sourceEntity == null) {
                    continue;
                }
                ExtractedEntityFact targetRouteEntity = inferredRouteEntity(candidate.framework(), candidate.targetLiteral(), relativePath, candidate.startLine(), candidate.sourceKind());
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
        ExtractedEntityFact targetRouteEntity = inferredRouteEntity(candidate.framework(), targetLiteral, relativePath, candidate.startLine(), frameworkRelationship);
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

    private ExtractedEntityFact routeEntity(String framework, String relativePath, String routePath, String fullPath, int line, String snippet,
                                            String declarationKind, String redirectTarget) {
        String qualifiedName = framework + "-route:" + fullPath;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", framework);
        metadata.put("route", true);
        metadata.put("routePath", routePath);
        metadata.put("routeFullPath", fullPath);
        metadata.put("entityRole", "route");
        metadata.put("targetClassification", "route-node");
        metadata.put("external", false);
        metadata.put("inferredInternal", true);
        metadata.put("routeSnippet", snippet == null ? "" : snippet);
        metadata.put("routeDeclarationKind", declarationKind == null || declarationKind.isBlank() ? "route-object" : declarationKind);
        metadata.put("routeSourceKind", "declared-route");
        if (redirectTarget != null && !redirectTarget.isBlank()) {
            metadata.put("redirectTargetLiteral", redirectTarget);
        }
        return ExtractionSupport.inferredTypeEntity(framework, EntityKind.UI_MODULE, qualifiedName, relativePath, line, Map.copyOf(metadata));
    }

    private ExtractedEntityFact inferredRouteEntity(String framework, String targetLiteral, String relativePath, int line, String sourceKind) {
        String fullPath = normalizeLiteralPath(targetLiteral);
        String qualifiedName = framework + "-route:" + fullPath;
        return ExtractionSupport.inferredTypeEntity(framework, EntityKind.UI_MODULE, qualifiedName, relativePath, line, Map.of(
            "framework", framework,
            "route", true,
            "routePath", targetLiteral,
            "routeFullPath", fullPath,
            "entityRole", "route",
            "targetClassification", "route-node",
            "external", false,
            "inferredInternal", true,
            "routeSourceKind", sourceKind,
            "routeLiteralOnly", true
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
            "navigationTargetLiteral", normalizeLiteralPath(candidate.targetLiteral())
        ));
    }

    private ExtractedEntityFact findNavigationSourceEntity(Map<String, ExtractedEntityFact> namedEntities, String relativePath, int line, String framework) {
        if (namedEntities == null || namedEntities.isEmpty()) {
            return null;
        }
        List<ExtractedEntityFact> sameFileEntities = namedEntities.values().stream()
            .filter(entity -> entity.sourceRefs().stream().anyMatch(ref -> relativePath.equals(ref.path())))
            .toList();
        if (sameFileEntities.isEmpty()) {
            return null;
        }
        Comparator<ExtractedEntityFact> comparator = Comparator
            .comparingInt((ExtractedEntityFact entity) -> frameworkScore(entity, framework))
            .thenComparingInt(entity -> lineContainmentScore(entity, line))
            .thenComparingInt(entity -> distanceToLine(entity, line));
        return sameFileEntities.stream()
            .max(comparator)
            .orElse(null);
    }

    private int frameworkScore(ExtractedEntityFact entity, String framework) {
        int score = 0;
        if (framework.equals(entity.metadata().get("framework"))) {
            score += 10;
        }
        if ("page-or-router".equals(entity.metadata().get("uiProfile"))) {
            score += 5;
        }
        return score;
    }

    private int lineContainmentScore(ExtractedEntityFact entity, int line) {
        int best = Integer.MIN_VALUE;
        for (SourceReference ref : entity.sourceRefs()) {
            Integer startLine = ref.startLine();
            Integer endLine = ref.endLine();
            if (startLine == null) {
                continue;
            }
            int score;
            if (endLine != null && startLine <= line && line <= endLine) {
                score = 100;
            } else if (startLine <= line) {
                score = 50;
            } else {
                score = -Math.abs(startLine - line);
            }
            if (score > best) {
                best = score;
            }
        }
        return best == Integer.MIN_VALUE ? 0 : best;
    }

    private int distanceToLine(ExtractedEntityFact entity, int line) {
        return entity.sourceRefs().stream()
            .map(SourceReference::startLine)
            .filter(Objects::nonNull)
            .mapToInt(startLine -> -Math.abs(line - startLine))
            .max()
            .orElse(Integer.MIN_VALUE);
    }

    private String normalizeLiteralPath(String targetLiteral) {
        if (targetLiteral == null || targetLiteral.isBlank()) {
            return "/";
        }
        String normalized = targetLiteral.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/+", "/");
    }
}
