package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.Map;

final class TypeScriptFrontendInterpretationRule implements InterpretationRule {
    private final TypeScriptFrontendClassifier classifier = new TypeScriptFrontendClassifier();

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
        String profile = classifier.classifyUiProfile(entity);
        if (profile == null) {
            return;
        }
        var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.UI_MODULE, " ui module", Map.of(
            "matchType", profile,
            "uiProfile", profile,
            "sourceLanguage", "typescript"
        ));
        accumulator.addEntity(role, ruleId());
        accumulator.addRelationship(InterpretationSupport.relationship(
            ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-ui-module", entity.sourceRefs(), Map.of(
                "sourceLanguage", "typescript",
                "uiProfile", profile
            )
        ), ruleId());
    }

    private void inferService(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        String profile = classifier.classifyServiceProfile(entity);
        if (profile == null) {
            return;
        }
        var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.SERVICE, " service", Map.of(
            "matchType", profile,
            "serviceProfile", profile,
            "sourceLanguage", "typescript"
        ));
        accumulator.addEntity(role, ruleId());
        accumulator.addRelationship(InterpretationSupport.relationship(
            ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-service", entity.sourceRefs(), Map.of(
                "sourceLanguage", "typescript",
                "serviceProfile", profile
            )
        ), ruleId());
    }

    private void inferStartupPoint(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        if (!classifier.isStartupPoint(entity)) {
            return;
        }
        var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.STARTUP_POINT, " startup point", Map.of("matchType", classifier.startupMatchType(entity)));
        accumulator.addEntity(role, ruleId());
        accumulator.addRelationship(InterpretationSupport.relationship(
            ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-startup-point", entity.sourceRefs(), Map.of("sourceLanguage", "typescript")
        ), ruleId());
    }
}
