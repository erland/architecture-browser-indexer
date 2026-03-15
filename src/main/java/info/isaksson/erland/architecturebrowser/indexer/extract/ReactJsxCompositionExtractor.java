package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReactJsxCompositionExtractor {
    private static final Pattern JSX_COMPONENT_TAG = Pattern.compile("<\\s*([A-Z][A-Za-z0-9_]*)\\b");

    private ReactJsxCompositionExtractor() {
    }

    static void extract(
        ExtractionAccumulator accumulator,
        String relativePath,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (accumulator == null || namedEntities == null || namedEntities.isEmpty()) {
            return;
        }
        for (ExtractedEntityFact entity : namedEntities.values()) {
            if (!isReactComponentCandidate(entity, relativePath)) {
                continue;
            }
            SourceReference ref = primaryRef(entity, relativePath);
            String snippet = ref == null ? "" : Objects.toString(ref.snippet(), "");
            if (!snippet.contains("<")) {
                continue;
            }
            LinkedHashSet<String> renderedComponents = jsxComponentNames(snippet);
            renderedComponents.remove(entity.name());
            for (String renderedName : renderedComponents) {
                if (renderedName == null || renderedName.isBlank()) {
                    continue;
                }
                ExtractedEntityFact targetEntity = namedEntities.get(renderedName);
                boolean resolved = targetEntity != null;
                if (!resolved) {
                    targetEntity = ExtractionSupport.inferredTypeEntity(
                        "react",
                        EntityKind.UI_MODULE,
                        renderedName,
                        relativePath,
                        refLine(entity),
                        Map.of(
                            "framework", "react",
                            "targetClassification", "react-component-target",
                            "resolution", "inferred-react-jsx-composition-target",
                            "external", false,
                            "inferredInternal", true
                        )
                    );
                    accumulator.addEntity(targetEntity);
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    entity.id(),
                    targetEntity.id(),
                    renderedName,
                    ref,
                    "typescript",
                    frameworkRelationshipMetadata(resolved)
                ));
            }
        }
    }

    private static boolean isReactComponentCandidate(ExtractedEntityFact entity, String relativePath) {
        if (entity == null || entity.name() == null || entity.name().isBlank()) {
            return false;
        }
        if (!(entity.kind() == EntityKind.FUNCTION || entity.kind() == EntityKind.CLASS)) {
            return false;
        }
        String name = entity.name();
        if (!Character.isUpperCase(name.codePointAt(0))) {
            return false;
        }
        SourceReference ref = primaryRef(entity, relativePath);
        String snippet = ref == null ? "" : Objects.toString(ref.snippet(), "");
        String path = ref == null ? relativePath : Objects.toString(ref.path(), relativePath);
        String lowerSnippet = snippet.toLowerCase(java.util.Locale.ROOT);
        return path.endsWith(".tsx") || snippet.contains("<") || lowerSnippet.contains("react.createelement");
    }

    private static LinkedHashSet<String> jsxComponentNames(String snippet) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (snippet == null || snippet.isBlank()) {
            return result;
        }
        Matcher matcher = JSX_COMPONENT_TAG.matcher(snippet);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (name != null && !name.isBlank()) {
                result.add(name);
            }
        }
        return result;
    }

    private static Map<String, Object> frameworkRelationshipMetadata(boolean resolved) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "react");
        metadata.put("frameworkRelationship", "renders");
        metadata.put("dependencySource", "react:jsx-renders");
        metadata.put("dependencyCategory", "framework");
        metadata.put("targetClassification", "react-component-target");
        metadata.put("dependencyTargetBoundary", "internal");
        metadata.put("resolvedFromJsxComposition", resolved);
        return Map.copyOf(metadata);
    }

    private static SourceReference primaryRef(ExtractedEntityFact entity, String relativePath) {
        return entity.sourceRefs().isEmpty()
            ? ExtractionSupport.sourceRef(relativePath, 1, entity.name(), Map.of("language", "typescript", "framework", "react"))
            : entity.sourceRefs().getFirst();
    }

    private static int refLine(ExtractedEntityFact entity) {
        return entity.sourceRefs().isEmpty() || entity.sourceRefs().getFirst().startLine() == null
            ? 1
            : entity.sourceRefs().getFirst().startLine();
    }
}
