package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaJpaDomainSemanticsSupport {
    private JavaJpaDomainSemanticsSupport() {}

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

    static boolean isJpaPersistentType(String snippet, ExtractedEntityFact entity) {
        if (hasAnnotation(JavaDeclaredTypeSupport.metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Entity")
            || hasAnnotation(JavaDeclaredTypeSupport.metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Embeddable")
            || hasAnnotation(JavaDeclaredTypeSupport.metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "MappedSuperclass")) {
            return true;
        }
        String lower = snippet == null ? "" : snippet.toLowerCase(Locale.ROOT);
        return lower.contains("@entity") || lower.contains("@embeddable") || lower.contains("@mappedsuperclass");
    }

    static Optional<String> detectJpaTypeKind(String snippet, ExtractedEntityFact entity) {
        List<String> annotations = JavaDeclaredTypeSupport.metadataStringList(entity == null ? null : entity.metadata().get("annotations"));
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

    static Optional<String> extractJpaTableName(String snippet) { return extractAnnotationStringAttribute(snippet, "Table", "name"); }
    static Optional<String> extractJpaColumnName(String snippet) { return extractAnnotationStringAttribute(snippet, "Column", "name"); }
    static Optional<String> extractJpaJoinColumn(String snippet) { return extractAnnotationStringAttribute(snippet, "JoinColumn", "name"); }
    static Optional<String> extractJpaJoinTable(String snippet) { return extractAnnotationStringAttribute(snippet, "JoinTable", "name"); }
    static Optional<String> extractJpaMappedBy(String snippet) {
        return extractAnnotationStringAttribute(snippet, "OneToMany", "mappedBy")
            .or(() -> extractAnnotationStringAttribute(snippet, "OneToOne", "mappedBy"))
            .or(() -> extractAnnotationStringAttribute(snippet, "ManyToMany", "mappedBy"));
    }


    static Optional<Boolean> extractJpaAssociationOptional(String snippet) {
        return extractAnnotationBooleanAttribute(snippet, "ManyToOne", "optional")
            .or(() -> extractAnnotationBooleanAttribute(snippet, "OneToOne", "optional"))
            .or(() -> extractAnnotationBooleanAttribute(snippet, "ManyToMany", "optional"))
            .or(() -> extractAnnotationBooleanAttribute(snippet, "OneToMany", "optional"));
    }

    static Optional<Boolean> extractJpaJoinColumnNullable(String snippet) {
        return extractAnnotationBooleanAttribute(snippet, "JoinColumn", "nullable");
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

    static Optional<String> detectJpaAssociation(String snippet) { return detectJpaAssociation(List.of(), snippet); }
    static Optional<String> detectJpaAssociation(List<String> annotations, String snippet) {
        if (hasAnnotation(annotations, "OneToOne") || containsAnnotationSnippet(snippet, "OneToOne")) return Optional.of("one-to-one");
        if (hasAnnotation(annotations, "OneToMany") || containsAnnotationSnippet(snippet, "OneToMany")) return Optional.of("one-to-many");
        if (hasAnnotation(annotations, "ManyToOne") || containsAnnotationSnippet(snippet, "ManyToOne")) return Optional.of("many-to-one");
        if (hasAnnotation(annotations, "ManyToMany") || containsAnnotationSnippet(snippet, "ManyToMany")) return Optional.of("many-to-many");
        return Optional.empty();
    }

    private static Optional<Boolean> extractAnnotationBooleanAttribute(String snippet, String annotationSimpleName, String attributeName) {
        if (snippet == null || snippet.isBlank()) {
            return Optional.empty();
        }
        String annotationPattern = "@(?:[A-Za-z_][\\w.]*\\.)?" + Pattern.quote(annotationSimpleName) + "\\s*\\((.*?)\\)";
        Matcher matcher = Pattern.compile(annotationPattern, Pattern.DOTALL).matcher(snippet);
        while (matcher.find()) {
            String body = matcher.group(1);
            Matcher named = Pattern.compile(Pattern.quote(attributeName) + "\\s*=\\s*(true|false)", Pattern.CASE_INSENSITIVE).matcher(body);
            if (named.find()) {
                return Optional.of(Boolean.parseBoolean(named.group(1).toLowerCase(Locale.ROOT)));
            }
        }
        return Optional.empty();
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
}
