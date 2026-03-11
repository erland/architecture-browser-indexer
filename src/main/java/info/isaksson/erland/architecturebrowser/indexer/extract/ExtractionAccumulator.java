package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
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
    private int filesVisited;
    private int filesExtracted;

    public void incrementFilesVisited() {
        filesVisited++;
    }

    public void incrementFilesExtracted(String languageKey) {
        filesExtracted++;
        extractedByLanguage.merge(languageKey, 1, Integer::sum);
    }

    public void addScope(LogicalScope scope) {
        if (scope != null) {
            scopesById.putIfAbsent(scope.id(), scope);
        }
    }

    public void addEntity(ExtractedEntityFact entity) {
        if (entity != null) {
            entitiesById.putIfAbsent(entity.id(), entity);
        }
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
    public int filesVisited() { return filesVisited; }
    public int filesExtracted() { return filesExtracted; }
}
