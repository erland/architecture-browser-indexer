package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

final class JavaJpaSemanticsSupport {
    private final JavaJpaTypeSemanticsSupport typeSemantics;
    private final JavaJpaPropertySemanticsSupport propertySemantics;

    JavaJpaSemanticsSupport(JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter) {
        this.typeSemantics = new JavaJpaTypeSemanticsSupport(relationshipEvidenceEmitter);
        this.propertySemantics = new JavaJpaPropertySemanticsSupport(
            new JavaJpaAssociationSemanticsSupport(relationshipEvidenceEmitter)
        );
    }

    void addJpaTypeMetadata(ExtractionAccumulator accumulator, JavaTypeContext typeContext) {
        typeSemantics.addJpaTypeMetadata(accumulator, typeContext);
    }

    void addJpaFieldFacts(ExtractionAccumulator accumulator, JavaFieldContext fieldContext) {
        propertySemantics.addJpaFieldFacts(accumulator, fieldContext);
    }

    void addJpaMethodFacts(ExtractionAccumulator accumulator, JavaMethodContext methodContext) {
        propertySemantics.addJpaMethodFacts(accumulator, methodContext);
    }

    void addJpaInheritanceFacts(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        typeSemantics.addJpaInheritanceFacts(
            accumulator,
            relativePath,
            packageName,
            typeNode,
            typeEntity,
            importsBySimpleName,
            declaredTypes
        );
    }
}
