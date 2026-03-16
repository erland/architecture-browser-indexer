package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

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

    JavaTypeTraversalResult handleTypeNode(JavaTypeNodeRequest request) {
        if (!JavaStructuralExtractor.isJavaTypeDeclaration(request.node())) {
            return JavaTypeTraversalResult.notHandled(request.ownerContext());
        }
        ExtractedEntityFact typeEntity = entityMapper.toTypeEntity(
            request.parseResult(),
            request.relativePath(),
            request.packageName(),
            request.extractionMode(),
            request.packageScopeId(),
            request.node(),
            request.ownerContext().owningQualifiedName()
        );
        if (typeEntity == null) {
            return JavaTypeTraversalResult.notHandled(request.ownerContext());
        }
        request.accumulator().addEntity(typeEntity);
        SourceReference ref = typeEntity.sourceRefs().isEmpty() ? null : typeEntity.sourceRefs().getFirst();
        request.accumulator().addRelationship(ExtractionSupport.containsRelationship(request.fileEntityId(), typeEntity.id(), ref));
        dependencyEmissionFlow.addTypeRelationships(request.accumulator(), request.relativePath(), request.packageName(), request.node(), typeEntity, request.importsBySimpleName(), request.declaredTypes());
        typeSemanticsFlow.applyTypeSemantics(request.accumulator(), new JavaTypeContext(request.extractionContext(), request.node(), typeEntity));
        typeSemanticsFlow.applyJpaInheritanceFacts(
            request.accumulator(),
            request.relativePath(),
            request.packageName(),
            request.node(),
            typeEntity,
            request.importsBySimpleName(),
            request.declaredTypes()
        );
        Object qualifiedName = typeEntity.metadata().get("qualifiedName");
        String nextOwningQualifiedName = qualifiedName == null ? request.ownerContext().owningQualifiedName() : String.valueOf(qualifiedName);
        return JavaTypeTraversalResult.handled(new JavaOwnerContext(
            typeEntity.id(),
            nextOwningQualifiedName,
            request.node().textSnippet()
        ));
    }
}
