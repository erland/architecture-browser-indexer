package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.LinkedHashMap;
import java.util.Map;

final class FrontendRouteEntityFactory {
    ExtractedEntityFact routeEntity(String framework, String relativePath, String routePath, String fullPath, int line, String snippet,
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

    ExtractedEntityFact inferredRouteEntity(String framework, String targetLiteral, String relativePath, int line, String sourceKind) {
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

    String normalizeLiteralPath(String targetLiteral) {
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
