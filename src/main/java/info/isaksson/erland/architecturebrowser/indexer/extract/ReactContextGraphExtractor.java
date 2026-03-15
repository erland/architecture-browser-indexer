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

final class ReactContextGraphExtractor {
    private static final Pattern CREATE_CONTEXT_PATTERN = Pattern.compile("\\b(?:export\\s+)?(?:const|let|var)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(?:React\\s*\\.\\s*)?createContext\\s*\\(");
    private static final Pattern CONTEXT_PROVIDER_PATTERN = Pattern.compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\.\\s*Provider\\b");
    private static final Pattern USE_CONTEXT_PATTERN = Pattern.compile("\\b(?:React\\s*\\.\\s*)?useContext\\s*(?:<[^>]+>)?\\s*\\(\\s*([A-Za-z_$][A-Za-z0-9_$]*)");

    private ReactContextGraphExtractor() {
    }

    static void extract(
        ExtractionAccumulator accumulator,
        String relativePath,
        String sourceText,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (accumulator == null || relativePath == null || sourceText == null || sourceText.isBlank()) {
            return;
        }
        if (!looksLikeReactContextSource(relativePath, sourceText, namedEntities)) {
            return;
        }

        LinkedHashMap<String, ExtractedEntityFact> contexts = new LinkedHashMap<>();
        Matcher declaredContexts = CREATE_CONTEXT_PATTERN.matcher(sourceText);
        while (declaredContexts.find()) {
            String contextName = declaredContexts.group(1);
            if (contextName == null || contextName.isBlank()) {
                continue;
            }
            ExtractedEntityFact contextEntity = contextEntity(contexts, accumulator, contextName, relativePath, oneBasedLine(sourceText, declaredContexts.start()), true);
            contexts.putIfAbsent(contextName, contextEntity);
        }

        if (namedEntities == null || namedEntities.isEmpty()) {
            return;
        }

        for (ExtractedEntityFact entity : namedEntities.values()) {
            if (!isReactContextActor(entity, relativePath)) {
                continue;
            }
            SourceReference ref = primaryRef(entity, relativePath);
            String snippet = ref == null ? "" : Objects.toString(ref.snippet(), "");
            if (snippet.isBlank()) {
                continue;
            }

            LinkedHashSet<String> providedContexts = matches(CONTEXT_PROVIDER_PATTERN, snippet);
            for (String contextName : providedContexts) {
                if (contextName.equals(entity.name())) {
                    continue;
                }
                boolean resolved = contexts.containsKey(contextName);
                ExtractedEntityFact contextEntity = contextEntity(contexts, accumulator, contextName, relativePath, refLine(entity), resolved);
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    entity.id(),
                    contextEntity.id(),
                    contextName,
                    ref,
                    "typescript",
                    relationshipMetadata("providesContext", resolved, snippet)
                ));
            }

            LinkedHashSet<String> consumedContexts = matches(USE_CONTEXT_PATTERN, snippet);
            for (String contextName : consumedContexts) {
                if (contextName.equals(entity.name())) {
                    continue;
                }
                boolean resolved = contexts.containsKey(contextName);
                ExtractedEntityFact contextEntity = contextEntity(contexts, accumulator, contextName, relativePath, refLine(entity), resolved);
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    entity.id(),
                    contextEntity.id(),
                    contextName,
                    ref,
                    "typescript",
                    relationshipMetadata("consumesContext", resolved, snippet)
                ));
            }
        }
    }

    private static boolean looksLikeReactContextSource(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerSource = sourceText.toLowerCase(java.util.Locale.ROOT);
        if (relativePath.endsWith(".tsx") || relativePath.endsWith(".jsx") || lowerSource.contains("createcontext") || lowerSource.contains("usecontext") || sourceText.contains(".Provider")) {
            return true;
        }
        if (namedEntities == null) {
            return false;
        }
        return namedEntities.values().stream().anyMatch(entity -> {
            String name = entity.name();
            return name != null && (name.endsWith("Provider") || name.endsWith("Context") || name.startsWith("use"));
        });
    }

    private static boolean isReactContextActor(ExtractedEntityFact entity, String relativePath) {
        if (entity == null || entity.name() == null || entity.name().isBlank()) {
            return false;
        }
        if (!(entity.kind() == EntityKind.FUNCTION || entity.kind() == EntityKind.CLASS)) {
            return false;
        }
        SourceReference ref = primaryRef(entity, relativePath);
        String snippet = ref == null ? "" : Objects.toString(ref.snippet(), "");
        if (snippet.contains(".Provider") || snippet.contains("useContext") || snippet.contains("React.useContext")) {
            return true;
        }
        return entity.name().startsWith("use") || Character.isUpperCase(entity.name().codePointAt(0));
    }

    private static ExtractedEntityFact contextEntity(
        Map<String, ExtractedEntityFact> contexts,
        ExtractionAccumulator accumulator,
        String contextName,
        String relativePath,
        int line,
        boolean declaredInFile
    ) {
        ExtractedEntityFact existing = contexts.get(contextName);
        if (existing != null) {
            return existing;
        }
        ExtractedEntityFact contextEntity = ExtractionSupport.inferredTypeEntity(
            "react",
            EntityKind.UI_MODULE,
            contextName,
            relativePath,
            line,
            Map.of(
                "framework", "react",
                "reactContext", true,
                "entityRole", "context",
                "uiProfile", "react-context",
                "targetClassification", "react-context",
                "resolution", declaredInFile ? "declared-react-context" : "inferred-react-context",
                "declaredReactContext", declaredInFile,
                "external", false,
                "inferredInternal", true
            )
        );
        accumulator.addEntity(contextEntity);
        contexts.put(contextName, contextEntity);
        return contextEntity;
    }

    private static LinkedHashSet<String> matches(Pattern pattern, String snippet) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (pattern == null || snippet == null || snippet.isBlank()) {
            return result;
        }
        Matcher matcher = pattern.matcher(snippet);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private static Map<String, Object> relationshipMetadata(String frameworkRelationship, boolean resolved, String snippet) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "react");
        metadata.put("frameworkRelationship", frameworkRelationship);
        metadata.put("dependencySource", "react:" + ("providesContext".equals(frameworkRelationship) ? "provides-context" : "consumes-context"));
        metadata.put("dependencyCategory", "framework");
        metadata.put("targetClassification", "react-context");
        metadata.put("dependencyTargetBoundary", "internal");
        metadata.put("resolvedFromReactContextExtraction", resolved);
        metadata.put("providerWrapsChildren", "providesContext".equals(frameworkRelationship)
            && snippet != null
            && (snippet.contains("children") || snippet.contains("props.children")));
        metadata.put("consumerKind", consumerKind(snippet));
        return Map.copyOf(metadata);
    }

    private static String consumerKind(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return "unknown";
        }
        String trimmed = snippet.strip();
        if (trimmed.startsWith("export function use") || trimmed.startsWith("function use") || trimmed.contains("= () =>") && trimmed.contains("useContext")) {
            return "hook";
        }
        return "component-or-class";
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

    private static int oneBasedLine(String text, int offset) {
        int line = 1;
        int max = Math.min(Math.max(offset, 0), text.length());
        for (int i = 0; i < max; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
