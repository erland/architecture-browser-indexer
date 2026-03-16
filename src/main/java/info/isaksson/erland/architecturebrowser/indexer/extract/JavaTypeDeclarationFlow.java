package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

final class JavaTypeDeclarationFlow {

    private final JavaEntityMapper entityMapper;
    private final JavaDependencyEmissionFlow dependencyEmissionFlow;
    private final JavaTypeSemanticsFlow typeSemanticsFlow;

    JavaTypeDeclarationFlow(
        JavaEntityMapper entityMapper,
        JavaDependencyEmissionFlow dependencyEmissionFlow,
        JavaTypeSemanticsFlow typeSemanticsFlow
    ) {
        this.entityMapper = entityMapper;
        this.dependencyEmissionFlow = dependencyEmissionFlow;
        this.typeSemanticsFlow = typeSemanticsFlow;
    }

    JavaTypeTraversalResult handleTypeNode(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String packageScopeId,
        String fileEntityId,
        SyntaxNode node,
        String currentOwningTypeEntityId,
        String currentOwningQualifiedName,
        String currentOwningTypeSnippet,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes,
        JavaExtractionContext extractionContext
    ) {
        if (!JavaStructuralExtractor.isJavaTypeDeclaration(node)) {
            return JavaTypeTraversalResult.notHandled(
                currentOwningTypeEntityId,
                currentOwningQualifiedName,
                currentOwningTypeSnippet
            );
        }
        ExtractedEntityFact typeEntity = entityMapper.toTypeEntity(
            parseResult,
            relativePath,
            packageName,
            extractionMode,
            packageScopeId,
            node,
            currentOwningQualifiedName
        );
        if (typeEntity == null) {
            return JavaTypeTraversalResult.notHandled(
                currentOwningTypeEntityId,
                currentOwningQualifiedName,
                currentOwningTypeSnippet
            );
        }
        accumulator.addEntity(typeEntity);
        SourceReference ref = typeEntity.sourceRefs().isEmpty() ? null : typeEntity.sourceRefs().getFirst();
        accumulator.addRelationship(ExtractionSupport.containsRelationship(fileEntityId, typeEntity.id(), ref));
        dependencyEmissionFlow.addTypeRelationships(accumulator, relativePath, packageName, node, typeEntity, importsBySimpleName, declaredTypes);
        typeSemanticsFlow.applyTypeSemantics(accumulator, new JavaTypeContext(extractionContext, node, typeEntity));
        typeSemanticsFlow.applyJpaInheritanceFacts(
            accumulator,
            relativePath,
            packageName,
            node,
            typeEntity,
            importsBySimpleName,
            declaredTypes
        );
        String nextOwningTypeEntityId = typeEntity.id();
        String nextOwningTypeSnippet = node.textSnippet();
        Object qualifiedName = typeEntity.metadata().get("qualifiedName");
        String nextOwningQualifiedName = qualifiedName == null ? currentOwningQualifiedName : String.valueOf(qualifiedName);
        return JavaTypeTraversalResult.handled(
            nextOwningTypeEntityId,
            nextOwningQualifiedName,
            nextOwningTypeSnippet
        );
    }
}
