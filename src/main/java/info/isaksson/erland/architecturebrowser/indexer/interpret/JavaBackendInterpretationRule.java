package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class JavaBackendInterpretationRule implements InterpretationRule {
    private final JavaBackendRoleClassifier roleClassifier = new JavaBackendRoleClassifier();
    private final JavaEndpointInterpreterSupport endpointSupport = new JavaEndpointInterpreterSupport();

    @Override
    public String ruleId() {
        return "java-backend-high-value";
    }

    @Override
    public void apply(InterpretationContext context, InterpretationAccumulator accumulator) {
        Map<String, ExtractedEntityFact> roleSourcesByQualifiedName = new LinkedHashMap<>();
        for (ExtractedEntityFact entity : context.entitiesByLanguage("java")) {
            if (entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE) {
                inferRoleEntities(entity, context, accumulator, roleSourcesByQualifiedName);
            }
        }
        for (ExtractedEntityFact entity : context.entitiesByLanguage("java")) {
            if (entity.kind() == EntityKind.FUNCTION) {
                inferEndpoints(entity, context, accumulator);
            } else if (entity.kind() == EntityKind.FIELD) {
                inferFieldCollaboratorUses(entity, context, accumulator, roleSourcesByQualifiedName);
            }
        }
    }

    private void inferRoleEntities(ExtractedEntityFact entity, InterpretationContext context, InterpretationAccumulator accumulator, Map<String, ExtractedEntityFact> roleSourcesByQualifiedName) {
        String declarationKind = String.valueOf(entity.metadata().getOrDefault("declarationKind", "class")).toLowerCase(Locale.ROOT);
        boolean roleEligible = "class".equals(declarationKind) || "interface".equals(declarationKind);
        if (!roleEligible) {
            return;
        }
        JavaBackendInterpretationClassification classification = roleClassifier.classifyRole(entity, context);
        if (classification == null) {
            return;
        }
        String qualifiedName = InterpretationContext.stringMetadata(entity, "qualifiedName");
        String displaySuffix = classification.roleKind() == EntityKind.SERVICE ? " service" : " persistence adapter";
        var role = InterpretationSupport.roleEntity(ruleId(), entity, classification.roleKind(), displaySuffix, classification.metadata());
        accumulator.addEntity(role, ruleId());
        accumulator.addRelationship(InterpretationSupport.relationship(
            ruleId(), RelationshipKind.USES, entity.id(), role.id(), classification.interpretationLabel(), entity.sourceRefs(), Map.of("sourceLanguage", "java")
        ), ruleId());
        if (qualifiedName != null && !qualifiedName.isBlank()) {
            roleSourcesByQualifiedName.putIfAbsent(qualifiedName, entity);
        }
    }

    private void inferEndpoints(ExtractedEntityFact methodEntity, InterpretationContext context, InterpretationAccumulator accumulator) {
        List<String> annotations = InterpretationContext.listMetadata(methodEntity, "annotations");
        String methodAnnotation = endpointSupport.endpointAnnotation(annotations).orElse(null);
        if (methodAnnotation == null) {
            return;
        }
        String sourceSnippet = InterpretationContext.primaryRef(methodEntity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(methodEntity).snippet());
        Optional<ExtractedEntityFact> ownerType = context.ownerType(methodEntity);
        String classLevelPath = ownerType.flatMap(endpointSupport::controllerBasePath).orElse("");
        String methodPath = endpointSupport.extractPath(sourceSnippet).orElse("");
        String path = endpointSupport.normalizeEndpointPath(classLevelPath, methodPath);
        String httpMethod = endpointSupport.httpMethodForAnnotation(methodAnnotation);
        var endpoint = InterpretationSupport.endpointEntity(ruleId(), methodEntity, httpMethod, path, Map.of(
            "sourceLanguage", "java",
            "ownerQualifiedName", InterpretationContext.stringMetadata(methodEntity, "ownerQualifiedName") == null ? "" : InterpretationContext.stringMetadata(methodEntity, "ownerQualifiedName")
        ));
        accumulator.addEntity(endpoint, ruleId());

        Optional<ExtractedEntityFact> owner = ownerType.filter(candidate -> endpointSupport.isController(candidate) || !InterpretationContext.listMetadata(candidate, "annotations").isEmpty());
        if (owner.isEmpty()) {
            String pathRef = InterpretationContext.path(methodEntity);
            Integer line = InterpretationContext.line(methodEntity);
            owner = context.nearestClassInFileAboveLine(pathRef, line)
                .filter(candidate -> endpointSupport.isController(candidate) || !InterpretationContext.listMetadata(candidate, "annotations").isEmpty());
        }
        if (owner.isEmpty()) {
            String pathRef = InterpretationContext.path(methodEntity);
            owner = context.fileModule(pathRef);
        }
        owner.ifPresent(sourceOwner -> accumulator.addRelationship(
            InterpretationSupport.relationship(ruleId(), RelationshipKind.EXPOSES, sourceOwner.id(), endpoint.id(), httpMethod + " " + path, methodEntity.sourceRefs(), Map.of("sourceLanguage", "java")),
            ruleId()
        ));
    }

    private void inferFieldCollaboratorUses(
        ExtractedEntityFact fieldEntity,
        InterpretationContext context,
        InterpretationAccumulator accumulator,
        Map<String, ExtractedEntityFact> roleSourcesByQualifiedName
    ) {
        Optional<ExtractedEntityFact> owner = context.ownerType(fieldEntity);
        if (owner.isEmpty()) {
            return;
        }
        for (ExtractedEntityFact dependencyTarget : context.relationshipsFrom(owner.get().id(), RelationshipKind.DEPENDS_ON)) {
            String qualifiedName = InterpretationContext.stringMetadata(dependencyTarget, "qualifiedName");
            if (qualifiedName == null || qualifiedName.isBlank()) {
                qualifiedName = dependencyTarget.name();
            }
            ExtractedEntityFact roleSource = roleSourcesByQualifiedName.get(qualifiedName);
            if (roleSource == null || roleSource.id().equals(owner.get().id())) {
                continue;
            }
            EntityKind interpretedKind = roleClassifier.inferredRoleKind(roleSource);
            if (interpretedKind == null) {
                continue;
            }
            String roleId = InterpretationSupport.roleEntity(ruleId(), roleSource, interpretedKind, interpretedKind == EntityKind.SERVICE ? " service" : " persistence adapter", Map.of()).id();
            accumulator.addRelationship(
                InterpretationSupport.relationship(
                    ruleId(),
                    RelationshipKind.USES,
                    owner.get().id(),
                    roleId,
                    fieldEntity.name(),
                    fieldEntity.sourceRefs(),
                    Map.of(
                        "sourceLanguage", "java",
                        "dependencySource", "field",
                        "fieldName", fieldEntity.name(),
                        "ownerQualifiedName", InterpretationContext.stringMetadata(fieldEntity, "ownerQualifiedName") == null ? "" : InterpretationContext.stringMetadata(fieldEntity, "ownerQualifiedName")
                    )
                ),
                ruleId()
            );
        }
    }
}
