package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AngularDependencyInjectionModularizationTest {
    @Test
    void constructorParameterDiscoveryAndReferenceParsingRemainAvailableThroughFocusedHelpers() {
        List<String> blocks = AngularDependencyInjectionReferenceSupport.extractConstructorParameterBlocks(
            "export class OrdersComponent { constructor(@Inject(ORDER_API) private api: OrdersApiService, private facade: OrdersFacade) {} }"
        );

        assertEquals(1, blocks.size());
        AngularInjectionReference tokenReference = AngularDependencyInjectionReferenceSupport.parseInjectionReference("@Inject(ORDER_API) private api: OrdersApiService");
        AngularInjectionReference typeReference = AngularDependencyInjectionReferenceSupport.parseInjectionReference("private facade: OrdersFacade");

        assertNotNull(tokenReference);
        assertEquals("ORDER_API", tokenReference.targetName());
        assertEquals(EntityKind.MODULE, tokenReference.kind());

        assertNotNull(typeReference);
        assertEquals("OrdersFacade", typeReference.targetName());
        assertEquals(EntityKind.CLASS, typeReference.kind());
        assertTrue(blocks.get(0).contains("OrdersFacade"));
    }
}
