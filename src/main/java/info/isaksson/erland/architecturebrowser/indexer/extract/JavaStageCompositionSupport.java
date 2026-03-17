package info.isaksson.erland.architecturebrowser.indexer.extract;

final class JavaStageCompositionSupport {

    JavaStageComposition composeDefault(JavaSyntaxTreeExtractionStage stage) {
        JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter = new JavaRelationshipEvidenceEmitter();
        JavaDependencyEmissionFlow dependencyEmissionFlow = new JavaDependencyEmissionFlow(relationshipEvidenceEmitter);

        JavaSyntaxTreeExtractionStage.JavaJaxRsSemantics jaxRsSemantics = stage.new JavaJaxRsSemantics();
        JavaSyntaxTreeExtractionStage.JavaJpaSemantics jpaSemantics = stage.new JavaJpaSemantics();
        JavaSyntaxTreeExtractionStage.JavaCdiSemantics cdiSemantics = stage.new JavaCdiSemantics();
        JavaSyntaxTreeExtractionStage.JavaWritePathSemantics writePathSemantics = stage.new JavaWritePathSemantics();

        JavaEntityMapper entityMapper = new JavaEntityMapper();
        JavaTypeSemanticsFlow typeSemanticsFlow = new JavaTypeSemanticsFlow(jaxRsSemantics, jpaSemantics);
        JavaTypeDeclarationFlow typeDeclarationFlow = new JavaTypeDeclarationFlow(
            entityMapper,
            dependencyEmissionFlow,
            typeSemanticsFlow
        );

        JavaMethodSemanticsFlow methodSemanticsFlow = new JavaMethodSemanticsFlow(
            new JavaJaxRsMethodSemantics(jaxRsSemantics),
            new JavaJpaMethodSemantics(jpaSemantics),
            new JavaCdiMethodSemantics(cdiSemantics),
            new JavaWritePathMethodSemantics(writePathSemantics)
        );

        JavaFieldExtractionFlow fieldExtractionFlow = new JavaFieldExtractionFlow(
            entityMapper,
            dependencyEmissionFlow,
            new JavaJpaFieldSemantics(jpaSemantics)
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
            traversalNodeDispatchFlow
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
