package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Map;

final class TypeScriptOwnedMemberExtractionSupport {
    private TypeScriptOwnedMemberExtractionSupport() {
    }

    static void addOwnedMembers(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        String relativePath,
        SyntaxNode ownerNode,
        ExtractionMode extractionMode,
        String ownerDeclarationKind,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String ownerQualifiedName = String.valueOf(ownerEntity.metadata().getOrDefault("qualifiedName", ownerEntity.name()));
        for (SyntaxNode memberNode : ownerNode.children()) {
            if (SyntaxTreeExtractionSupport.isTypeScriptMethodLikeDeclaration(memberNode)) {
                ExtractedEntityFact methodEntity = TypeScriptMethodDeclarationSemanticsSupport.toTypeScriptMethodEntity(
                    parseResult,
                    relativePath,
                    extractionMode,
                    ownerEntity.scopeId(),
                    memberNode,
                    ownerQualifiedName,
                    ownerDeclarationKind
                );
                if (methodEntity != null) {
                    accumulator.addEntity(methodEntity);
                    SourceReference ref = methodEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(memberNode), memberNode.textSnippet(), Map.of("language", "typescript", "kind", memberNode.type()))
                        : methodEntity.sourceRefs().getFirst();
                    accumulator.addRelationship(ExtractionSupport.containsRelationship(ownerEntity.id(), methodEntity.id(), ref));
                    TypeScriptTypeRelationshipSupport.addMethodTypeDependencies(
                        accumulator,
                        ownerEntity,
                        methodEntity,
                        relativePath,
                        lineOf(ref, memberNode),
                        ref,
                        declaredTypes
                    );
                }
            } else if (SyntaxTreeExtractionSupport.isTypeScriptPropertyLikeDeclaration(memberNode)) {
                ExtractedEntityFact propertyEntity = TypeScriptPropertyDeclarationSemanticsSupport.toTypeScriptPropertyEntity(
                    parseResult,
                    relativePath,
                    extractionMode,
                    ownerEntity.scopeId(),
                    memberNode,
                    ownerQualifiedName,
                    ownerDeclarationKind
                );
                if (propertyEntity != null) {
                    accumulator.addEntity(propertyEntity);
                    SourceReference ref = propertyEntity.sourceRefs().isEmpty()
                        ? ExtractionSupport.sourceRef(relativePath, SyntaxTreeExtractionSupport.oneBasedLine(memberNode), memberNode.textSnippet(), Map.of("language", "typescript", "kind", memberNode.type()))
                        : propertyEntity.sourceRefs().getFirst();
                    accumulator.addRelationship(ExtractionSupport.containsRelationship(ownerEntity.id(), propertyEntity.id(), ref));
                    TypeScriptTypeRelationshipSupport.addPropertyTypeDependencies(
                        accumulator,
                        ownerEntity,
                        propertyEntity,
                        relativePath,
                        lineOf(ref, memberNode),
                        ref,
                        declaredTypes
                    );
                }
            }
        }
    }

    private static int lineOf(SourceReference ref, SyntaxNode fallbackNode) {
        return ref == null
            ? SyntaxTreeExtractionSupport.oneBasedLine(fallbackNode)
            : java.util.Objects.requireNonNullElse(ref.startLine(), SyntaxTreeExtractionSupport.oneBasedLine(fallbackNode));
    }
}
