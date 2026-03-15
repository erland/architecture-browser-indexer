package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReactCustomHookExtractor {
    private static final Pattern CUSTOM_HOOK_CALL_PATTERN = Pattern.compile("\\b(use[A-Z][A-Za-z0-9_$]*)\\s*(?:<[^>]+>)?\\s*\\(");
    private static final Set<String> REACT_BUILTIN_HOOKS = Set.of(
        "useState", "useEffect", "useLayoutEffect", "useInsertionEffect", "useMemo", "useCallback", "useRef",
        "useContext", "useReducer", "useImperativeHandle", "useDeferredValue", "useTransition", "useId",
        "useSyncExternalStore", "useOptimistic", "useActionState", "useDebugValue"
    );

    private ReactCustomHookExtractor() {
    }

    static void extract(
        ExtractionAccumulator accumulator,
        String relativePath,
        String sourceText,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (accumulator == null || relativePath == null || sourceText == null || sourceText.isBlank() || namedEntities == null || namedEntities.isEmpty()) {
            return;
        }
        if (!looksLikeReactHookSource(relativePath, sourceText, namedEntities)) {
            return;
        }

        LinkedHashMap<String, ExtractedEntityFact> customHooks = new LinkedHashMap<>();
        for (ExtractedEntityFact entity : namedEntities.values()) {
            if (!isCustomHookDeclaration(entity)) {
                continue;
            }
            ExtractedEntityFact hookEntity = customHookEntity(accumulator, entity, hookClassification(entity, relativePath));
            customHooks.put(entity.name(), hookEntity);
        }

        for (ExtractedEntityFact entity : namedEntities.values()) {
            if (!isReactHookActor(entity, relativePath)) {
                continue;
            }
            SourceReference ref = primaryRef(entity, relativePath);
            String snippet = ref == null ? "" : Objects.toString(ref.snippet(), "");
            if (snippet.isBlank()) {
                continue;
            }
            for (String hookName : customHookCalls(snippet)) {
                if (hookName.equals(entity.name())) {
                    continue;
                }
                ExtractedEntityFact hookEntity = customHooks.get(hookName);
                boolean resolved = hookEntity != null;
                if (hookEntity == null) {
                    hookEntity = inferredHookEntity(accumulator, hookName, relativePath, refLine(entity), inferredHookClassification(hookName, snippet, relativePath));
                    customHooks.put(hookName, hookEntity);
                }
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    entity.id(),
                    hookEntity.id(),
                    hookName,
                    ref,
                    "typescript",
                    relationshipMetadata(entity, hookEntity, resolved)
                ));
            }
        }
    }

    private static boolean looksLikeReactHookSource(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerSource = sourceText.toLowerCase(Locale.ROOT);
        if (relativePath.endsWith(".tsx") || relativePath.endsWith(".jsx") || lowerSource.contains("usecontext") || lowerSource.contains("usestate") || lowerSource.contains("useeffect")) {
            return true;
        }
        return namedEntities.values().stream().anyMatch(entity -> entity != null && isCustomHookName(entity.name()));
    }

    private static boolean isCustomHookDeclaration(ExtractedEntityFact entity) {
        if (entity == null || entity.name() == null || entity.name().isBlank()) {
            return false;
        }
        return entity.kind() == EntityKind.FUNCTION && isCustomHookName(entity.name());
    }

    private static boolean isReactHookActor(ExtractedEntityFact entity, String relativePath) {
        if (entity == null || entity.name() == null || entity.name().isBlank()) {
            return false;
        }
        if (entity.kind() != EntityKind.FUNCTION && entity.kind() != EntityKind.CLASS) {
            return false;
        }
        SourceReference ref = primaryRef(entity, relativePath);
        String snippet = ref == null ? "" : Objects.toString(ref.snippet(), "");
        if (snippet.contains("use") && snippet.contains("(")) {
            return true;
        }
        return isCustomHookName(entity.name()) || Character.isUpperCase(entity.name().codePointAt(0));
    }

    private static ExtractedEntityFact customHookEntity(ExtractionAccumulator accumulator, ExtractedEntityFact baseEntity, String hookClassification) {
        Map<String, Object> metadata = new LinkedHashMap<>(baseEntity.metadata());
        metadata.put("framework", "react");
        metadata.put("reactHook", true);
        metadata.put("customHook", true);
        metadata.put("entityRole", "hook");
        metadata.put("uiProfile", "react-hook");
        metadata.put("hookProfile", hookClassification);
        metadata.put("hookClassification", hookClassification);
        metadata.put("declaredReactHook", true);
        metadata.put("external", false);
        metadata.put("inferredInternal", false);
        ExtractedEntityFact enriched = new ExtractedEntityFact(
            baseEntity.id(),
            baseEntity.kind(),
            baseEntity.origin(),
            baseEntity.name(),
            baseEntity.displayName(),
            baseEntity.scopeId(),
            baseEntity.sourceRefs(),
            Map.copyOf(metadata)
        );
        accumulator.addEntity(enriched);
        return enriched;
    }

    private static ExtractedEntityFact inferredHookEntity(
        ExtractionAccumulator accumulator,
        String hookName,
        String relativePath,
        int line,
        String hookClassification
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "react");
        metadata.put("reactHook", true);
        metadata.put("customHook", true);
        metadata.put("entityRole", "hook");
        metadata.put("uiProfile", "react-hook");
        metadata.put("hookProfile", hookClassification);
        metadata.put("hookClassification", hookClassification);
        metadata.put("targetClassification", "react-hook");
        metadata.put("resolution", "inferred-react-hook");
        metadata.put("declaredReactHook", false);
        metadata.put("external", false);
        metadata.put("inferredInternal", true);
        ExtractedEntityFact hookEntity = ExtractionSupport.inferredTypeEntity(
            "react",
            EntityKind.FUNCTION,
            hookName,
            relativePath,
            line,
            Map.copyOf(metadata)
        );
        accumulator.addEntity(hookEntity);
        return hookEntity;
    }

    private static LinkedHashSet<String> customHookCalls(String snippet) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (snippet == null || snippet.isBlank()) {
            return result;
        }
        Matcher matcher = CUSTOM_HOOK_CALL_PATTERN.matcher(snippet);
        while (matcher.find()) {
            String hookName = matcher.group(1);
            if (isCustomHookName(hookName)) {
                result.add(hookName);
            }
        }
        return result;
    }

    private static boolean isCustomHookName(String hookName) {
        return hookName != null && !hookName.isBlank() && hookName.startsWith("use") && hookName.length() > 3
            && Character.isUpperCase(hookName.charAt(3)) && !REACT_BUILTIN_HOOKS.contains(hookName);
    }

    private static Map<String, Object> relationshipMetadata(ExtractedEntityFact actor, ExtractedEntityFact hookEntity, boolean resolved) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "react");
        metadata.put("frameworkRelationship", "usesHook");
        metadata.put("dependencySource", "react:uses-hook");
        metadata.put("dependencyCategory", "framework");
        metadata.put("targetClassification", "react-hook");
        metadata.put("dependencyTargetBoundary", "internal");
        metadata.put("resolvedFromReactHookExtraction", resolved);
        metadata.put("hookClassification", hookEntity.metadata().getOrDefault("hookClassification", "utility"));
        metadata.put("hookConsumerKind", consumerKind(actor));
        return Map.copyOf(metadata);
    }

    private static String consumerKind(ExtractedEntityFact actor) {
        if (actor == null || actor.name() == null || actor.name().isBlank()) {
            return "unknown";
        }
        if (isCustomHookName(actor.name())) {
            return "hook";
        }
        return Character.isUpperCase(actor.name().charAt(0)) ? "component" : "function-or-class";
    }

    private static String hookClassification(ExtractedEntityFact entity, String relativePath) {
        SourceReference ref = primaryRef(entity, relativePath);
        String snippet = ref == null ? "" : Objects.toString(ref.snippet(), "");
        return inferredHookClassification(entity.name(), snippet, relativePath);
    }

    private static String inferredHookClassification(String hookName, String snippet, String relativePath) {
        String lowerSnippet = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        String lowerName = hookName == null ? "" : hookName.toLowerCase(Locale.ROOT);
        String lowerPath = relativePath == null ? "" : relativePath.toLowerCase(Locale.ROOT);
        if (lowerSnippet.contains("usequery") || lowerSnippet.contains("usemutation") || lowerSnippet.contains("fetch(") || lowerSnippet.contains("axios.")
            || lowerName.contains("query") || lowerName.contains("fetch") || lowerPath.contains("/queries/") || lowerPath.contains("/api/")) {
            return "data-fetch";
        }
        if (lowerSnippet.contains("usenavigate") || lowerSnippet.contains("uselocation") || lowerSnippet.contains("useparams") || lowerSnippet.contains("userouter")
            || lowerName.contains("route") || lowerName.contains("navigation") || lowerPath.contains("/routes/")) {
            return "routing";
        }
        if (lowerSnippet.contains("useform") || lowerSnippet.contains("usefield") || lowerSnippet.contains("usecontroller") || lowerName.contains("form") || lowerPath.contains("/forms/")) {
            return "form";
        }
        if (lowerSnippet.contains("usecontext") || lowerSnippet.contains(".provider") || lowerName.contains("auth") || lowerName.contains("context") || lowerPath.contains("/context/")) {
            return "context";
        }
        if (lowerSnippet.contains("usestate") || lowerSnippet.contains("usereducer") || lowerName.contains("state") || lowerName.contains("store") || lowerPath.contains("/state/")) {
            return "state";
        }
        if (lowerSnippet.contains("useeffect") || lowerSnippet.contains("uselayouteffect")) {
            return "effect";
        }
        if (lowerSnippet.contains("usememo") || lowerSnippet.contains("usecallback")) {
            return "memoization";
        }
        return "utility";
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
