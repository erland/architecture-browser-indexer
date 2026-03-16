package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classByQualifiedName;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.classDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.extract;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.fieldDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.importDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.methodDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.packageDecl;
import static info.isaksson.erland.architecturebrowser.indexer.extract.JavaSyntaxTreeExtractionStageTestSupport.typeIdentifier;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaStageDependencyFlowSafetyNetTest {

    @Test
    void freezesDeclaredTypeDependencyEmissionForHierarchyFieldsReturnsAndParameters() {
        String source = """
            package com.example.orders;
            import com.example.shared.RequestContext;

            class BaseService {}
            class OrderService extends BaseService {
                private RequestContext context;
                RequestContext load(RequestContext request) { return request; }
            }
            """;

        ExtractionAccumulator accumulator = extract(
            "src/main/java/com/example/orders/OrderService.java",
            source,
            packageDecl(0, "package com.example.orders;", "com.example.orders"),
            importDecl(1, "import com.example.shared.RequestContext;"),
            classDecl(3, "BaseService", "class BaseService {}"),
            classDecl(4, "OrderService", "class OrderService extends BaseService { ... }",
                typeIdentifier(4, "BaseService"),
                fieldDecl(5, "private RequestContext context;", "RequestContext", "context"),
                methodDecl(6, "RequestContext load(RequestContext request) { return request; }", "RequestContext", "load", "(RequestContext request)")
            )
        );

        ExtractedEntityFact orderService = classByQualifiedName(accumulator, "com.example.orders.OrderService");

        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.EXTENDS
            && orderService.id().equals(rel.fromEntityId())
            && "extends".equals(rel.metadata().get("dependencySource"))));
        long declaredTypeDependencies = accumulator.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON)
            .filter(rel -> orderService.id().equals(rel.fromEntityId()))
            .filter(rel -> rel.metadata().containsKey("dependencySource"))
            .count();
        assertTrue(declaredTypeDependencies >= 3);
    }
}
