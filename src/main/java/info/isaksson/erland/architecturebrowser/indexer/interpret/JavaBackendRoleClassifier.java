package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class JavaBackendRoleClassifier {
    JavaBackendInterpretationClassification classifyRole(ExtractedEntityFact entity, InterpretationContext context) {
        if (InterpretationContext.isTestEntity(entity)) {
            return null;
        }
        List<String> annotations = InterpretationContext.listMetadata(entity, "annotations");
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String packageName = String.valueOf(entity.metadata().getOrDefault("packageName", "")).toLowerCase(Locale.ROOT);
        String snippet = InterpretationContext.primaryRef(entity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(entity).snippet());

        if (Boolean.TRUE.equals(entity.metadata().get("jaxRsResource")) || matchesAny(annotations, "path", "restcontroller", "controller") || packageName.contains(".api") || lowerName.endsWith("resource") || lowerName.endsWith("controller")) {
            return new JavaBackendInterpretationClassification(
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
            return new JavaBackendInterpretationClassification(
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
            return new JavaBackendInterpretationClassification(
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

    EntityKind inferredRoleKind(ExtractedEntityFact entity) {
        if (InterpretationContext.isTestEntity(entity)) {
            return null;
        }
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

    boolean entityMatchesOwner(ExtractedEntityFact owner, ExtractedEntityFact member) {
        String ownerQualifiedName = InterpretationContext.stringMetadata(owner, "qualifiedName");
        String memberOwnerQualifiedName = InterpretationContext.stringMetadata(member, "ownerQualifiedName");
        return ownerQualifiedName != null && ownerQualifiedName.equals(memberOwnerQualifiedName);
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

    private List<String> frameworksFor(ExtractedEntityFact entity, boolean persistence, InterpretationContext context) {
        LinkedHashSet<String> frameworks = new LinkedHashSet<>();
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

    private static boolean matchesAny(List<String> annotations, String... values) {
        List<String> normalized = annotations.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
        for (String candidate : values) {
            if (normalized.stream().anyMatch(annotation -> annotation.endsWith(candidate.toLowerCase(Locale.ROOT)))) {
                return true;
            }
        }
        return false;
    }
}
