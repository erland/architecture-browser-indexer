package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaJpaPropertyAccessStructuralExtractionTest {

    @Test
    void extractsJpaPropertyAccessFromGetterMethods() {
        String source = """
            package com.example.orders.domain;

            import jakarta.persistence.Embedded;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.JoinColumn;
            import jakarta.persistence.ManyToOne;

            class AddressValue {}
            class CustomerEntity {}

            @Entity
            class OrderEntity {
                private String id;
                private AddressValue shippingAddress;
                private CustomerEntity customer;

                @Id
                public String getId() { return id; }

                @Embedded
                public AddressValue getShippingAddress() { return shippingAddress; }

                @ManyToOne
                @JoinColumn(name = "customer_id", nullable = false)
                public CustomerEntity getCustomer() { return customer; }
            }
            """;

        SyntaxNode root = program(source,
            packageDecl(0, "package com.example.orders.domain;"),
            importDecl(1, "import jakarta.persistence.Embedded;"),
            importDecl(2, "import jakarta.persistence.Entity;"),
            importDecl(3, "import jakarta.persistence.Id;"),
            importDecl(4, "import jakarta.persistence.JoinColumn;"),
            importDecl(5, "import jakarta.persistence.ManyToOne;"),
            classDecl(7, "AddressValue", "class AddressValue {}"),
            classDecl(8, "CustomerEntity", "class CustomerEntity {}"),
            classDecl(11, "OrderEntity", """
                @Entity
                class OrderEntity {
                    private String id;
                    private AddressValue shippingAddress;
                    private CustomerEntity customer;

                    @Id
                    public String getId() { return id; }

                    @Embedded
                    public AddressValue getShippingAddress() { return shippingAddress; }

                    @ManyToOne
                    @JoinColumn(name = "customer_id", nullable = false)
                    public CustomerEntity getCustomer() { return customer; }
                }
                """,
                annotation(11, "@Entity"),
                fieldDecl(13, "private String id;", "id"),
                fieldDecl(14, "private AddressValue shippingAddress;", "shippingAddress"),
                fieldDecl(15, "private CustomerEntity customer;", "customer"),
                methodDecl(18, "@Id public String getId() { return id; }", "getId", "()", annotation(18, "@Id")),
                methodDecl(21, "@Embedded public AddressValue getShippingAddress() { return shippingAddress; }", "getShippingAddress", "()", annotation(21, "@Embedded")),
                methodDecl(24, "@ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) public CustomerEntity getCustomer() { return customer; }", "getCustomer", "()",
                    annotation(24, "@ManyToOne"),
                    annotation(24, "@JoinColumn(name = \"customer_id\", nullable = false)")
                )
            )
        );

        StructuralExtractionResult result = extract("src/main/java/com/example/orders/domain/OrderEntity.java", source, root);

        var orderEntity = entityByQualifiedName(result, "com.example.orders.domain.OrderEntity");
        var idGetter = method(result, "com.example.orders.domain.OrderEntity", "getId");
        var embeddedGetter = method(result, "com.example.orders.domain.OrderEntity", "getShippingAddress");
        var associationGetter = method(result, "com.example.orders.domain.OrderEntity", "getCustomer");

        assertNotNull(idGetter, "Expected getId() method to be extracted");
        assertNotNull(embeddedGetter, "Expected getShippingAddress() method to be extracted");
        assertNotNull(associationGetter, "Expected getCustomer() method to be extracted");

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(orderEntity.id())
            && "com.example.orders.domain.AddressValue".equals(rel.label())
            && "embeds".equals(rel.metadata().get("relationshipType"))
            && "method".equals(rel.metadata().get("ownerMemberKind"))
            && "shippingAddress".equals(rel.metadata().get("ownerPropertyName"))),
            () -> "Expected method-based embeds relationship. Relationships=" + result.relationships());

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(orderEntity.id())
            && "com.example.orders.domain.CustomerEntity".equals(rel.label())
            && "hasAssociation".equals(rel.metadata().get("relationshipType"))
            && "many-to-one".equals(rel.metadata().get("jpaAssociation"))
            && "method".equals(rel.metadata().get("ownerMemberKind"))
            && "customer".equals(rel.metadata().get("ownerPropertyName"))),
            () -> "Expected method-based association relationship. Relationships=" + result.relationships());
    }



    private static StructuralExtractionResult extract(String relativePath, String source, SyntaxNode root) {
        SourceParseRequest request = new SourceParseRequest(
            Path.of(relativePath),
            relativePath,
            ParseLanguage.JAVA,
            source
        );
        SourceParseResult parseResult = new SourceParseResult(
            request,
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
        return new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.JAVA, 1), Map.of(ParseStatus.SUCCESS, 1)));
    }

    private static ExtractedEntityFact entityByQualifiedName(StructuralExtractionResult result, String qualifiedName) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS)
            .filter(entity -> qualifiedName.equals(entity.metadata().get("qualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static ExtractedEntityFact field(StructuralExtractionResult result, String ownerQualifiedName, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FIELD)
            .filter(entity -> name.equals(entity.name()))
            .filter(entity -> ownerQualifiedName.equals(entity.metadata().get("ownerQualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static ExtractedEntityFact method(StructuralExtractionResult result, String ownerQualifiedName, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION)
            .filter(entity -> name.equals(entity.name()))
            .filter(entity -> ownerQualifiedName.equals(entity.metadata().get("ownerQualifiedName")))
            .findFirst()
            .orElseThrow();
    }

    private static SyntaxNode program(String source, SyntaxNode... children) {
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, 0, 0, false, false, source, List.of(children));
    }

    private static SyntaxNode packageDecl(int line, String snippet) {
        return new SyntaxNode("package_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode importDecl(int line, String snippet) {
        return new SyntaxNode("import_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode annotation(int line, String snippet) {
        return new SyntaxNode("marker_annotation", true, 0, 0, line, 0, line, 0, false, false, snippet, List.of());
    }

    private static SyntaxNode classDecl(int line, String name, String snippet, SyntaxNode... members) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.add(new SyntaxNode("type_identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.addAll(List.of(members));
        return new SyntaxNode("class_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode fieldDecl(int line, String snippet, String name, SyntaxNode... annotations) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.addAll(List.of(annotations));
        children.add(new SyntaxNode("variable_declarator", true, 0, 0, line, 0, line, 0, false, false, name, List.of(
            new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of())
        )));
        return new SyntaxNode("field_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

    private static SyntaxNode methodDecl(int line, String snippet, String name, String parameters, SyntaxNode... annotations) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.addAll(List.of(annotations));
        children.add(new SyntaxNode("identifier", true, 0, 0, line, 0, line, 0, false, false, name, List.of()));
        children.add(new SyntaxNode("formal_parameters", true, 0, 0, line, 0, line, 0, false, false, parameters, List.of()));
        return new SyntaxNode("method_declaration", true, 0, 0, line, 0, line, 0, false, false, snippet, List.copyOf(children));
    }

}
