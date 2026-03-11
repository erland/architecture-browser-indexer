package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

final class InterpretationContext {
    private final StructuralExtractionResult extractionResult;
    private final Map<String, ExtractedEntityFact> entitiesById;

    InterpretationContext(StructuralExtractionResult extractionResult) {
        this.extractionResult = extractionResult;
        this.entitiesById = extractionResult.entities().stream().collect(Collectors.toMap(ExtractedEntityFact::id, entity -> entity, (left, right) -> left));
    }

    StructuralExtractionResult extractionResult() {
        return extractionResult;
    }

    List<ExtractedEntityFact> entities() {
        return extractionResult.entities();
    }

    List<ExtractedRelationshipFact> relationships() {
        return extractionResult.relationships();
    }

    List<ExtractedEntityFact> entitiesByLanguage(String language) {
        return extractionResult.entities().stream()
            .filter(entity -> language.equalsIgnoreCase(String.valueOf(entity.metadata().getOrDefault("language", ""))))
            .toList();
    }

    List<ExtractedEntityFact> entitiesByLanguageAndKind(String language, EntityKind kind) {
        return entitiesByLanguage(language).stream()
            .filter(entity -> entity.kind() == kind)
            .toList();
    }

    List<ExtractedEntityFact> entitiesInFile(String relativePath) {
        return extractionResult.entities().stream()
            .filter(entity -> entity.sourceRefs().stream().anyMatch(ref -> relativePath.equals(ref.path())))
            .toList();
    }

    Optional<ExtractedEntityFact> nearestClassInFileAboveLine(String relativePath, Integer line) {
        if (line == null) {
            return Optional.empty();
        }
        return extractionResult.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE)
            .filter(entity -> entity.sourceRefs().stream().anyMatch(ref -> relativePath.equals(ref.path()) && ref.startLine() != null && ref.startLine() <= line))
            .sorted(Comparator.comparingInt((ExtractedEntityFact entity) -> entity.sourceRefs().get(0).startLine()).reversed())
            .findFirst();
    }

    Optional<ExtractedEntityFact> fileModule(String relativePath) {
        return extractionResult.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.MODULE)
            .filter(entity -> relativePath.equals(entity.name()) || relativePath.equals(entity.displayName()) || relativePath.equals(entity.metadata().get("relativePath")))
            .findFirst();
    }

    Optional<ExtractedEntityFact> entityById(String id) {
        return Optional.ofNullable(entitiesById.get(id));
    }

    static String language(ExtractedEntityFact entity) {
        return String.valueOf(entity.metadata().getOrDefault("language", "")).toLowerCase(Locale.ROOT);
    }

    static List<String> listMetadata(ExtractedEntityFact entity, String key) {
        Object value = entity.metadata().get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    static String stringMetadata(ExtractedEntityFact entity, String key) {
        Object value = entity.metadata().get(key);
        return value == null ? null : String.valueOf(value);
    }

    static SourceReference primaryRef(ExtractedEntityFact entity) {
        return entity.sourceRefs().isEmpty() ? null : entity.sourceRefs().get(0);
    }

    static Integer line(ExtractedEntityFact entity) {
        SourceReference ref = primaryRef(entity);
        return ref == null ? null : ref.startLine();
    }

    static String path(ExtractedEntityFact entity) {
        SourceReference ref = primaryRef(entity);
        return ref == null ? null : ref.path();
    }
}
