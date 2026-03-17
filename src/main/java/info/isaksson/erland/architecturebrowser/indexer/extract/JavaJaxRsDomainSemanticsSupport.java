package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaJaxRsDomainSemanticsSupport {
    private JavaJaxRsDomainSemanticsSupport() {}

    static boolean isJaxRsResource(ExtractedEntityFact entity) {
        return JavaDeclaredTypeSupport.metadataStringList(entity.metadata().get("annotations")).stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .anyMatch(value -> value.endsWith("path"));
    }

    static Optional<String> jaxRsHttpMethod(List<String> annotations) {
        for (String annotation : annotations) {
            String value = annotation.toLowerCase(Locale.ROOT);
            if (value.endsWith("get")) return Optional.of("GET");
            if (value.endsWith("post")) return Optional.of("POST");
            if (value.endsWith("put")) return Optional.of("PUT");
            if (value.endsWith("delete")) return Optional.of("DELETE");
            if (value.endsWith("patch")) return Optional.of("PATCH");
            if (value.endsWith("head")) return Optional.of("HEAD");
            if (value.endsWith("options")) return Optional.of("OPTIONS");
        }
        return Optional.empty();
    }

    static Optional<String> extractJaxRsPath(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        Matcher valueMatcher = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Path\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"").matcher(snippet);
        if (valueMatcher.find()) {
            return Optional.ofNullable(valueMatcher.group(1));
        }
        Matcher bareMatcher = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Path\\s*\\(\\s*\\)").matcher(snippet);
        if (bareMatcher.find()) {
            return Optional.of("/");
        }
        return Optional.empty();
    }

    static String normalizeJaxRsPath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String normalized = value.strip();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("//+", "/");
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    static String normalizeJaxRsEndpointPath(String classPath, String methodPath) {
        String base = normalizeJaxRsPath(classPath);
        String method = normalizeJaxRsPath(methodPath);
        if ("/".equals(base) && "/".equals(method)) {
            return "/";
        }
        if ("/".equals(base)) {
            return method;
        }
        if ("/".equals(method)) {
            return base;
        }
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
            + (method.startsWith("/") ? method : "/" + method);
    }

    static List<Map<String, String>> extractJaxRsParameterDetails(String parameterSnippet) {
        if (parameterSnippet == null || parameterSnippet.isBlank() || "()".equals(parameterSnippet.strip())) {
            return List.of();
        }
        String inner = parameterSnippet.strip();
        if (inner.startsWith("(")) {
            inner = inner.substring(1);
        }
        if (inner.endsWith(")")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        if (inner.isBlank()) {
            return List.of();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (String part : splitTopLevelCommaSeparated(inner)) {
            String snippet = part.strip();
            if (snippet.isBlank()) {
                continue;
            }
            LinkedHashMap<String, String> detail = new LinkedHashMap<>();
            detail.put("name", extractParameterName(snippet));
            detail.put("declaredType", extractParameterDeclaredType(snippet));
            detail.put("parameterKind", classifyJaxRsParameter(snippet));
            result.add(Map.copyOf(detail));
        }
        return List.copyOf(result);
    }

    static List<String> splitTopLevelCommaSeparated(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return List.of();
        }
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '<' || ch == '(' || ch == '[') {
                depth++;
            } else if (ch == '>' || ch == ')' || ch == ']') {
                depth = Math.max(0, depth - 1);
            } else if (ch == ',' && depth == 0) {
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            result.add(tail);
        }
        return List.copyOf(result);
    }

    static String extractParameterName(String snippet) {
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*$").matcher(snippet);
        return matcher.find() ? matcher.group(1) : "";
    }

    static String extractParameterDeclaredType(String snippet) {
        String value = snippet == null ? "" : snippet
            .replaceAll("@[A-Za-z_][\\w.]*\\s*(\\([^)]*\\))?", " ")
            .replaceAll("\\bfinal\\b", " ")
            .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w.$]*(?:\\s*<[^>{}]+>)?(?:\\s*\\[\\])*)\\s+[A-Za-z_$][\\w$]*$").matcher(value);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", " ").trim() : "";
    }

    static String classifyJaxRsParameter(String snippet) {
        String lower = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        if (lower.contains("@pathparam")) return "PATH";
        if (lower.contains("@queryparam")) return "QUERY";
        if (lower.contains("@headerparam")) return "HEADER";
        if (lower.contains("@cookieparam")) return "COOKIE";
        if (lower.contains("@matrixparam")) return "MATRIX";
        if (lower.contains("@formparam")) return "FORM";
        if (lower.contains("@beanparam")) return "BEAN";
        if (lower.contains("@context")) return "CONTEXT";
        return "BODY";
    }
}
