package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaExtractionSupportTest {

    @Test
    void supportHelpersProvideStableParameterOwnershipAndSourceReferenceBehavior() {
        assertEquals(List.of("customerId", "request"), JavaDeclaredTypeSupport.extractParameterNames("(String customerId, CreateOrderRequest request)"));
        assertEquals(List.of("x", "7"), JavaDeclaredTypeSupport.metadataStringList(List.of("x", 7)));
        assertEquals("type:order-service", JavaOwnershipSupport.dependencySourceEntityId(new JavaOwnerContext("type:order-service", "com.example.OrderService", "class OrderService {}"), "file:orders"));
        assertEquals("file:orders", JavaOwnershipSupport.dependencySourceEntityId(JavaOwnerContext.root(), "file:orders"));

        SyntaxNode node = new SyntaxNode("method_declaration", true, 5, 1, 5, 20, 40, 65, false, false, "void create() {}", List.of());
        SourceReference ref = JavaSourceReferenceSupport.primaryReference("src/main/java/com/example/OrderService.java", node, "method_declaration", List.of(), "void create() {}\n");
        assertEquals(6, JavaSourceReferenceSupport.lineOf(ref, node));
        assertEquals("method_declaration", String.valueOf(ref.metadata().get("kind")));
    }
}
