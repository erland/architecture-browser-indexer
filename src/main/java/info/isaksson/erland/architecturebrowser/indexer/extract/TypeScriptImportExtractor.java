package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TypeScriptImportExtractor {
    private static final Pattern IMPORT_FROM_SNIPPET = Pattern.compile("from\\s+['\\\"]([^'\\\"]+)['\\\"]");
    private static final Pattern IMPORT_SIDE_EFFECT_SNIPPET = Pattern.compile("^import\\s+['\\\"]([^'\\\"]+)['\\\"];?$");

    private TypeScriptImportExtractor() {
    }

    static void extract(TypeScriptExtractionContext context) {
        for (SyntaxNode importNode : SyntaxTreeExtractionSupport.findAllByType(context.root(), Set.of("import_statement"))) {
            String imported = importFromSnippet(importNode.textSnippet());
            if (imported == null || imported.isBlank()) {
                continue;
            }
            int line = SyntaxTreeExtractionSupport.oneBasedLine(importNode);
            ImportClassification classification = classifyImport(importNode.textSnippet(), imported);
            var target = classification.internalTarget()
                ? ExtractionSupport.internalDependencyEntity("typescript", imported, context.relativePath(), line, classification.targetMetadata())
                : ExtractionSupport.externalDependencyEntity("typescript", imported, context.relativePath(), line, classification.targetMetadata());
            context.accumulator().addEntity(target);
            context.accumulator().addRelationship(ExtractionSupport.dependencyRelationship(
                context.fileEntity().id(), target.id(), imported,
                ExtractionSupport.sourceRef(context.relativePath(), line, importNode.textSnippet(), Map.of("language", "typescript", "kind", "import")),
                "typescript",
                classification.relationshipMetadata()
            ));
        }
    }

    private static ImportClassification classifyImport(String importSnippet, String imported) {
        boolean sideEffect = isSideEffectImport(importSnippet);
        boolean typeOnly = isTypeOnlyImport(importSnippet);
        boolean relative = imported.startsWith("./") || imported.startsWith("../");
        String importKind = sideEffect ? "sideEffect" : (typeOnly ? "typeOnly" : (relative ? "relative" : "package"));
        String targetClassification = relative ? "inferred-internal-module" : "external-package-target";
        String targetBoundary = relative ? "internal" : "external";
        java.util.Map<String, Object> relationshipMetadata = new java.util.LinkedHashMap<>();
        relationshipMetadata.put("importKind", importKind);
        relationshipMetadata.put("importTargetBoundary", targetBoundary);
        relationshipMetadata.put("targetClassification", targetClassification);
        relationshipMetadata.put("dependencySource", "import");
        relationshipMetadata.put("dependencyCategory", relative ? "internal-module" : "external-package");
        java.util.Map<String, Object> targetMetadata = new java.util.LinkedHashMap<>();
        targetMetadata.put("importKind", importKind);
        targetMetadata.put("targetClassification", targetClassification);
        targetMetadata.put("resolution", relative ? "relative-import" : "package-import");
        targetMetadata.put("packageImport", !relative);
        return new ImportClassification(importKind, relative, java.util.Map.copyOf(relationshipMetadata), java.util.Map.copyOf(targetMetadata));
    }

    private static boolean isTypeOnlyImport(String snippet) {
        return snippet != null && snippet.strip().matches("^import\\s+type\\b.*");
    }

    private static boolean isSideEffectImport(String snippet) {
        return snippet != null && snippet.strip().matches("^import\\s+['\\\"].*['\\\"];?$");
    }

    private static String importFromSnippet(String snippet) {
        if (snippet == null) {
            return null;
        }
        Matcher fromMatcher = IMPORT_FROM_SNIPPET.matcher(snippet);
        if (fromMatcher.find()) {
            return fromMatcher.group(1);
        }
        Matcher sideEffectMatcher = IMPORT_SIDE_EFFECT_SNIPPET.matcher(snippet.strip());
        return sideEffectMatcher.find() ? sideEffectMatcher.group(1) : null;
    }

    private record ImportClassification(
        String importKind,
        boolean internalTarget,
        Map<String, Object> relationshipMetadata,
        Map<String, Object> targetMetadata
    ) {
    }
}
