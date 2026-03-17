package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TypeScriptDeclarationExtractor {
    private TypeScriptDeclarationExtractor() {
    }

    static TypeScriptDeclarationResult extract(TypeScriptExtractionContext context) {
        Map<String, ExtractedEntityFact> declaredTypes = new LinkedHashMap<>();
        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discoveredDeclarations =
            TypeScriptDeclarationDiscoverySupport.discover(context.root());

        extractNamedTopLevelDeclarations(context, discoveredDeclarations, declaredTypes, namedEntities);
        extractOwnedMembersAndTypeRelationships(context, discoveredDeclarations, declaredTypes);
        extractFunctions(context, discoveredDeclarations, namedEntities);

        return new TypeScriptDeclarationResult(Map.copyOf(declaredTypes), Map.copyOf(namedEntities));
    }

    private static void extractNamedTopLevelDeclarations(
        TypeScriptExtractionContext context,
        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discoveredDeclarations,
        Map<String, ExtractedEntityFact> declaredTypes,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        for (TypeScriptDeclarationDiscoverySupport.DiscoveredTypeDeclaration discoveredType : discoveredDeclarations.namedTypeDeclarations()) {
            ExtractedEntityFact typeEntity = TypeScriptNamedDeclarationSemanticsSupport.addNamedEntityFromNode(
                context.parseResult(),
                context.accumulator(),
                context.fileEntity().id(),
                context.relativePath(),
                discoveredType.node(),
                discoveredType.entityKind(),
                discoveredType.matchedKind(),
                discoveredType.declarationKind(),
                context.extractionMode()
            );
            indexNamedEntity(declaredTypes, namedEntities, typeEntity);
        }
    }

    private static void extractOwnedMembersAndTypeRelationships(
        TypeScriptExtractionContext context,
        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discoveredDeclarations,
        Map<String, ExtractedEntityFact> declaredTypes
    ) {
        for (SyntaxNode classNode : discoveredDeclarations.classDeclarations()) {
            ExtractedEntityFact typeEntity = declaredTypes.get(SyntaxTreeExtractionSupport.declarationName(classNode));
            if (typeEntity != null) {
                TypeScriptOwnedMemberExtractionSupport.addOwnedMembers(context.parseResult(), context.accumulator(), typeEntity, context.relativePath(), classNode, context.extractionMode(), "class", declaredTypes);
                TypeScriptTypeRelationshipSupport.addTypeRelationships(context.accumulator(), context.relativePath(), classNode, typeEntity, declaredTypes);
            }
        }
        for (SyntaxNode interfaceNode : discoveredDeclarations.interfaceDeclarations()) {
            ExtractedEntityFact typeEntity = declaredTypes.get(SyntaxTreeExtractionSupport.declarationName(interfaceNode));
            if (typeEntity != null) {
                TypeScriptOwnedMemberExtractionSupport.addOwnedMembers(context.parseResult(), context.accumulator(), typeEntity, context.relativePath(), interfaceNode, context.extractionMode(), "interface", declaredTypes);
                TypeScriptTypeRelationshipSupport.addTypeRelationships(context.accumulator(), context.relativePath(), interfaceNode, typeEntity, declaredTypes);
            }
        }
    }

    private static void extractFunctions(
        TypeScriptExtractionContext context,
        TypeScriptDeclarationDiscoverySupport.TypeScriptDiscoveredDeclarations discoveredDeclarations,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        for (SyntaxNode functionNode : discoveredDeclarations.functionDeclarations()) {
            ExtractedEntityFact functionEntity = TypeScriptNamedDeclarationSemanticsSupport.addNamedEntityFromNode(
                context.parseResult(),
                context.accumulator(),
                context.fileEntity().id(),
                context.relativePath(),
                functionNode,
                EntityKind.FUNCTION,
                "function_declaration",
                "function",
                context.extractionMode()
            );
            if (functionEntity != null) {
                namedEntities.putIfAbsent(functionEntity.name(), functionEntity);
            }
        }
        for (SyntaxNode variableDeclarator : discoveredDeclarations.arrowFunctionDeclarators()) {
            ExtractedEntityFact functionEntity = TypeScriptNamedDeclarationSemanticsSupport.addNamedEntityFromNode(
                context.parseResult(),
                context.accumulator(),
                context.fileEntity().id(),
                context.relativePath(),
                variableDeclarator,
                EntityKind.FUNCTION,
                "arrow_function",
                "function",
                context.extractionMode()
            );
            if (functionEntity != null) {
                namedEntities.putIfAbsent(functionEntity.name(), functionEntity);
            }
        }
    }

    private static void indexNamedEntity(
        Map<String, ExtractedEntityFact> declaredTypes,
        Map<String, ExtractedEntityFact> namedEntities,
        ExtractedEntityFact typeEntity
    ) {
        if (typeEntity == null) {
            return;
        }
        declaredTypes.putIfAbsent(typeEntity.name(), typeEntity);
        namedEntities.putIfAbsent(typeEntity.name(), typeEntity);
        Object qualifiedName = typeEntity.metadata().get("qualifiedName");
        if (qualifiedName instanceof String qualified && !qualified.isBlank()) {
            declaredTypes.putIfAbsent(qualified, typeEntity);
            namedEntities.putIfAbsent(qualified, typeEntity);
        }
    }

    record TypeScriptDeclarationResult(
        Map<String, ExtractedEntityFact> declaredTypes,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
    }
}