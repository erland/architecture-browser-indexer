package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaBackendInterpretationRule implements InterpretationRule {
    private static final Pattern PATH_PATTERN = Pattern.compile("([\\\"\'])(/[^\\\"\']*)\\1");

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
        RoleClassification classification = classifyRole(entity, context);
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


    private RoleClassification classifyRole(ExtractedEntityFact entity, InterpretationContext context) {
        List<String> annotations = InterpretationContext.listMetadata(entity, "annotations");
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String packageName = String.valueOf(entity.metadata().getOrDefault("packageName", "")).toLowerCase(Locale.ROOT);
        String snippet = InterpretationContext.primaryRef(entity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(entity).snippet());

        if (Boolean.TRUE.equals(entity.metadata().get("jaxRsResource")) || matchesAny(annotations, "path", "restcontroller", "controller") || packageName.contains(".api") || lowerName.endsWith("resource") || lowerName.endsWith("controller")) {
            return new RoleClassification(
                EntityKind.SERVICE,
                Map.of(
                    "matchType", "framework-or-package",
                    "entityRole", "resource",
                    "backendProfile", "jax-rs-resource",
                    "frameworks", List.of("jax-rs")
                ),
                "interpreted-as-resource"
            );
        }

        boolean serviceByAnnotationOrName = matchesAny(annotations, "service") || lowerName.endsWith("service") || lowerName.endsWith("facade") || lowerName.endsWith("manager");
        boolean serviceByPackage = packageName.contains(".service") || packageName.contains(".application") || packageName.contains(".usecase");
        boolean serviceByDependencies = usesPersistenceRoleDependency(entity, context) || publishesOrObservesCdiEvent(entity, context);
        if (serviceByAnnotationOrName || serviceByPackage || serviceByDependencies) {
            String backendProfile = publishesOrObservesCdiEvent(entity, context) ? "application-service-with-events" : "application-service";
            return new RoleClassification(
                EntityKind.SERVICE,
                Map.of(
                    "matchType", serviceByAnnotationOrName ? "annotation-or-name" : (serviceByPackage ? "package" : "dependency-pattern"),
                    "entityRole", "service",
                    "backendProfile", backendProfile,
                    "frameworks", frameworksFor(entity, false, context)
                ),
                "interpreted-as-service"
            );
        }

        boolean repositoryByAnnotationOrName = matchesAny(annotations, "repository", "mapper") || lowerName.endsWith("repository") || lowerName.endsWith("dao") || lowerName.endsWith("mapper");
        boolean repositoryByPackage = packageName.contains(".repo") || packageName.contains(".repository") || packageName.contains(".dao") || packageName.contains(".mapper") || packageName.contains(".persistence");
        boolean repositoryByStructure = snippet.contains("EntityManager") || snippet.contains("JpaRepository") || snippet.contains("CrudRepository") || hasEntityManagerField(entity, context);
        if (repositoryByAnnotationOrName || repositoryByPackage || repositoryByStructure) {
            String entityRole = lowerName.endsWith("mapper") ? "mapper" : (lowerName.endsWith("dao") ? "dao" : "repository");
            return new RoleClassification(
                EntityKind.PERSISTENCE_ADAPTER,
                Map.of(
                    "matchType", repositoryByAnnotationOrName ? "annotation-or-name" : (repositoryByPackage ? "package" : "structural-evidence"),
                    "entityRole", entityRole,
                    "backendProfile", entityRole.equals("mapper") ? "mapping-adapter" : "repository",
                    "frameworks", frameworksFor(entity, true, context)
                ),
                "interpreted-as-persistence-adapter"
            );
        }

        return null;
    }

    private boolean publishesOrObservesCdiEvent(ExtractedEntityFact entity, InterpretationContext context) {
        return context.entities().stream()
            .filter(candidate -> candidate.kind() == EntityKind.FUNCTION)
            .filter(candidate -> entityMatchesOwner(entity, candidate))
            .anyMatch(candidate -> Boolean.TRUE.equals(candidate.metadata().get("cdiEventPublisher")) || Boolean.TRUE.equals(candidate.metadata().get("cdiObserver")));
    }

    private boolean usesPersistenceRoleDependency(ExtractedEntityFact entity, InterpretationContext context) {
        return context.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && entity.id().equals(rel.fromEntityId()))
            .anyMatch(rel -> {
                ExtractedEntityFact target = context.entityById(rel.toEntityId()).orElse(null);
                return target != null && inferredRoleKind(target) == EntityKind.PERSISTENCE_ADAPTER;
            });
    }

    private boolean hasEntityManagerField(ExtractedEntityFact entity, InterpretationContext context) {
        return context.entities().stream()
            .filter(candidate -> candidate.kind() == EntityKind.FIELD)
            .filter(candidate -> entityMatchesOwner(entity, candidate))
            .map(candidate -> String.valueOf(candidate.metadata().getOrDefault("declaredType", "")))
            .anyMatch(type -> type.contains("EntityManager"));
    }

    private boolean entityMatchesOwner(ExtractedEntityFact owner, ExtractedEntityFact member) {
        String ownerQualifiedName = InterpretationContext.stringMetadata(owner, "qualifiedName");
        String memberOwnerQualifiedName = InterpretationContext.stringMetadata(member, "ownerQualifiedName");
        return ownerQualifiedName != null && ownerQualifiedName.equals(memberOwnerQualifiedName);
    }

    private List<String> frameworksFor(ExtractedEntityFact entity, boolean persistence, InterpretationContext context) {
        java.util.LinkedHashSet<String> frameworks = new java.util.LinkedHashSet<>();
        if (Boolean.TRUE.equals(entity.metadata().get("jaxRsResource")) || matchesAny(InterpretationContext.listMetadata(entity, "annotations"), "path")) {
            frameworks.add("jax-rs");
        }
        if (persistence) {
            frameworks.add("persistence");
        }
        if (publishesOrObservesCdiEvent(entity, context) || hasInjectAnnotation(entity, context)) {
            frameworks.add("cdi");
        }
        return List.copyOf(frameworks);
    }

    private boolean hasInjectAnnotation(ExtractedEntityFact entity, InterpretationContext context) {
        return context.entities().stream()
            .filter(candidate -> candidate.kind() == EntityKind.FIELD || candidate.kind() == EntityKind.FUNCTION)
            .filter(candidate -> entityMatchesOwner(entity, candidate))
            .anyMatch(candidate -> matchesAny(InterpretationContext.listMetadata(candidate, "annotations"), "inject"));
    }

    private record RoleClassification(EntityKind roleKind, Map<String, Object> metadata, String interpretationLabel) {}

    private void inferEndpoints(ExtractedEntityFact methodEntity, InterpretationContext context, InterpretationAccumulator accumulator) {
        List<String> annotations = InterpretationContext.listMetadata(methodEntity, "annotations");
        String methodAnnotation = endpointAnnotation(annotations).orElse(null);
        if (methodAnnotation == null) {
            return;
        }
        String sourceSnippet = InterpretationContext.primaryRef(methodEntity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(methodEntity).snippet());
        Optional<ExtractedEntityFact> ownerType = context.ownerType(methodEntity);
        String classLevelPath = ownerType.flatMap(JavaBackendInterpretationRule::controllerBasePath).orElse("");
        String methodPath = extractPath(sourceSnippet).orElse("");
        String path = normalizeEndpointPath(classLevelPath, methodPath);
        String httpMethod = httpMethodForAnnotation(methodAnnotation);
        var endpoint = InterpretationSupport.endpointEntity(ruleId(), methodEntity, httpMethod, path, Map.of(
            "sourceLanguage", "java",
            "ownerQualifiedName", InterpretationContext.stringMetadata(methodEntity, "ownerQualifiedName") == null ? "" : InterpretationContext.stringMetadata(methodEntity, "ownerQualifiedName")
        ));
        accumulator.addEntity(endpoint, ruleId());

        Optional<ExtractedEntityFact> owner = ownerType.filter(candidate -> isController(candidate) || !InterpretationContext.listMetadata(candidate, "annotations").isEmpty());
        if (owner.isEmpty()) {
            String pathRef = InterpretationContext.path(methodEntity);
            Integer line = InterpretationContext.line(methodEntity);
            owner = context.nearestClassInFileAboveLine(pathRef, line)
                .filter(candidate -> isController(candidate) || !InterpretationContext.listMetadata(candidate, "annotations").isEmpty());
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
            EntityKind interpretedKind = inferredRoleKind(roleSource);
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

    private static EntityKind inferredRoleKind(ExtractedEntityFact entity) {
        List<String> annotations = InterpretationContext.listMetadata(entity, "annotations");
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String packageName = String.valueOf(entity.metadata().getOrDefault("packageName", "")).toLowerCase(Locale.ROOT);
        String declarationKind = String.valueOf(entity.metadata().getOrDefault("declarationKind", "class")).toLowerCase(Locale.ROOT);
        if (!("class".equals(declarationKind) || "interface".equals(declarationKind))) {
            return null;
        }
        if (Boolean.TRUE.equals(entity.metadata().get("jaxRsResource"))
            || matchesAny(annotations, "service", "path", "restcontroller", "controller")
            || lowerName.endsWith("service") || lowerName.endsWith("facade") || lowerName.endsWith("manager")
            || lowerName.endsWith("resource") || lowerName.endsWith("controller")
            || packageName.contains(".service") || packageName.contains(".api") || packageName.contains(".application") || packageName.contains(".usecase")) {
            return EntityKind.SERVICE;
        }
        if (matchesAny(annotations, "repository", "mapper") || lowerName.endsWith("repository") || lowerName.endsWith("dao") || lowerName.endsWith("mapper")
            || packageName.contains(".repo") || packageName.contains(".repository") || packageName.contains(".dao") || packageName.contains(".mapper") || packageName.contains(".persistence")) {
            return EntityKind.PERSISTENCE_ADAPTER;
        }
        return null;
    }

    private static Optional<String> controllerBasePath(ExtractedEntityFact entity) {
        String snippet = InterpretationContext.primaryRef(entity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(entity).snippet());
        return extractPath(snippet);
    }

    private static String normalizeEndpointPath(String classLevelPath, String methodPath) {
        String base = classLevelPath == null ? "" : classLevelPath.strip();
        String method = methodPath == null ? "" : methodPath.strip();
        if (base.isEmpty() && method.isEmpty()) {
            return "/";
        }
        if (base.isEmpty()) {
            return normalizeSinglePath(method);
        }
        if (method.isEmpty() || "/".equals(method)) {
            return normalizeSinglePath(base);
        }
        String normalizedBase = normalizeSinglePath(base);
        String normalizedMethod = normalizeSinglePath(method);
        if ("/".equals(normalizedBase)) {
            return normalizedMethod;
        }
        if ("/".equals(normalizedMethod)) {
            return normalizedBase;
        }
        return (normalizedBase.endsWith("/") ? normalizedBase.substring(0, normalizedBase.length() - 1) : normalizedBase)
            + (normalizedMethod.startsWith("/") ? normalizedMethod : "/" + normalizedMethod);
    }

    private static String normalizeSinglePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        String normalized = value.startsWith("/") ? value : "/" + value;
        return normalized.replaceAll("//+", "/");
    }

    private static boolean isController(ExtractedEntityFact entity) {
        List<String> annotations = InterpretationContext.listMetadata(entity, "annotations");
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        return matchesAny(annotations, "restcontroller", "controller", "path") || lowerName.endsWith("controller");
    }

    private static boolean matchesAny(List<String> annotations, String... values) {
        List<String> normalized = annotations.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
        for (String candidate : values) {
            if (normalized.stream().anyMatch(annotation -> annotation.endsWith(candidate.toLowerCase(Locale.ROOT)))) {
                return true;
            }
        }
        return false;
    }

    private static Optional<String> endpointAnnotation(List<String> annotations) {
        return annotations.stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .filter(value -> value.endsWith("getmapping") || value.endsWith("postmapping") || value.endsWith("putmapping")
                || value.endsWith("deletemapping") || value.endsWith("patchmapping") || value.endsWith("requestmapping"))
            .findFirst();
    }

    private static String httpMethodForAnnotation(String annotation) {
        String normalized = annotation.toLowerCase(Locale.ROOT);
        if (normalized.endsWith("getmapping")) {
            return "GET";
        }
        if (normalized.endsWith("postmapping")) {
            return "POST";
        }
        if (normalized.endsWith("putmapping")) {
            return "PUT";
        }
        if (normalized.endsWith("deletemapping")) {
            return "DELETE";
        }
        if (normalized.endsWith("patchmapping")) {
            return "PATCH";
        }
        return "REQUEST";
    }

    private static Optional<String> extractPath(String sourceSnippet) {
        if (sourceSnippet == null) {
            return Optional.empty();
        }
        Matcher matcher = PATH_PATTERN.matcher(sourceSnippet);
        return matcher.find() ? Optional.ofNullable(matcher.group(2)) : Optional.empty();
    }
}
