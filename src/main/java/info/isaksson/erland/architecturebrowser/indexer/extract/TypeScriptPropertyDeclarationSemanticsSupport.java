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

final class TypeScriptPropertyDeclarationSemanticsSupport {
    private TypeScriptPropertyDeclarationSemanticsSupport() {
    }

    static ExtractedEntityFact toTypeScriptPropertyEntity(
        SourceParseResult parseResult,
        String relativePath,
        ExtractionMode extractionMode,
        String fileScopeId,
        SyntaxNode propertyNode,
        String ownerQualifiedName,
        String ownerDeclarationKind
    ) {
        String propertyName = SyntaxTreeExtractionSupport.declarationName(propertyNode);
        if (propertyName == null || propertyName.isBlank()) {
            return null;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(propertyNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, propertyNode.textSnippet(), Map.of("language", "typescript", "kind", propertyNode.type()));
        List<String> decorators = SyntaxTreeExtractionSupport.descendantsByType(propertyNode, Set.of("decorator")).stream()
            .flatMap(node -> SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(node.textSnippet()).stream())
            .distinct()
            .toList();
        String declaredType = SyntaxTreeExtractionSupport.typeScriptDeclaredType(propertyNode);
        List<String> modifiers = SyntaxTreeExtractionSupport.typeScriptModifiers(propertyNode);
        boolean optional = SyntaxTreeExtractionSupport.typeScriptOptional(propertyNode);
        boolean readonly = SyntaxTreeExtractionSupport.typeScriptReadonly(propertyNode);
        String accessibility = SyntaxTreeExtractionSupport.typeScriptAccessibility(propertyNode);
        String canonicalName = ownerQualifiedName == null || ownerQualifiedName.isBlank() ? propertyName : ownerQualifiedName + "#" + propertyName;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("language", "typescript");
        metadata.put("declaredType", declaredType);
        metadata.put("optional", optional);
        metadata.put("readonly", readonly);
        metadata.put("accessibility", accessibility);
        metadata.put("modifiers", modifiers);
        metadata.put("decorators", decorators);
        metadata.put("ownerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
        metadata.put("ownerDeclarationKind", ownerDeclarationKind == null ? "" : ownerDeclarationKind);
        metadata.put("parseStatus", parseResult.status().name());
        metadata.put("extractionMode", extractionMode.name());
        return new ExtractedEntityFact(
            IdUtils.scopedEntityId("typescript", relativePath, canonicalName, line),
            EntityKind.FIELD,
            EntityOrigin.OBSERVED,
            propertyName,
            DisplayNamePolicy.entityDisplayName(EntityKind.FIELD, canonicalName, "typescript"),
            fileScopeId,
            List.of(ref),
            Map.copyOf(metadata)
        );
    }
}
