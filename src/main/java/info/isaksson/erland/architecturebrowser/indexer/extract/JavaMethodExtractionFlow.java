package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.List;
final class JavaMethodExtractionFlow {

    private final JavaEntityMapper entityMapper;
    private final JavaDependencyEmissionFlow dependencyEmissionFlow;
    private final JavaMethodSemanticsFlow methodSemanticsFlow;

    JavaMethodExtractionFlow(
        JavaEntityMapper entityMapper,
        JavaDependencyEmissionFlow dependencyEmissionFlow,
        JavaMethodSemanticsFlow methodSemanticsFlow
    ) {
        this.entityMapper = entityMapper;
        this.dependencyEmissionFlow = dependencyEmissionFlow;
        this.methodSemanticsFlow = methodSemanticsFlow;
    }

    JavaMemberExtractionResult handleMethodNode(JavaMemberNodeRequest request) {
        ExtractedEntityFact methodEntity = entityMapper.toMethodEntity(request.parseResult(), request.relativePath(), request.extractionMode(), request.fileScopeId(), request.node(), request.ownerContext().owningQualifiedName());
        if (methodEntity == null) {
            return JavaMemberExtractionResult.notHandled();
        }
        request.accumulator().addEntity(methodEntity);
        SourceReference ref = methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst();
        String dependencySourceEntityId = JavaOwnershipSupport.dependencySourceEntityId(request.ownerContext(), request.fileEntityId());
        request.accumulator().addRelationship(ExtractionSupport.containsRelationship(
            dependencySourceEntityId,
            methodEntity.id(),
            ref
        ));
        String returnType = String.valueOf(methodEntity.metadata().getOrDefault("returnType", ""));
        if (!returnType.isBlank()) {
            dependencyEmissionFlow.addMethodReturnTypeDependencies(
                request.accumulator(),
                dependencySourceEntityId,
                returnType,
                request.relativePath(),
                request.packageName(),
                JavaSourceReferenceSupport.lineOf(ref, request.node()),
                ref,
                request.importsBySimpleName(),
                request.declaredTypes()
            );
        }
        @SuppressWarnings("unchecked")
        List<String> parameterTypes = (List<String>) methodEntity.metadata().getOrDefault("parameterTypes", List.of());
        if (!parameterTypes.isEmpty()) {
            dependencyEmissionFlow.addMethodParameterDependencies(
                request.accumulator(),
                dependencySourceEntityId,
                parameterTypes,
                JavaGenericSyntaxSupport.isConstructor(methodEntity),
                request.relativePath(),
                request.packageName(),
                JavaSourceReferenceSupport.lineOf(ref, request.node()),
                ref,
                request.importsBySimpleName(),
                request.declaredTypes()
            );
        }
        methodSemanticsFlow.applyMethodSemantics(
            request.accumulator(),
            request.extractionContext(),
            request.node(),
            methodEntity,
            request.ownerContext().owningTypeEntityId(),
            request.ownerContext().owningQualifiedName(),
            request.ownerContext().owningTypeSnippet()
        );
        return JavaMemberExtractionResult.handled(List.of(methodEntity.id()), 1);
    }
}
