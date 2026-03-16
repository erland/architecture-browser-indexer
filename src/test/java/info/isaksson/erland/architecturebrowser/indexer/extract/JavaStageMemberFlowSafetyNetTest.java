package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classByQualifiedName;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.importDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodByOwner;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStageMemberFlowSafetyNetTest {

    @Test
    void freezesFieldAndMethodExtractionContainmentAndOwnerMetadata() {
        String source = """
            package com.example.orders;
            import java.util.List;
            import com.example.shared.RequestContext;

            class OrderService {
                private List<RequestContext> contexts;

                public RequestContext load(RequestContext request) {
                    return request;
                }
            }
            """;

        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderService.java",
            source,
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            importDecl(1, "import java.util.List;"),
            importDecl(2, "import com.example.shared.RequestContext;"),
            classDecl(4, "OrderService", "class OrderService { ... }",
                fieldDecl(5, "private List<RequestContext> contexts;", "List<RequestContext>", "contexts"),
                methodDecl(7, "public RequestContext load(RequestContext request) { return request; }", "RequestContext", "load", "(RequestContext request)")
            )
        );

        ExtractedEntityFact orderService = classByQualifiedName(accumulator, "com.example.orders.OrderService");
        ExtractedEntityFact contexts = fieldByOwner(accumulator, "com.example.orders.OrderService", "contexts");
        ExtractedEntityFact load = methodByOwner(accumulator, "com.example.orders.OrderService", "load");

        assertEquals("List<RequestContext>", contexts.metadata().get("declaredType"));
        assertEquals("com.example.orders.OrderService", contexts.metadata().get("ownerQualifiedName"));
        assertEquals(List.of("RequestContext"), load.metadata().get("parameterTypes"));
        assertEquals("RequestContext", load.metadata().get("returnType"));
        assertEquals("com.example.orders.OrderService", load.metadata().get("ownerQualifiedName"));

        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && orderService.id().equals(rel.fromEntityId())
            && contexts.id().equals(rel.toEntityId())));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.CONTAINS
            && orderService.id().equals(rel.fromEntityId())
            && load.id().equals(rel.toEntityId())));
    }
}
