package info.isaksson.erland.architecturebrowser.indexer.naming;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayNamePolicyTest {
    @Test
    void compactsCommonScopeDisplayNames() {
        assertEquals("order", DisplayNamePolicy.scopeDisplayName(ScopeKind.DIRECTORY, "src/main/java/com/example/order", null));
        assertEquals("OrderService.java", DisplayNamePolicy.scopeDisplayName(ScopeKind.FILE, "src/main/java/com/example/order/OrderService.java", null));
        assertEquals("order", DisplayNamePolicy.scopeDisplayName(ScopeKind.PACKAGE, "com.example.order", "java"));
    }

    @Test
    void compactsCommonEntityDisplayNames() {
        assertEquals("NestedTypesMode", DisplayNamePolicy.entityDisplayName(EntityKind.MODULE, "info.isaksson.erland.javatoxmi.uml.NestedTypesMode", "java"));
        assertEquals("build", DisplayNamePolicy.entityDisplayName(EntityKind.CONFIG_ARTIFACT, ".github/workflows/build.yml#build", "yaml"));
        assertEquals("canRequestHint", DisplayNamePolicy.entityDisplayName(EntityKind.FUNCTION, "src/pages/game/useHintController.ts#canRequestHint", "typescript"));
        assertEquals("OrderService", DisplayNamePolicy.entityDisplayName(EntityKind.CLASS, "com.example.order.OrderService", "java"));
    }
}
