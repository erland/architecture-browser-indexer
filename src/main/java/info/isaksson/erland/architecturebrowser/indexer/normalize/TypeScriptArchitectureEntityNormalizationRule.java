package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Conservative TypeScript/Angular/React normalization mapping.
 *
 * <p>This rule now delegates framework-specific heuristics to focused TypeScript helpers so the
 * normalization stage owns architectural meaning while extraction can stay factual.</p>
 */
final class TypeScriptArchitectureEntityNormalizationRule implements ArchitectureEntityNormalizationRule {
    @Override
    public NormalizedArchitectureEntity normalize(ArchitectureEntityNormalizationContext context) {
        ArchitectureEntity entity = context.entity();
        if (entity == null || !TypeScriptArchitectureMetadataSupport.isTypeScriptBacked(entity, context.entitiesById())) {
            return null;
        }

        List<String> roles = new ArrayList<>();
        List<String> traits = new ArrayList<>();

        if (TypeScriptArchitectureEntitySemanticsSupport.isApiEntrypoint(entity, context.entitiesById())) {
            addIfMissing(roles, ArchitecturalRole.API_ENTRYPOINT.id());
            addIfMissing(traits, ArchitecturalTrait.EXTERNALLY_EXPOSED.id());
        }
        if (TypeScriptArchitectureEntitySemanticsSupport.isUiPage(entity, context)) {
            addIfMissing(roles, ArchitecturalRole.UI_PAGE.id());
            addIfMissing(traits, ArchitecturalTrait.USER_FACING.id());
            if (context.frontendRouteEvidenceOptional().filter(FrontendRouteEvidence::declaredRoute).isPresent()) {
                addIfMissing(traits, ArchitecturalTrait.ROUTE_DECLARED.id());
            }
        }
        if (TypeScriptArchitectureEntitySemanticsSupport.isUiLayout(entity, context)) {
            addIfMissing(roles, ArchitecturalRole.UI_LAYOUT.id());
            addIfMissing(traits, ArchitecturalTrait.USER_FACING.id());
            if (context.frontendRouteEvidenceOptional().filter(FrontendRouteEvidence::declaredRoute).isPresent()) {
                addIfMissing(traits, ArchitecturalTrait.ROUTE_DECLARED.id());
            }
        }
        if (TypeScriptArchitectureEntitySemanticsSupport.isUiNavigationNode(entity, context)) {
            addIfMissing(roles, ArchitecturalRole.UI_NAVIGATION_NODE.id());
            addIfMissing(traits, ArchitecturalTrait.USER_FACING.id());
        }
        if (TypeScriptArchitectureEntitySemanticsSupport.isApplicationService(entity, context.entitiesById())) {
            addIfMissing(roles, ArchitecturalRole.APPLICATION_SERVICE.id());
        }
        if (TypeScriptArchitectureEntitySemanticsSupport.isIntegrationAdapter(entity, context.entitiesById())) {
            addIfMissing(roles, ArchitecturalRole.INTEGRATION_ADAPTER.id());
        }
        if (TypeScriptArchitectureEntitySemanticsSupport.isConfigurationProvider(entity, context.entitiesById())) {
            addIfMissing(roles, ArchitecturalRole.CONFIGURATION_PROVIDER.id());
            addIfMissing(traits, ArchitecturalTrait.CONFIGURATION_DRIVEN.id());
        }
        if (TypeScriptArchitectureEntitySemanticsSupport.isFrameworkManaged(entity)) {
            addIfMissing(traits, ArchitecturalTrait.FRAMEWORK_MANAGED.id());
        }

        if (roles.isEmpty() && traits.isEmpty()) {
            return null;
        }
        return new NormalizedArchitectureEntity(
            roles.isEmpty() ? null : roles,
            traits.isEmpty() ? null : traits
        );
    }

    private static void addIfMissing(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }
}
