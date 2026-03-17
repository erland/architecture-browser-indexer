package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class JavaStageCompositionSupportTest {

    @Test
    void composesStageCollaboratorsWithStableFlowHandoff() {
        JavaStageCompositionSupport.JavaStageComposition composition = new JavaStageCompositionSupport().composeDefault();

        assertNotNull(composition.relationshipEvidenceEmitter());
        assertNotNull(composition.typeDeclarationFlow());
        assertNotNull(composition.traversalNodeDispatchFlow());
        assertNotNull(composition.compilationUnitExtractionFlow());

        JavaSyntaxTreeExtractionStage stage = new JavaSyntaxTreeExtractionStage(composition);

        assertSame(composition.relationshipEvidenceEmitter(), readField(stage, "relationshipEvidenceEmitter"));
        assertSame(composition.typeDeclarationFlow(), readField(stage, "typeDeclarationFlow"));
        assertSame(composition.traversalNodeDispatchFlow(), readField(stage, "traversalNodeDispatchFlow"));
        assertSame(composition.compilationUnitExtractionFlow(), readField(stage, "compilationUnitExtractionFlow"));
    }

    private static Object readField(JavaSyntaxTreeExtractionStage stage, String fieldName) {
        try {
            var field = JavaSyntaxTreeExtractionStage.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(stage);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
