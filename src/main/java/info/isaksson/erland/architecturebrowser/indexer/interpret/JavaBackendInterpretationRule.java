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
        for (ExtractedEntityFact entity : context.entitiesByLanguage("java")) {
            if (entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE) {
                inferRoleEntities(entity, accumulator);
            } else if (entity.kind() == EntityKind.FUNCTION) {
                inferEndpoints(entity, context, accumulator);
            }
        }
    }

    private void inferRoleEntities(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        List<String> annotations = InterpretationContext.listMetadata(entity, "annotations");
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        if (matchesAny(annotations, "service") || lowerName.endsWith("service") || lowerName.endsWith("facade") || lowerName.endsWith("manager")) {
            var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.SERVICE, " service", Map.of("matchType", "annotation-or-name"));
            accumulator.addEntity(role, ruleId());
            accumulator.addRelationship(InterpretationSupport.relationship(
                ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-service", entity.sourceRefs(), Map.of("sourceLanguage", "java")
            ), ruleId());
        }
        if (matchesAny(annotations, "repository", "mapper") || lowerName.endsWith("repository") || lowerName.endsWith("dao") || lowerName.endsWith("mapper")) {
            var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.PERSISTENCE_ADAPTER, " persistence adapter", Map.of("matchType", "annotation-or-name"));
            accumulator.addEntity(role, ruleId());
            accumulator.addRelationship(InterpretationSupport.relationship(
                ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-persistence-adapter", entity.sourceRefs(), Map.of("sourceLanguage", "java")
            ), ruleId());
        }
    }

    private void inferEndpoints(ExtractedEntityFact methodEntity, InterpretationContext context, InterpretationAccumulator accumulator) {
        List<String> annotations = InterpretationContext.listMetadata(methodEntity, "annotations");
        String methodAnnotation = endpointAnnotation(annotations).orElse(null);
        if (methodAnnotation == null) {
            return;
        }
        String sourceSnippet = InterpretationContext.primaryRef(methodEntity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(methodEntity).snippet());
        String path = extractPath(sourceSnippet).orElse("/");
        String httpMethod = httpMethodForAnnotation(methodAnnotation);
        var endpoint = InterpretationSupport.endpointEntity(ruleId(), methodEntity, httpMethod, path, Map.of("sourceLanguage", "java"));
        accumulator.addEntity(endpoint, ruleId());

        String pathRef = InterpretationContext.path(methodEntity);
        Integer line = InterpretationContext.line(methodEntity);
        Optional<ExtractedEntityFact> owner = context.nearestClassInFileAboveLine(pathRef, line)
            .filter(candidate -> isController(candidate) || !InterpretationContext.listMetadata(candidate, "annotations").isEmpty());
        if (owner.isEmpty()) {
            owner = context.fileModule(pathRef);
        }
        owner.ifPresent(sourceOwner -> accumulator.addRelationship(
            InterpretationSupport.relationship(ruleId(), RelationshipKind.EXPOSES, sourceOwner.id(), endpoint.id(), httpMethod + " " + path, methodEntity.sourceRefs(), Map.of("sourceLanguage", "java")),
            ruleId()
        ));
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