package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaOwnerContextContractsTest {

    @Test
    void ownerContextRoundTripsThroughTraversalOwnershipAndResults() {
        JavaOwnerContext owner = new JavaOwnerContext("entity:type:OrderResource", "com.example.orders.OrderResource", "class OrderResource {} ");

        JavaSyntaxTreeTraversal.JavaTraversalOwnership traversalOwnership = owner.toTraversalOwnership();
        JavaOwnerContext roundTripped = JavaOwnerContext.fromTraversalOwnership(traversalOwnership);
        JavaTypeTraversalResult handled = JavaTypeTraversalResult.handled(roundTripped);
        JavaTypeTraversalResult notHandled = JavaTypeTraversalResult.notHandled(JavaOwnerContext.root());

        assertEquals(owner.owningTypeEntityId(), roundTripped.owningTypeEntityId());
        assertEquals(owner.owningQualifiedName(), handled.owningQualifiedName());
        assertEquals(owner.owningTypeSnippet(), handled.owningTypeSnippet());
        assertTrue(handled.handled());
        assertFalse(notHandled.handled());
        assertNull(notHandled.owningTypeEntityId());
    }
}
