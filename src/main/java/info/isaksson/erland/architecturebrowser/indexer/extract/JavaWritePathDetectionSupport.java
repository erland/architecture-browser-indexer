package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaWritePathDetectionSupport {
    private JavaWritePathDetectionSupport() {}

    record DetectedWritePath(String operation, String writeKind, String argumentExpression, String viaField, String viaType) {}

    static List<DetectedWritePath> detectJpaWriteOperations(ExtractedEntityFact methodEntity, String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<DetectedWritePath> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(persist|merge|remove)\\s*\\(([^)]*)\\)", Pattern.DOTALL).matcher(snippet);
        while (matcher.find()) {
            result.add(new DetectedWritePath(matcher.group(2).toLowerCase(Locale.ROOT), "entity-manager", matcher.group(3).strip(), matcher.group(1), null));
        }
        return List.copyOf(result);
    }

    static List<DetectedWritePath> detectRepositoryWriteOperations(ExtractedEntityFact methodEntity, String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        List<DetectedWritePath> result = new ArrayList<>();
        Matcher callMatcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(saveAndFlush|save|update|delete|remove)\\s*\\(([^)]*)\\)", Pattern.DOTALL).matcher(snippet);
        while (callMatcher.find()) {
            String operation = normalizeWriteOperation(callMatcher.group(2));
            result.add(new DetectedWritePath(operation, "repository-call", callMatcher.group(3).strip(), callMatcher.group(1), null));
        }
        String ownerQualifiedName = String.valueOf(methodEntity.metadata().getOrDefault("ownerQualifiedName", ""));
        String loweredOwner = ownerQualifiedName.toLowerCase(Locale.ROOT);
        String methodName = methodEntity.name() == null ? "" : methodEntity.name();
        if (loweredOwner.contains("repository") || loweredOwner.contains("repo")) {
            String operation = normalizeWriteOperation(methodName);
            if (operation != null) {
                List<String> parameterTypes = JavaDeclaredTypeSupport.metadataStringList(methodEntity.metadata().get("parameterTypes"));
                String params = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
                List<String> paramNames = JavaDeclaredTypeSupport.extractParameterNames(params);
                for (int i = 0; i < Math.min(parameterTypes.size(), paramNames.size()); i++) {
                    String type = JavaRelationshipEvidenceEmitter.normalizeTypeReference(parameterTypes.get(i));
                    if (!type.isBlank()) {
                        result.add(new DetectedWritePath(operation, "repository-method", paramNames.get(i), null, type));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    static String normalizeWriteOperation(String rawOperation) {
        if (rawOperation == null || rawOperation.isBlank()) {
            return null;
        }
        String value = rawOperation.toLowerCase(Locale.ROOT);
        if (value.contains("save")) return "persist";
        if (value.contains("merge") || value.contains("update")) return "merge";
        if (value.contains("delete") || value.contains("remove")) return "remove";
        if (value.equals("persist")) return "persist";
        return null;
    }

    static Map<String, String> collectMethodVariableTypes(ExtractedEntityFact methodEntity, String snippet) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String params = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        List<String> paramTypes = JavaDeclaredTypeSupport.metadataStringList(methodEntity.metadata().get("parameterTypes"));
        List<String> paramNames = JavaDeclaredTypeSupport.extractParameterNames(params);
        for (int i = 0; i < Math.min(paramTypes.size(), paramNames.size()); i++) {
            String type = JavaRelationshipEvidenceEmitter.normalizeTypeReference(paramTypes.get(i));
            if (!type.isBlank()) {
                result.putIfAbsent(paramNames.get(i), type);
            }
        }
        if (snippet != null && !snippet.isBlank()) {
            Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?)\\s+([A-Za-z_$][\\w$]*)\\s*=", Pattern.DOTALL).matcher(snippet);
            while (matcher.find()) {
                String type = JavaRelationshipEvidenceEmitter.normalizeTypeReference(matcher.group(1));
                String name = matcher.group(2);
                if (!type.isBlank() && !isJavaPrimitiveOrKeyword(type)) {
                    result.putIfAbsent(name, type);
                }
            }
        }
        return Map.copyOf(result);
    }

    static Optional<String> resolveWriteTargetEntityType(String argumentExpression, Map<String, String> variableTypes) {
        if (argumentExpression == null || argumentExpression.isBlank()) {
            return Optional.empty();
        }
        String arg = argumentExpression.strip();
        Matcher newMatcher = Pattern.compile("new\\s+([A-Za-z_$][\\w.$]*)\\b").matcher(arg);
        if (newMatcher.find()) {
            return Optional.of(newMatcher.group(1));
        }
        Matcher identifierMatcher = Pattern.compile("([A-Za-z_$][\\w$]*)").matcher(arg);
        while (identifierMatcher.find()) {
            String candidate = identifierMatcher.group(1);
            if (variableTypes.containsKey(candidate)) {
                return Optional.of(variableTypes.get(candidate));
            }
        }
        return Optional.empty();
    }

    private static boolean isJavaPrimitiveOrKeyword(String candidate) {
        return Set.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void", "var", "this", "super"
        ).contains(candidate);
    }
}
