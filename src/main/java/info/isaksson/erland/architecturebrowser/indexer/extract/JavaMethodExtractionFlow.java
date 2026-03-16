package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Map;

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

    JavaMemberExtractionResult handleMethodNode(
        SourceParseResult parseResult,
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        ExtractionMode extractionMode,
        String fileScopeId,
        String fileEntityId,
        SyntaxNode node,
        String owningTypeEntityId,
        String owningQualifiedName,
        String owningTypeSnippet,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes,
        JavaExtractionContext extractionContext
    ) {
        ExtractedEntityFact methodEntity = entityMapper.toMethodEntity(parseResult, relativePath, extractionMode, fileScopeId, node, owningQualifiedName);
        if (methodEntity == null) {
            return JavaMemberExtractionResult.notHandled();
        }
        accumulator.addEntity(methodEntity);
        SourceReference ref = methodEntity.sourceRefs().isEmpty() ? null : methodEntity.sourceRefs().getFirst();
        String dependencySourceEntityId = owningTypeEntityId == null ? fileEntityId : owningTypeEntityId;
        accumulator.addRelationship(ExtractionSupport.containsRelationship(
            dependencySourceEntityId,
            methodEntity.id(),
            ref
        ));
        String returnType = String.valueOf(methodEntity.metadata().getOrDefault("returnType", ""));
        if (!returnType.isBlank()) {
            dependencyEmissionFlow.addMethodReturnTypeDependencies(
                accumulator,
                dependencySourceEntityId,
                returnType,
                relativePath,
                packageName,
                JavaSyntaxTreeExtractionStage.lineOf(ref, node),
                ref,
                importsBySimpleName,
                declaredTypes
            );
        }
        @SuppressWarnings("unchecked")
        List<String> parameterTypes = (List<String>) methodEntity.metadata().getOrDefault("parameterTypes", List.of());
        if (!parameterTypes.isEmpty()) {
            dependencyEmissionFlow.addMethodParameterDependencies(
                accumulator,
                dependencySourceEntityId,
                parameterTypes,
                JavaSyntaxTreeExtractionStage.isConstructor(methodEntity),
                relativePath,
                packageName,
                JavaSyntaxTreeExtractionStage.lineOf(ref, node),
                ref,
                importsBySimpleName,
                declaredTypes
            );
        }
        methodSemanticsFlow.applyMethodSemantics(
            accumulator,
            extractionContext,
            node,
            methodEntity,
            owningTypeEntityId,
            owningQualifiedName,
            owningTypeSnippet
        );
        return JavaMemberExtractionResult.handled(List.of(methodEntity.id()), 1);
    }
}
