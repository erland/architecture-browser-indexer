package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendUiEntityNormalizationTest {

    private final ArchitectureEntityNormalizationService entityService = ArchitectureEntityNormalizationService.defaultService();

    @Test
    void preservesExistingRolesWhenUiPageRoleIsAdded() {
        ArchitectureEntity uiModule = new ArchitectureEntity(
            "entity:ts:orders-page-ui",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "OrdersPage",
            "OrdersPage ui module",
            "scope:repo",
            List.of(),
            Map.of(
                "sourceLanguage", "typescript",
                "uiProfile", "page-or-router"
            )
        );

        ArchitectureEntity normalized = entityService.normalizeEntity(uiModule, Map.of(uiModule.id(), uiModule));

        assertTrue(normalized.architecturalRoles().contains("api-entrypoint"));
        assertTrue(normalized.architecturalRoles().contains("ui-page"));
        assertTrue(normalized.architecturalTraits().contains("user-facing"));
    }
}
