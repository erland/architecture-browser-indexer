package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRoleInterpretationRegressionTest extends AbstractFrontendFrameworkBaselineTestSupport {
    @Test
    void currentAngularAndReactRoleInterpretationRemainsStable() {
        ArchitectureIndexDocument document = buildRoleInterpretationDocument();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.SERVICE && "OrderService".equals(entity.name()) && "angular-injectable".equals(entity.metadata().get("serviceProfile"))));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "OrderListComponent".equals(entity.name()) && "angular-component".equals(entity.metadata().get("uiProfile"))));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "OrdersModule".equals(entity.name()) && "angular-module".equals(entity.metadata().get("uiProfile"))));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "UserCard".equals(entity.name()) && "react-function-component".equals(entity.metadata().get("uiProfile"))));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "AuthProvider".equals(entity.name()) && "react-context".equals(entity.metadata().get("uiProfile"))));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.UI_MODULE && "OrdersPage".equals(entity.name()) && "page-or-router".equals(entity.metadata().get("uiProfile"))));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.CLASS && entity.origin() == EntityOrigin.OBSERVED && "OrderListComponent".equals(entity.name())));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && entity.origin() == EntityOrigin.OBSERVED && "UserCard".equals(entity.name())));
    }
}
