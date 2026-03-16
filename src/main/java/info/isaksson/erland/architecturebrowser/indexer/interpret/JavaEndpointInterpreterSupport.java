package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaEndpointInterpreterSupport {
    private static final Pattern PATH_PATTERN = Pattern.compile("([\\\"\'])(/[^\\\"\']*)\\1");

    Optional<String> controllerBasePath(ExtractedEntityFact entity) {
        String snippet = InterpretationContext.primaryRef(entity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(entity).snippet());
        return extractPath(snippet);
    }

    String normalizeEndpointPath(String classLevelPath, String methodPath) {
        String base = classLevelPath == null ? "" : classLevelPath.strip();
        String method = methodPath == null ? "" : methodPath.strip();
        if (base.isEmpty() && method.isEmpty()) {
            return "/";
        }
        if (base.isEmpty()) {
            return normalizeSinglePath(method);
        }
        if (method.isEmpty() || "/".equals(method)) {
            return normalizeSinglePath(base);
        }
        String normalizedBase = normalizeSinglePath(base);
        String normalizedMethod = normalizeSinglePath(method);
        if ("/".equals(normalizedBase)) {
            return normalizedMethod;
        }
        if ("/".equals(normalizedMethod)) {
            return normalizedBase;
        }
        return (normalizedBase.endsWith("/") ? normalizedBase.substring(0, normalizedBase.length() - 1) : normalizedBase)
            + (normalizedMethod.startsWith("/") ? normalizedMethod : "/" + normalizedMethod);
    }

    boolean isController(ExtractedEntityFact entity) {
        List<String> annotations = InterpretationContext.listMetadata(entity, "annotations");
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        return matchesAny(annotations, "restcontroller", "controller", "path") || lowerName.endsWith("controller");
    }

    Optional<String> endpointAnnotation(List<String> annotations) {
        return annotations.stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .filter(value -> value.endsWith("getmapping") || value.endsWith("postmapping") || value.endsWith("putmapping")
                || value.endsWith("deletemapping") || value.endsWith("patchmapping") || value.endsWith("requestmapping"))
            .findFirst();
    }

    String httpMethodForAnnotation(String annotation) {
        String normalized = annotation.toLowerCase(Locale.ROOT);
        if (normalized.endsWith("getmapping")) {
            return "GET";
        }
        if (normalized.endsWith("postmapping")) {
            return "POST";
        }
        if (normalized.endsWith("putmapping")) {
            return "PUT";
        }
        if (normalized.endsWith("deletemapping")) {
            return "DELETE";
        }
        if (normalized.endsWith("patchmapping")) {
            return "PATCH";
        }
        return "REQUEST";
    }

    Optional<String> extractPath(String sourceSnippet) {
        if (sourceSnippet == null) {
            return Optional.empty();
        }
        Matcher matcher = PATH_PATTERN.matcher(sourceSnippet);
        return matcher.find() ? Optional.ofNullable(matcher.group(2)) : Optional.empty();
    }

    private String normalizeSinglePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String normalized = value.startsWith("/") ? value : "/" + value;
        return normalized.replaceAll("//+", "/");
    }

    private static boolean matchesAny(List<String> annotations, String... values) {
        List<String> normalized = annotations.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
        for (String candidate : values) {
            if (normalized.stream().anyMatch(annotation -> annotation.endsWith(candidate.toLowerCase(Locale.ROOT)))) {
                return true;
            }
        }
        return false;
    }
}
