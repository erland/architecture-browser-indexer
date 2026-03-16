package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaJpaDetailSupport {
    record JpaTypeDetails(LinkedHashMap<String, Object> metadata) {}
    record JpaFieldDetails(LinkedHashMap<String, Object> metadata, boolean changed, boolean embedded, String associationKind, String mappedBy, String joinColumn, String joinTable, String declaredType) {}
    record JpaMethodDetails(LinkedHashMap<String, Object> metadata, boolean changed, String propertyName, boolean embedded, String associationKind, String mappedBy, String joinColumn, String joinTable, String declaredType) {}

    JpaTypeDetails analyzeType(ExtractedEntityFact typeEntity, String snippet) {
        if (!isJpaPersistentType(snippet, typeEntity)) return null;
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(typeEntity.metadata());
        metadata.put("framework", "jpa");
        String jpaKind = detectJpaTypeKind(snippet, typeEntity).orElse("entity");
        metadata.put("jpaKind", jpaKind);
        metadata.put("jpaEntity", "entity".equals(jpaKind));
        metadata.put("jpaEmbeddable", "embeddable".equals(jpaKind));
        metadata.put("jpaMappedSuperclass", "mapped-superclass".equals(jpaKind));
        extractAnnotationStringAttribute(snippet, "Table", "name").ifPresent(v -> metadata.put("tableName", v));
        return new JpaTypeDetails(metadata);
    }

    JpaFieldDetails analyzeField(ExtractedEntityFact fieldEntity, String snippet) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(fieldEntity.metadata());
        metadata.put("framework", "jpa");
        List<String> annotations = metadataStringList(fieldEntity.metadata().get("annotations"));
        boolean changed = false;
        if (hasAnnotation(annotations, "Embedded") || hasAnnotation(annotations, "EmbeddedId")) {
            metadata.put("jpaEmbedded", true);
            changed = true;
        }
        Optional<String> assoc = detectJpaAssociation(annotations, snippet);
        if (assoc.isPresent()) {
            metadata.put("jpaAssociation", assoc.get());
            extractAnnotationStringAttribute(snippet, "OneToMany", "mappedBy").or(() -> extractAnnotationStringAttribute(snippet, "OneToOne", "mappedBy")).or(() -> extractAnnotationStringAttribute(snippet, "ManyToMany", "mappedBy")).ifPresent(v -> metadata.put("mappedBy", v));
            extractAnnotationStringAttribute(snippet, "JoinColumn", "name").ifPresent(v -> metadata.put("joinColumn", v));
            extractAnnotationStringAttribute(snippet, "JoinTable", "name").ifPresent(v -> metadata.put("joinTable", v));
            changed = true;
        }
        return new JpaFieldDetails(metadata, changed, Boolean.TRUE.equals(metadata.get("jpaEmbedded")), assoc.orElse(null), str(metadata.get("mappedBy")), str(metadata.get("joinColumn")), str(metadata.get("joinTable")), String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", "")));
    }

    JpaMethodDetails analyzeMethod(ExtractedEntityFact methodEntity, String snippet, List<String> annotations) {
        String propertyName = deriveJavaPropertyName(methodEntity.name(), String.valueOf(methodEntity.metadata().getOrDefault("parameters", "")));
        if (propertyName == null || (!annotations.stream().anyMatch(a -> a != null && !a.isBlank()) && !containsJpaPropertyAnnotation(snippet))) return null;
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(methodEntity.metadata());
        metadata.put("framework", "jpa");
        metadata.put("jpaPropertyAccess", true);
        metadata.put("jpaPropertyName", propertyName);
        Optional<String> assoc = detectJpaAssociation(annotations, snippet);
        if (assoc.isPresent()) metadata.put("jpaAssociation", assoc.get());
        return new JpaMethodDetails(metadata, true, propertyName, Boolean.TRUE.equals(metadata.get("jpaEmbedded")), assoc.orElse(null), str(metadata.get("mappedBy")), str(metadata.get("joinColumn")), str(metadata.get("joinTable")), String.valueOf(methodEntity.metadata().getOrDefault("returnType", "")));
    }

    static List<String> metadataStringList(Object value) { return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of(); }
    static boolean isJpaPersistentType(String snippet, ExtractedEntityFact entity) { return hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Entity") || hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Embeddable") || hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "MappedSuperclass") || (snippet != null && snippet.toLowerCase(Locale.ROOT).contains("@entity")); }
    static Optional<String> detectJpaTypeKind(String snippet, ExtractedEntityFact entity) { if (hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "Embeddable")) return Optional.of("embeddable"); if (hasAnnotation(metadataStringList(entity == null ? null : entity.metadata().get("annotations")), "MappedSuperclass")) return Optional.of("mapped-superclass"); if (isJpaPersistentType(snippet, entity)) return Optional.of("entity"); return Optional.empty(); }
    static boolean containsJpaPropertyAnnotation(String snippet) { return snippet != null && snippet.contains("@"); }
    static Optional<String> detectJpaAssociation(List<String> annotations, String snippet) { if (hasAnnotation(annotations, "OneToOne") || containsAnnotationSnippet(snippet, "OneToOne")) return Optional.of("one-to-one"); if (hasAnnotation(annotations, "OneToMany") || containsAnnotationSnippet(snippet, "OneToMany")) return Optional.of("one-to-many"); if (hasAnnotation(annotations, "ManyToOne") || containsAnnotationSnippet(snippet, "ManyToOne")) return Optional.of("many-to-one"); if (hasAnnotation(annotations, "ManyToMany") || containsAnnotationSnippet(snippet, "ManyToMany")) return Optional.of("many-to-many"); return Optional.empty(); }
    static String deriveJavaPropertyName(String methodName, String params) { if (methodName == null) return null; if (!(params == null || params.isBlank() || "()".equals(params.strip()))) return null; if (methodName.startsWith("get") && methodName.length() > 3) return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4); if (methodName.startsWith("is") && methodName.length() > 2) return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3); return null; }
    static boolean hasAnnotation(List<String> annotations, String simpleName) { String expected = simpleName.toLowerCase(Locale.ROOT); return annotations.stream().map(v -> v == null ? "" : v.toLowerCase(Locale.ROOT)).anyMatch(v -> v.endsWith(expected)); }
    static boolean containsAnnotationSnippet(String snippet, String simpleName) { return snippet != null && snippet.toLowerCase(Locale.ROOT).contains("@" + simpleName.toLowerCase(Locale.ROOT)); }
    static Optional<String> extractAnnotationStringAttribute(String snippet, String annotationSimpleName, String attributeName) { if (snippet == null || snippet.isBlank()) return Optional.empty(); Matcher m = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?" + Pattern.quote(annotationSimpleName) + "\\s*\\((.*?)\\)", Pattern.DOTALL).matcher(snippet); while (m.find()) { String body = m.group(1); Matcher n = Pattern.compile(Pattern.quote(attributeName) + "\\s*=\\s*\"([^\"]*)\"").matcher(body); if (n.find()) return Optional.of(n.group(1)); } return Optional.empty(); }
    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
}
