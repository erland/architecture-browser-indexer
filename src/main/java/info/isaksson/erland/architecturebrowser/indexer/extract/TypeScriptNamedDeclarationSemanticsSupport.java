package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.naming.DisplayNamePolicy;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TypeScriptNamedDeclarationSemanticsSupport {
    private TypeScriptNamedDeclarationSemanticsSupport() {
    }

    static ExtractedEntityFact addNamedEntityFromNode(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String parentEntityId,
        String relativePath,
        SyntaxNode node,
        EntityKind kind,
        String matchedKind,
        String declarationKind,
        ExtractionMode extractionMode
    ) {
        String name = SyntaxTreeExtractionSupport.declarationName(node);
        if (name == null || name.isBlank()) {
            return null;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(node);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, node.textSnippet(), Map.of("language", "typescript", "kind", matchedKind));
        List<String> decorators = SyntaxTreeExtractionSupport.descendantsByType(node, Set.of("decorator")).stream()
            .flatMap(candidate -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(candidate.textSnippet()).stream())
            .distinct()
            .toList();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("language", "typescript");
        metadata.put("declarationKind", declarationKind);
        metadata.put("decorators", decorators);
        metadata.putAll(AngularDecoratorMetadataExtractor.extract(node));
        metadata.put("parseStatus", parseResult.status().name());
        metadata.put("extractionMode", extractionMode.name());
        if (kind == EntityKind.CLASS || kind == EntityKind.INTERFACE) {
            metadata.put("qualifiedName", name);
        }
        ExtractedEntityFact entity = new ExtractedEntityFact(
            IdUtils.scopedEntityId("typescript", relativePath, name, line),
            kind,
            EntityOrigin.OBSERVED,
            name,
            DisplayNamePolicy.entityDisplayName(kind, name, "typescript"),
            IdUtils.scopeId("file", relativePath),
            List.of(ref),
            Map.copyOf(metadata)
        );
        accumulator.addEntity(entity);
        accumulator.addRelationship(ExtractionSupport.containsRelationship(parentEntityId, entity.id(), ref));
        return entity;
    }
}
