package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class JavaFieldExtractionFlow {

    private final JavaEntityMapper entityMapper;
    private final JavaDependencyEmissionFlow dependencyEmissionFlow;
    private final JavaJpaFieldSemantics jpaFieldSemantics;

    JavaFieldExtractionFlow(
        JavaEntityMapper entityMapper,
        JavaDependencyEmissionFlow dependencyEmissionFlow,
        JavaJpaFieldSemantics jpaFieldSemantics
    ) {
        this.entityMapper = entityMapper;
        this.dependencyEmissionFlow = dependencyEmissionFlow;
        this.jpaFieldSemantics = jpaFieldSemantics;
    }

    JavaMemberExtractionResult handleFieldNode(
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
        List<String> emittedEntityIds = new ArrayList<>();
        int emittedRelationshipCount = 0;
        for (ExtractedEntityFact fieldEntity : entityMapper.toFieldEntities(parseResult, relativePath, extractionMode, fileScopeId, node, owningQualifiedName)) {
            emittedEntityIds.add(fieldEntity.id());
            accumulator.addEntity(fieldEntity);
            SourceReference ref = fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst();
            String dependencySourceEntityId = owningTypeEntityId == null ? fileEntityId : owningTypeEntityId;
            accumulator.addRelationship(ExtractionSupport.containsRelationship(
                dependencySourceEntityId,
                fieldEntity.id(),
                ref
            ));
            emittedRelationshipCount++;
            dependencyEmissionFlow.addFieldDeclaredTypeDependencies(
                accumulator,
                dependencySourceEntityId,
                String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", "")),
                relativePath,
                packageName,
                JavaSyntaxTreeExtractionStage.lineOf(ref, node),
                ref,
                importsBySimpleName,
                declaredTypes
            );
            jpaFieldSemantics.apply(
                accumulator,
                new JavaFieldContext(
                    extractionContext,
                    node,
                    fieldEntity,
                    owningTypeEntityId,
                    owningQualifiedName,
                    owningTypeSnippet
                )
            );
        }
        return JavaMemberExtractionResult.handled(emittedEntityIds, emittedRelationshipCount);
    }
}
