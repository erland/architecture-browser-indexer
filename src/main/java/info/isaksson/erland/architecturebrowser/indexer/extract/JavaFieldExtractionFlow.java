package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.List;
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

    JavaMemberExtractionResult handleFieldNode(JavaMemberNodeRequest request) {
        List<String> emittedEntityIds = new ArrayList<>();
        int emittedRelationshipCount = 0;
        for (ExtractedEntityFact fieldEntity : entityMapper.toFieldEntities(request.parseResult(), request.relativePath(), request.extractionMode(), request.fileScopeId(), request.node(), request.ownerContext().owningQualifiedName())) {
            emittedEntityIds.add(fieldEntity.id());
            request.accumulator().addEntity(fieldEntity);
            SourceReference ref = fieldEntity.sourceRefs().isEmpty() ? null : fieldEntity.sourceRefs().getFirst();
            String dependencySourceEntityId = request.ownerContext().owningTypeEntityId() == null ? request.fileEntityId() : request.ownerContext().owningTypeEntityId();
            request.accumulator().addRelationship(ExtractionSupport.containsRelationship(
                dependencySourceEntityId,
                fieldEntity.id(),
                ref
            ));
            emittedRelationshipCount++;
            dependencyEmissionFlow.addFieldDeclaredTypeDependencies(
                request.accumulator(),
                dependencySourceEntityId,
                String.valueOf(fieldEntity.metadata().getOrDefault("declaredType", "")),
                request.relativePath(),
                request.packageName(),
                JavaSyntaxTreeExtractionStage.lineOf(ref, request.node()),
                ref,
                request.importsBySimpleName(),
                request.declaredTypes()
            );
            jpaFieldSemantics.apply(
                request.accumulator(),
                new JavaFieldContext(
                    request.extractionContext(),
                    request.node(),
                    fieldEntity,
                    request.ownerContext().owningTypeEntityId(),
                    request.ownerContext().owningQualifiedName(),
                    request.ownerContext().owningTypeSnippet()
                )
            );
        }
        return JavaMemberExtractionResult.handled(emittedEntityIds, emittedRelationshipCount);
    }
}
