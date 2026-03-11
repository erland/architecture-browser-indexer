package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.List;
import java.util.Map;

final class InterpretationSupport {
    private InterpretationSupport() {
    }

    static InterpretedEntityFact roleEntity(String ruleId, ExtractedEntityFact sourceEntity, EntityKind roleKind, String displaySuffix, Map<String, Object> extraMetadata) {
        return new InterpretedEntityFact(
            IdUtils.externalEntityId("interpret-" + roleKind.name().toLowerCase(), sourceEntity.id()),
            roleKind,
            EntityOrigin.INFERRED,
            sourceEntity.name(),
            sourceEntity.displayName() + displaySuffix,
            sourceEntity.scopeId(),
            sourceEntity.sourceRefs(),
            merge(
                Map.of(
                    "sourceEntityId", sourceEntity.id(),
                    "ruleId", ruleId,
                    "interpretedKind", roleKind.name()
                ),
                extraMetadata
            )
        );
    }

    static InterpretedEntityFact endpointEntity(String ruleId, ExtractedEntityFact sourceEntity, String httpMethod, String path, Map<String, Object> extraMetadata) {
        String stable = sourceEntity.id() + ":" + httpMethod + ":" + path;
        return new InterpretedEntityFact(
            IdUtils.externalEntityId("interpret-endpoint", stable),
            EntityKind.ENDPOINT,
            EntityOrigin.INFERRED,
            httpMethod + " " + path,
            sourceEntity.displayName() + " endpoint " + httpMethod + " " + path,
            sourceEntity.scopeId(),
            sourceEntity.sourceRefs(),
            merge(
                Map.of(
                    "sourceEntityId", sourceEntity.id(),
                    "ruleId", ruleId,
                    "httpMethod", httpMethod,
                    "path", path
                ),
                extraMetadata
            )
        );
    }

    static InterpretedRelationshipFact relationship(String ruleId, RelationshipKind kind, String fromId, String toId, String label, List<SourceReference> refs, Map<String, Object> metadata) {
        return new InterpretedRelationshipFact(
            IdUtils.relationshipId("interpret-" + kind.name().toLowerCase(), fromId, toId, label),
            kind,
            fromId,
            toId,
            label,
            refs == null ? List.of() : refs,
            merge(Map.of("ruleId", ruleId), metadata)
        );
    }

    static Diagnostic interpretationWarning(String code, String message, String path, List<SourceReference> refs) {
        return new Diagnostic(
            "diag:interpret:" + IdUtils.stableToken(code + ":" + path + ":" + message),
            DiagnosticSeverity.WARNING,
            DiagnosticPhase.INTERPRETATION,
            code,
            message,
            false,
            path,
            null,
            null,
            refs == null ? List.of() : refs,
            Map.of()
        );
    }

    private static Map<String, Object> merge(Map<String, Object> left, Map<String, Object> right) {
        java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>(left);
        if (right != null) {
            merged.putAll(right);
        }
        return Map.copyOf(merged);
    }
}
