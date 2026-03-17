package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaExtractionSemanticsSupport {
    private JavaExtractionSemanticsSupport() {
    }

    record DetectedWritePath(String operation, String writeKind, String argumentExpression, String viaField, String viaType) {}
    record PublishedCdiEvent(String eventType, boolean async, String publisherField) {}
    record ObservedCdiEvent(String eventType, boolean async, List<String> qualifiers) {}
    record ResolvedJavaType(String entityId, String label, EntityKind kind) {}

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
                List<String> parameterTypes = metadataStringList(methodEntity.metadata().get("parameterTypes"));
                String params = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
                List<String> paramNames = extractParameterNames(params);
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
        List<String> paramTypes = metadataStringList(methodEntity.metadata().get("parameterTypes"));
        List<String> paramNames = extractParameterNames(params);
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

    static String exactNodeSnippet(String sourceText, SyntaxNode node) {
        return JavaSourceReferenceSupport.exactNodeSnippet(sourceText, node);
    }

    static List<String> extractParameterNames(String parameterSnippet) {
        return JavaDeclaredTypeSupport.extractParameterNames(parameterSnippet);
    }

    static List<PublishedCdiEvent> detectCdiPublishedEvents(String methodSnippet, String ownerTypeSnippet) {
        if (methodSnippet == null || methodSnippet.isBlank()) {
            return List.of();
        }
        LinkedHashMap<String, PublishedCdiEvent> events = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(fireAsync|fire)\\s*\\((.*?)\\)", Pattern.DOTALL).matcher(methodSnippet);
        while (matcher.find()) {
            String publisherField = matcher.group(1);
            boolean async = "fireAsync".equals(matcher.group(2));
            String args = matcher.group(3) == null ? "" : matcher.group(3);
            String eventType = extractCdiEventTypeFromField(ownerTypeSnippet, publisherField)
                .or(() -> extractCdiEventTypeFromArguments(args))
                .orElse(null);
            if (eventType == null || eventType.isBlank()) {
                continue;
            }
            events.putIfAbsent(publisherField + ":" + eventType + ":" + async, new PublishedCdiEvent(eventType, async, publisherField));
        }
        return List.copyOf(events.values());
    }

    static Optional<String> extractCdiEventTypeFromField(String ownerTypeSnippet, String publisherField) {
        if (ownerTypeSnippet == null || ownerTypeSnippet.isBlank() || publisherField == null || publisherField.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("(?:^|[;{}\\s])(?:@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?\\s*)*(?:public|protected|private)?\\s*(?:static\\s+|final\\s+|transient\\s+)*?(?:[A-Za-z_][\\w.]*\\.)?Event\\s*<\\s*([^>]+?)\\s*>\\s+" + Pattern.quote(publisherField) + "\\b", Pattern.DOTALL).matcher(ownerTypeSnippet);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1)).map(String::trim).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }

    static Optional<String> extractCdiEventTypeFromArguments(String args) {
        if (args == null || args.isBlank()) {
            return Optional.empty();
        }
        Matcher constructorMatcher = Pattern.compile("new\\s+([A-Za-z_$][\\w.$]*)\\b").matcher(args);
        if (constructorMatcher.find()) {
            return Optional.of(constructorMatcher.group(1));
        }
        return Optional.empty();
    }

    static Optional<ObservedCdiEvent> detectCdiObservedEvent(ExtractedEntityFact methodEntity, String snippet) {
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        if (parameters == null || parameters.isBlank() || "()".equals(parameters.strip())) {
            parameters = snippet;
        }
        if (parameters == null || parameters.isBlank()) {
            return Optional.empty();
        }
        String normalized = parameters.replace('\n', ' ').replace('\r', ' ');
        Matcher matcher = Pattern.compile("((?:@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?\\s*)*)@(?:[A-Za-z_][\\w.]*\\.)?(ObservesAsync|Observes)\\b(?:\\s*\\([^)]*\\))?\\s+([A-Za-z_$][\\w.$<>]*)").matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        boolean async = "ObservesAsync".equals(matcher.group(2));
        String eventType = matcher.group(3) == null ? "" : matcher.group(3).trim();
        String annotationPrefix = matcher.group(1) == null ? "" : matcher.group(1);
        java.util.LinkedHashSet<String> qualifiers = new java.util.LinkedHashSet<>(SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(annotationPrefix));
        qualifiers.removeIf(value -> value == null || value.endsWith("Observes") || value.endsWith("ObservesAsync"));
        return eventType.isBlank() ? Optional.empty() : Optional.of(new ObservedCdiEvent(eventType, async, List.copyOf(qualifiers)));
    }

    static List<String> extractExtendedTypes(SyntaxNode typeNode) {
        return JavaRelationshipEvidenceEmitter.extractExtendedTypes(typeNode);
    }

    static boolean isConstructor(ExtractedEntityFact methodEntity) {
        if (methodEntity == null) {
            return false;
        }
        Object ownerQualifiedName = methodEntity.metadata().get("ownerQualifiedName");
        if (ownerQualifiedName == null || String.valueOf(ownerQualifiedName).isBlank()) {
            return false;
        }
        String ownerSimpleName = simpleName(String.valueOf(ownerQualifiedName));
        return ownerSimpleName != null && ownerSimpleName.equals(methodEntity.name());
    }

    static String simpleName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        int idx = qualifiedName.lastIndexOf('.');
        return idx >= 0 ? qualifiedName.substring(idx + 1) : qualifiedName;
    }

    static boolean containsJpaPropertyAnnotation(String snippet) {
        return hasAnyJpaAnnotation(snippet, "Id", "EmbeddedId", "Version", "Embedded", "Column", "OneToOne", "OneToMany", "ManyToOne", "ManyToMany", "JoinColumn", "JoinTable");
    }

    static boolean hasAnyJpaAnnotation(String snippet, String... simpleNames) {
        if (snippet == null || snippet.isBlank()) {
            return false;
        }
        for (String simpleName : simpleNames) {
            if (containsAnnotationSnippet(snippet, simpleName)) {
                return true;
            }
        }
        return false;
    }

    static String deriveJavaPropertyName(String methodName, String parameterSnippet) {
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        String params = parameterSnippet == null ? "" : parameterSnippet.strip();
        if (!(params.isBlank() || "()".equals(params))) {
            return null;
        }
        if (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
            return decapitalizeJavaProperty(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(methodName.charAt(2))) {
            return decapitalizeJavaProperty(methodName.substring(2));
        }
        return null;
    }

    private static String decapitalizeJavaProperty(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    static boolean isJpaPersistentType(String snippet, ExtractedEntityFact entity) {
        if (hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Entity")
            || hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Embeddable")
            || hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "MappedSuperclass")) {
            return true;
        }
        String lower = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        return lower.contains("@entity") || lower.contains("@embeddable") || lower.contains("@mappedsuperclass");
    }

    static Optional<String> detectJpaTypeKind(String snippet, ExtractedEntityFact entity) {
        List<String> annotations = metadataStringList(entity == null ? null : entity.metadata().get("annotations"));
        if (hasAnnotation(annotations, "Embeddable") || containsAnnotationSnippet(snippet, "Embeddable")) {
            return Optional.of("embeddable");
        }
        if (hasAnnotation(annotations, "MappedSuperclass") || containsAnnotationSnippet(snippet, "MappedSuperclass")) {
            return Optional.of("mapped-superclass");
        }
        if (hasAnnotation(annotations, "Entity") || containsAnnotationSnippet(snippet, "Entity")) {
            return Optional.of("entity");
        }
        return Optional.empty();
    }

    static boolean hasAnnotation(List<String> annotations, String simpleName) {
        if (annotations == null || simpleName == null || simpleName.isBlank()) {
            return false;
        }
        String expected = simpleName.toLowerCase(Locale.ROOT);
        return annotations.stream().map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT)).anyMatch(value -> value.endsWith(expected));
    }

    static boolean containsAnnotationSnippet(String snippet, String simpleName) {
        if (snippet == null || simpleName == null || simpleName.isBlank()) {
            return false;
        }
        return snippet.toLowerCase(Locale.ROOT).contains("@" + simpleName.toLowerCase(Locale.ROOT));
    }

    static Optional<String> extractJpaTableName(String snippet) {
        return extractAnnotationStringAttribute(snippet, "Table", "name");
    }

    static Optional<String> extractJpaColumnName(String snippet) {
        return extractAnnotationStringAttribute(snippet, "Column", "name");
    }

    static Optional<String> extractJpaJoinColumn(String snippet) {
        return extractAnnotationStringAttribute(snippet, "JoinColumn", "name");
    }

    static Optional<String> extractJpaJoinTable(String snippet) {
        return extractAnnotationStringAttribute(snippet, "JoinTable", "name");
    }

    static Optional<String> extractJpaMappedBy(String snippet) {
        return extractAnnotationStringAttribute(snippet, "OneToMany", "mappedBy")
            .or(() -> extractAnnotationStringAttribute(snippet, "OneToOne", "mappedBy"))
            .or(() -> extractAnnotationStringAttribute(snippet, "ManyToMany", "mappedBy"));
    }

    static Optional<String> extractJpaInheritanceStrategy(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        Matcher strategy = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Inheritance\\s*\\([^)]*strategy\\s*=\\s*(?:[A-Za-z_][\\w.]*\\.)?([A-Z_]+)", Pattern.DOTALL).matcher(snippet);
        if (strategy.find()) {
            return Optional.of(strategy.group(1));
        }
        return Optional.empty();
    }

    static Optional<String> detectJpaAssociation(String snippet) {
        return detectJpaAssociation(List.of(), snippet);
    }

    static Optional<String> detectJpaAssociation(List<String> annotations, String snippet) {
        if (hasAnnotation(annotations, "OneToOne") || containsAnnotationSnippet(snippet, "OneToOne")) return Optional.of("one-to-one");
        if (hasAnnotation(annotations, "OneToMany") || containsAnnotationSnippet(snippet, "OneToMany")) return Optional.of("one-to-many");
        if (hasAnnotation(annotations, "ManyToOne") || containsAnnotationSnippet(snippet, "ManyToOne")) return Optional.of("many-to-one");
        if (hasAnnotation(annotations, "ManyToMany") || containsAnnotationSnippet(snippet, "ManyToMany")) return Optional.of("many-to-many");
        return Optional.empty();
    }

    static Optional<String> inferJavaMethodReturnTypeFromSnippet(String snippet, String methodName) {
        if (snippet == null || snippet.isBlank() || methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile("(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?([A-Za-z_$][\\w$<>., ?]+?)\\s+" + Pattern.quote(methodName) + "\\s*\\(");
        Matcher matcher = pattern.matcher(snippet.replace("\n", " "));
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1)).map(String::trim).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }

    private static Optional<String> extractAnnotationStringAttribute(String snippet, String annotationSimpleName, String attributeName) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        String annotationPattern = "@(?:[A-Za-z_][\\w.]*\\.)?" + Pattern.quote(annotationSimpleName) + "\\s*\\((.*?)\\)";
        Matcher matcher = Pattern.compile(annotationPattern, Pattern.DOTALL).matcher(snippet);
        while (matcher.find()) {
            String body = matcher.group(1);
            Matcher named = Pattern.compile(Pattern.quote(attributeName) + "\\s*=\\s*\"([^\"]*)\"").matcher(body);
            if (named.find()) {
                return Optional.of(named.group(1));
            }
            if ("name".equals(attributeName)) {
                Matcher positional = Pattern.compile("^\\s*\"([^\"]*)\"").matcher(body.strip());
                if (positional.find()) {
                    return Optional.of(positional.group(1));
                }
            }
        }
        return Optional.empty();
    }

    static boolean isJavaTypeDeclaration(SyntaxNode node) {
        return node != null && Set.of(
            "class_declaration", "interface_declaration", "enum_declaration", "record_declaration"
        ).contains(node.type());
    }

    static boolean isJaxRsResource(ExtractedEntityFact entity) {
        return metadataStringList(entity.metadata().get("annotations")).stream()
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

    @SuppressWarnings("unchecked")
    private static List<String> metadataStringList(Object value) {
        return JavaDeclaredTypeSupport.metadataStringList(value);
    }

    static int lineOf(SourceReference ref, SyntaxNode fallbackNode) {
        return JavaSourceReferenceSupport.lineOf(ref, fallbackNode);
    }

    static Optional<String> importQualifiedName(String snippet) {
        return SyntaxTreeExtractionSupport.extractQualifiedName(
            snippet == null ? null : snippet.replaceFirst("^\\s*import\\s+", "").replaceFirst(";\\s*$", "")
        );
    }

    static String derivePackageFromPath(String relativePath) {
        int marker = relativePath.indexOf("/java/");
        if (marker >= 0) {
            String candidate = relativePath.substring(marker + 6);
            int slash = candidate.lastIndexOf('/');
            if (slash > 0) {
                return candidate.substring(0, slash).replace('/', '.');
            }
        }
        int slash = relativePath.lastIndexOf('/');
        return slash > 0 ? relativePath.substring(0, slash).replace('/', '.') : "default";
    }

    private static boolean isJavaPrimitiveOrKeyword(String candidate) {
        return Set.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void", "var", "this", "super"
        ).contains(candidate);
    }
}
