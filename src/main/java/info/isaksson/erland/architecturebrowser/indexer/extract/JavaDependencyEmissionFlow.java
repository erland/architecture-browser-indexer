package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Map;

final class JavaDependencyEmissionFlow {

    private final JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter;

    JavaDependencyEmissionFlow(JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter) {
        this.relationshipEvidenceEmitter = relationshipEvidenceEmitter;
    }

    void addTypeRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        String packageName,
        SyntaxNode typeNode,
        ExtractedEntityFact typeEntity,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        relationshipEvidenceEmitter.addTypeRelationships(
            accumulator,
            relativePath,
            packageName,
            typeNode,
            typeEntity,
            importsBySimpleName,
            declaredTypes
        );
    }

    void addFieldDeclaredTypeDependencies(
        ExtractionAccumulator accumulator,
        String dependencySourceEntityId,
        String declaredType,
        String relativePath,
        String packageName,
        int line,
        SourceReference ref,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        relationshipEvidenceEmitter.addDeclaredTypeDependencies(
            accumulator,
            dependencySourceEntityId,
            List.of(String.valueOf(declaredType == null ? "" : declaredType)),
            relativePath,
            packageName,
            line,
            ref,
            importsBySimpleName,
            declaredTypes,
            relationshipEvidenceEmitter.dependencyMetadata("field", "composition")
        );
    }

    void addMethodReturnTypeDependencies(
        ExtractionAccumulator accumulator,
        String dependencySourceEntityId,
        String returnType,
        String relativePath,
        String packageName,
        int line,
        SourceReference ref,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        if (returnType == null || returnType.isBlank()) {
            return;
        }
        relationshipEvidenceEmitter.addDeclaredTypeDependencies(
            accumulator,
            dependencySourceEntityId,
            List.of(returnType),
            relativePath,
            packageName,
            line,
            ref,
            importsBySimpleName,
            declaredTypes,
            relationshipEvidenceEmitter.dependencyMetadata("returnType", "api")
        );
    }

    void addMethodParameterDependencies(
        ExtractionAccumulator accumulator,
        String dependencySourceEntityId,
        List<String> parameterTypes,
        boolean constructor,
        String relativePath,
        String packageName,
        int line,
        SourceReference ref,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return;
        }
        relationshipEvidenceEmitter.addDeclaredTypeDependencies(
            accumulator,
            dependencySourceEntityId,
            parameterTypes,
            relativePath,
            packageName,
            line,
            ref,
            importsBySimpleName,
            declaredTypes,
            relationshipEvidenceEmitter.dependencyMetadata(constructor ? "constructorParameter" : "parameterType", "api")
        );
    }
}
