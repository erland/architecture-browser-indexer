package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.List;
import java.util.Locale;

final class TypeScriptFrontendInterpretationRule implements InterpretationRule {
    @Override
    public String ruleId() {
        return "typescript-frontend-high-value";
    }

    @Override
    public void apply(InterpretationContext context, InterpretationAccumulator accumulator) {
        for (ExtractedEntityFact entity : context.entitiesByLanguage("typescript")) {
            inferUiModule(entity, accumulator);
            inferService(entity, accumulator);
            inferStartupPoint(entity, accumulator);
        }
    }

    private void inferUiModule(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        List<String> decorators = InterpretationContext.listMetadata(entity, "decorators");
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        if (decorators.stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(value -> value.endsWith("component") || value.endsWith("ngmodule"))
            || lowerName.endsWith("component") || lowerName.endsWith("module")) {
            var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.UI_MODULE, " ui module", java.util.Map.of("matchType", "decorator-or-name"));
            accumulator.addEntity(role, ruleId());
            accumulator.addRelationship(InterpretationSupport.relationship(
                ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-ui-module", entity.sourceRefs(), java.util.Map.of("sourceLanguage", "typescript")
            ), ruleId());
        }
    }

    private void inferService(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        List<String> decorators = InterpretationContext.listMetadata(entity, "decorators");
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        if (decorators.stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(value -> value.endsWith("injectable"))
            || lowerName.endsWith("service") || lowerName.endsWith("api") || lowerName.endsWith("client")) {
            var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.SERVICE, " service", java.util.Map.of("matchType", "decorator-or-name"));
            accumulator.addEntity(role, ruleId());
            accumulator.addRelationship(InterpretationSupport.relationship(
                ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-service", entity.sourceRefs(), java.util.Map.of("sourceLanguage", "typescript")
            ), ruleId());
        }
    }

    private void inferStartupPoint(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String path = InterpretationContext.path(entity);
        boolean startupByName = lowerName.equals("main") || lowerName.contains("bootstrap") || lowerName.equals("renderapp") || lowerName.equals("startup");
        boolean startupByPath = path != null && (path.endsWith("/main.ts") || path.endsWith("/main.tsx") || path.endsWith("/index.tsx") || path.endsWith("/bootstrap.ts"));
        if (startupByName || startupByPath) {
            var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.STARTUP_POINT, " startup point", java.util.Map.of("matchType", startupByName ? "name" : "path"));
            accumulator.addEntity(role, ruleId());
            accumulator.addRelationship(InterpretationSupport.relationship(
                ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-startup-point", entity.sourceRefs(), java.util.Map.of("sourceLanguage", "typescript")
            ), ruleId());
        }
    }
}
