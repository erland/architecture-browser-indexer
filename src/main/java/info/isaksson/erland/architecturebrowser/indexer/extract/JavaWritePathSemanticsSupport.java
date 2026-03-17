package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaWritePathSemanticsSupport {
    private final JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter;

    JavaWritePathSemanticsSupport(JavaRelationshipEvidenceEmitter relationshipEvidenceEmitter) {
        this.relationshipEvidenceEmitter = relationshipEvidenceEmitter;
    }

    private JavaRelationshipEvidenceEmitter.ResolvedJavaType resolveJavaTypeReference(
        ExtractionAccumulator accumulator,
        String referencedType,
        EntityKind fallbackTargetKind,
        String relativePath,
        String packageName,
        int line,
        Map<String, String> importsBySimpleName,
        Map<String, JavaDeclaredType> declaredTypes
    ) {
        return relationshipEvidenceEmitter.resolveJavaTypeReference(
            accumulator, referencedType, fallbackTargetKind, relativePath, packageName, line, importsBySimpleName, declaredTypes
        );
    }
void addWritePathFacts(
        ExtractionAccumulator accumulator,
        JavaMethodContext methodContext
    ) {
        JavaExtractionContext extractionContext = methodContext.extractionContext();
        String relativePath = extractionContext.relativePath();
        String packageName = extractionContext.packageName();
        SyntaxNode methodNode = methodContext.methodNode();
        ExtractedEntityFact methodEntity = methodContext.methodEntity();
        String ownerTypeEntityId = methodContext.ownerTypeEntityId();
        String ownerQualifiedName = methodContext.ownerQualifiedName();
        SourceReference ref = methodContext.sourceRef();
        String snippet = methodContext.snippet();
        Map<String, String> importsBySimpleName = extractionContext.importsBySimpleName();
        Map<String, JavaDeclaredType> declaredTypes = extractionContext.declaredTypes();
        if (methodEntity == null || ownerTypeEntityId == null) {
            return;
        }

        List<JavaWritePathDetectionSupport.DetectedWritePath> detections = new ArrayList<>();
        detections.addAll(JavaWritePathDetectionSupport.detectJpaWriteOperations(methodEntity, snippet));
        detections.addAll(JavaWritePathDetectionSupport.detectRepositoryWriteOperations(methodEntity, snippet));
        if (detections.isEmpty()) {
            return;
        }

        Map<String, String> variableTypes = JavaWritePathDetectionSupport.collectMethodVariableTypes(methodEntity, snippet);
        LinkedHashMap<String, Object> methodMetadata = new LinkedHashMap<>(methodEntity.metadata());
        java.util.LinkedHashSet<String> writeOperations = new java.util.LinkedHashSet<>(JavaDeclaredTypeSupport.metadataStringList(methodMetadata.get("writeOperations")));
        java.util.LinkedHashSet<String> writeTargets = new java.util.LinkedHashSet<>(JavaDeclaredTypeSupport.metadataStringList(methodMetadata.get("writeEntityTypes")));
        boolean changed = false;

        for (JavaWritePathDetectionSupport.DetectedWritePath detection : detections) {
            String entityType = JavaWritePathDetectionSupport.resolveWriteTargetEntityType(detection.argumentExpression(), variableTypes).orElse(null);
            if (entityType == null || entityType.isBlank()) {
                continue;
            }
            JavaRelationshipEvidenceEmitter.ResolvedJavaType target = resolveJavaTypeReference(
                accumulator,
                entityType,
                EntityKind.CLASS,
                relativePath,
                packageName,
                JavaGenericSyntaxSupport.lineOf(ref, methodNode),
                importsBySimpleName,
                declaredTypes
            );
            if (target == null) {
                continue;
            }
            LinkedHashMap<String, Object> relationshipMetadata = new LinkedHashMap<>();
            relationshipMetadata.put("framework", "jpa");
            relationshipMetadata.put("relationshipType", "writePath");
            relationshipMetadata.put("writeOperation", detection.operation());
            relationshipMetadata.put("writeKind", detection.writeKind());
            relationshipMetadata.put("writerMethod", methodEntity.name());
            relationshipMetadata.put("writerQualifiedName", ownerQualifiedName == null ? "" : ownerQualifiedName);
            relationshipMetadata.put("entityType", target.label());
            if (detection.viaField() != null && !detection.viaField().isBlank()) {
                relationshipMetadata.put("writeViaField", detection.viaField());
            }
            if (detection.viaType() != null && !detection.viaType().isBlank()) {
                relationshipMetadata.put("writeViaType", detection.viaType());
            }
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                ownerTypeEntityId,
                target.entityId(),
                target.label(),
                ref,
                "java",
                Map.copyOf(relationshipMetadata)
            ));
            LinkedHashMap<String, Object> methodRelationshipMetadata = new LinkedHashMap<>(relationshipMetadata);
            methodRelationshipMetadata.put("ownerMemberKind", "method");
            methodRelationshipMetadata.put("ownerMemberName", methodEntity.name());
            accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                methodEntity.id(),
                target.entityId(),
                target.label(),
                ref,
                "java",
                Map.copyOf(methodRelationshipMetadata)
            ));
            writeOperations.add(detection.operation());
            writeTargets.add(target.label());
            changed = true;
        }

        if (changed) {
            methodMetadata.put("writePath", true);
            methodMetadata.put("writeOperations", List.copyOf(writeOperations));
            methodMetadata.put("writeEntityTypes", List.copyOf(writeTargets));
            accumulator.addEntity(new ExtractedEntityFact(
                methodEntity.id(),
                methodEntity.kind(),
                methodEntity.origin(),
                methodEntity.name(),
                methodEntity.displayName(),
                methodEntity.scopeId(),
                List.of(ref),
                Map.copyOf(methodMetadata)
            ));
        }
    }
    
}
