package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaSyntaxTreeTraversalTest {

    @Test
    void traversalPropagatesUpdatedOwnershipToChildNodes() {
        SyntaxNode method = new SyntaxNode("method_declaration", true, 20, 40, 3, 1, 3, 20, false, false, "void handle() {}", List.of());
        SyntaxNode field = new SyntaxNode("field_declaration", true, 10, 19, 2, 1, 2, 19, false, false, "OrderService service;", List.of());
        SyntaxNode type = new SyntaxNode("class_declaration", true, 0, 50, 1, 1, 4, 1, false, false, "class Orders {}", List.of(field, method));

        JavaSyntaxTreeTraversal traversal = new JavaSyntaxTreeTraversal();
        List<String> ownershipSeen = new ArrayList<>();

        traversal.traverse(
            type,
            new JavaSyntaxTreeTraversal.JavaTraversalOwnership(null, null, null),
            (node, ownership) -> {
                ownershipSeen.add(node.type() + ":" + (ownership == null ? "<none>" : String.valueOf(ownership.owningQualifiedName())));
                if ("class_declaration".equals(node.type())) {
                    return new JavaSyntaxTreeTraversal.JavaTraversalOwnership("type:orders", "com.example.Orders", node.textSnippet());
                }
                return ownership;
            }
        );

        assertEquals(
            List.of(
                "class_declaration:null",
                "field_declaration:com.example.Orders",
                "method_declaration:com.example.Orders"
            ),
            ownershipSeen
        );
    }
}
