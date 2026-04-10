package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Map;

final class TypeScriptTypeRelationshipSupport {
    private TypeScriptTypeRelationshipSupport() {
    }

    static void addTypeRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        if (typeNode == null || typeEntity == null) {
            return;
        }
        int line = SyntaxTreeExtractionSupport.oneBasedLine(typeNode);
        SourceReference ref = ExtractionSupport.sourceRef(relativePath, line, typeNode.textSnippet(), Map.of("language", "typescript", "kind", typeNode.type()));
        for (String parentType : TypeScriptDeclaredTypeParsingSupport.extractExtendedTypes(typeNode)) {
            EntityKind targetKind = typeEntity.kind() == EntityKind.INTERFACE ? EntityKind.INTERFACE : EntityKind.CLASS;
            TypeScriptResolvedTypeDependencySupport.addResolvedTypeRelationship(accumulator, typeEntity, parentType, targetKind, RelationshipKind.EXTENDS, "extends", relativePath, line, ref, declaredTypes);
        }
        for (String implementedType : TypeScriptDeclaredTypeParsingSupport.extractImplementedTypes(typeNode)) {
            TypeScriptResolvedTypeDependencySupport.addResolvedTypeRelationship(accumulator, typeEntity, implementedType, EntityKind.INTERFACE, RelationshipKind.IMPLEMENTS, "implements", relativePath, line, ref, declaredTypes);
        }
    }

    static void addPropertyTypeDependencies(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        ExtractedEntityFact propertyEntity,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String declaredType = String.valueOf(propertyEntity.metadata().getOrDefault("declaredType", ""));
        if (declaredType.isBlank()) {
            return;
        }
        TypeScriptResolvedTypeDependencySupport.addDeclaredTypeDependencies(
            accumulator,
            ownerEntity.id(),
            List.of(declaredType),
            relativePath,
            line,
            ref,
            declaredTypes,
            TypeScriptResolvedTypeDependencySupport.dependencyMetadata("field", "composition")
        );
    }

    @SuppressWarnings("unchecked")
    static void addMethodTypeDependencies(
        ExtractionAccumulator accumulator,
        ExtractedEntityFact ownerEntity,
        ExtractedEntityFact methodEntity,
        String relativePath,
        int line,
        SourceReference ref,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        String returnType = String.valueOf(methodEntity.metadata().getOrDefault("returnType", ""));
        if (!returnType.isBlank()) {
            TypeScriptResolvedTypeDependencySupport.addDeclaredTypeDependencies(
                accumulator,
                ownerEntity.id(),
                List.of(returnType),
                relativePath,
                line,
                ref,
                declaredTypes,
                TypeScriptResolvedTypeDependencySupport.dependencyMetadata("returnType", "api")
            );
        }
        List<String> parameterTypes = (List<String>) methodEntity.metadata().getOrDefault("parameterTypes", List.of());
        if (!parameterTypes.isEmpty()) {
            TypeScriptResolvedTypeDependencySupport.addDeclaredTypeDependencies(
                accumulator,
                ownerEntity.id(),
                parameterTypes,
                relativePath,
                line,
                ref,
                declaredTypes,
                TypeScriptResolvedTypeDependencySupport.dependencyMetadata("constructor".equals(methodEntity.name()) ? "constructorParameter" : "parameterType", "api")
            );
        }
    }
}
