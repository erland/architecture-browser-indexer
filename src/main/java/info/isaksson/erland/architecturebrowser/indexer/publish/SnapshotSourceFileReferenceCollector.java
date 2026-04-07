package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects the unique repository-relative file paths referenced by exported source refs.
 *
 * <p>This step intentionally does not read file contents yet. It prepares a stable, de-duplicated
 * path set that later export steps can resolve back against the indexed source tree.</p>
 */
public final class SnapshotSourceFileReferenceCollector {

    public List<String> collectReferencedRelativePaths(ArchitectureIndexDocument document) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        collectScopePaths(document.scopes(), paths);
        collectEntityPaths(document.entities(), paths);
        collectRelationshipPaths(document.relationships(), paths);
        collectDiagnosticPaths(document.diagnostics(), paths);
        return List.copyOf(paths);
    }

    private static void collectScopePaths(List<LogicalScope> scopes, Set<String> paths) {
        for (LogicalScope scope : scopes) {
            collectSourceReferences(scope.sourceRefs(), paths);
        }
    }

    private static void collectEntityPaths(List<ArchitectureEntity> entities, Set<String> paths) {
        for (ArchitectureEntity entity : entities) {
            collectSourceReferences(entity.sourceRefs(), paths);
        }
    }

    private static void collectRelationshipPaths(List<ArchitectureRelationship> relationships, Set<String> paths) {
        for (ArchitectureRelationship relationship : relationships) {
            collectSourceReferences(relationship.sourceRefs(), paths);
        }
    }

    private static void collectDiagnosticPaths(List<Diagnostic> diagnostics, Set<String> paths) {
        for (Diagnostic diagnostic : diagnostics) {
            collectSourceReferences(diagnostic.sourceRefs(), paths);
        }
    }

    private static void collectSourceReferences(List<SourceReference> sourceReferences, Set<String> paths) {
        for (SourceReference sourceReference : sourceReferences) {
            String normalizedPath = normalizeRelativePath(sourceReference.path());
            if (normalizedPath != null) {
                paths.add(normalizedPath);
            }
        }
    }

    static String normalizeRelativePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.replace('\\', '/').trim();
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains("..")) {
            return null;
        }
        return normalized;
    }
}
