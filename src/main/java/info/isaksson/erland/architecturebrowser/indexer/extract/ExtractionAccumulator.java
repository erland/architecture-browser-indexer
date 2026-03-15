package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExtractionAccumulator {
    private final Map<String, LogicalScope> scopesById = new LinkedHashMap<>();
    private final Map<String, ExtractedEntityFact> entitiesById = new LinkedHashMap<>();
    private final Map<String, ExtractedRelationshipFact> relationshipsById = new LinkedHashMap<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final Map<String, Integer> extractedByLanguage = new LinkedHashMap<>();
    private final Map<String, Integer> extractedByMode = new LinkedHashMap<>();
    private int filesVisited;
    private int filesExtracted;

    public void incrementFilesVisited() {
        filesVisited++;
    }

    public void incrementFilesExtracted(String languageKey, ExtractionMode extractionMode) {
        filesExtracted++;
        extractedByLanguage.merge(languageKey, 1, Integer::sum);
        if (extractionMode != null) {
            extractedByMode.merge(extractionMode.name(), 1, Integer::sum);
        }
    }

    public void addScope(LogicalScope scope) {
        if (scope != null) {
            scopesById.putIfAbsent(scope.id(), scope);
        }
    }

    public void addEntity(ExtractedEntityFact entity) {
        if (entity == null) {
            return;
        }
        ExtractedEntityFact existing = entitiesById.get(entity.id());
        if (existing == null) {
            entitiesById.put(entity.id(), entity);
            return;
        }
        entitiesById.put(entity.id(), mergeEntity(existing, entity));
    }

    private static ExtractedEntityFact mergeEntity(ExtractedEntityFact existing, ExtractedEntityFact candidate) {
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>(existing.metadata());
        metadata.putAll(candidate.metadata());

        java.util.ArrayList<info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference> sourceRefs = new java.util.ArrayList<>(existing.sourceRefs());
        for (info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference sourceRef : candidate.sourceRefs()) {
            if (!sourceRefs.contains(sourceRef)) {
                sourceRefs.add(sourceRef);
            }
        }

        return new ExtractedEntityFact(
            existing.id(),
            existing.kind() != null ? existing.kind() : candidate.kind(),
            preferOrigin(existing.origin(), candidate.origin()),
            preferredString(existing.name(), candidate.name()),
            preferredString(existing.displayName(), candidate.displayName()),
            preferredString(existing.scopeId(), candidate.scopeId()),
            sourceRefs,
            metadata
        );
    }

    private static info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin preferOrigin(
        info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin existing,
        info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin candidate
    ) {
        if (existing == info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED
            || candidate == info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED) {
            return info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED;
        }
        return candidate != null ? candidate : existing;
    }

    private static String preferredString(String existing, String candidate) {
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return candidate;
    }

    public void addRelationship(ExtractedRelationshipFact relationship) {
        if (relationship != null) {
            relationshipsById.putIfAbsent(relationship.id(), relationship);
        }
    }

    public void addDiagnostic(Diagnostic diagnostic) {
        if (diagnostic != null) {
            diagnostics.add(diagnostic);
        }
    }

    public List<LogicalScope> scopes() { return List.copyOf(scopesById.values()); }
    public List<ExtractedEntityFact> entities() { return List.copyOf(entitiesById.values()); }
    public List<ExtractedRelationshipFact> relationships() { return List.copyOf(relationshipsById.values()); }
    public List<Diagnostic> diagnostics() { return List.copyOf(diagnostics); }
    public Map<String, Integer> extractedByLanguage() { return Map.copyOf(extractedByLanguage); }
    public Map<String, Integer> extractedByMode() { return Map.copyOf(extractedByMode); }
    public int filesVisited() { return filesVisited; }
    public int filesExtracted() { return filesExtracted; }
}
