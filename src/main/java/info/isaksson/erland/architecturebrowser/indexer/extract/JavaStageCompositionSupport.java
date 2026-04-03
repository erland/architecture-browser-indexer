package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.Map;
final class JavaStageCompositionSupport {

    JavaStageComposition composeDefault() {
        return composeDefault(Map.of());
    }

    JavaStageComposition composeDefault(Map<String, JavaDeclaredType> workspaceDeclaredTypes) {
        JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter = new JavaRelationshipEvidenceEmitter();
        JavaDependencyEmissionFlow dependencyEmissionFlow = new JavaDependencyEmissionFlow(relationshipEvidenceEmitter);

        JavaEntityMapper entityMapper = new JavaEntityMapper();
        JavaTypeSemanticsFlow typeSemanticsFlow = new JavaTypeSemanticsFlow(new JavaTypeNodeSemanticsSupport(relationshipEvidenceEmitter));
        JavaTypeDeclarationFlow typeDeclarationFlow = new JavaTypeDeclarationFlow(
            entityMapper,
            dependencyEmissionFlow,
            typeSemanticsFlow
        );

        JavaMethodSemanticsFlow methodSemanticsFlow = new JavaMethodSemanticsFlow(
            new JavaJaxRsMethodSemantics(new JavaJaxRsSemanticsSupport()),
            new JavaJpaMethodSemantics(new JavaJpaSemanticsSupport(relationshipEvidenceEmitter)),
            new JavaCdiMethodSemantics(new JavaCdiSemanticsSupport(relationshipEvidenceEmitter)),
            new JavaWritePathMethodSemantics(new JavaWritePathSemanticsSupport(relationshipEvidenceEmitter))
        );

        JavaFieldExtractionFlow fieldExtractionFlow = new JavaFieldExtractionFlow(
            entityMapper,
            dependencyEmissionFlow,
            new JavaJpaFieldSemantics(new JavaJpaSemanticsSupport(relationshipEvidenceEmitter))
        );
        JavaMethodExtractionFlow methodExtractionFlow = new JavaMethodExtractionFlow(
            entityMapper,
            dependencyEmissionFlow,
            methodSemanticsFlow
        );
        JavaTraversalNodeDispatchFlow traversalNodeDispatchFlow = new JavaTraversalNodeDispatchFlow(
            typeDeclarationFlow,
            fieldExtractionFlow,
            methodExtractionFlow
        );
        JavaCompilationUnitExtractionFlow compilationUnitExtractionFlow = new JavaCompilationUnitExtractionFlow(
            new JavaSyntaxTreeTraversal(),
            traversalNodeDispatchFlow,
            workspaceDeclaredTypes
        );

        return new JavaStageComposition(
            relationshipEvidenceEmitter,
            typeDeclarationFlow,
            traversalNodeDispatchFlow,
            compilationUnitExtractionFlow
        );
    }

    record JavaStageComposition(
        JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter,
        JavaTypeDeclarationFlow typeDeclarationFlow,
        JavaTraversalNodeDispatchFlow traversalNodeDispatchFlow,
        JavaCompilationUnitExtractionFlow compilationUnitExtractionFlow
    ) {
    }
}
