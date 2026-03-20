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

class JavaJpaStructuralExtractionTest {

    @Test
    void extractsJpaEntityMetadataEmbeddedValuesAndAssociations() {
        String source = """
            package com.example.orders.domain;

            import jakarta.persistence.Column;
            import jakarta.persistence.Embeddable;
            import jakarta.persistence.Embedded;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.JoinColumn;
            import jakarta.persistence.ManyToOne;
            import jakarta.persistence.Table;
            import jakarta.persistence.Version;

            @Embeddable
            class AddressValue {
                @Column(name = \"street_name\")
                String street;
            }

            @Entity
            @Table(name = \"customers\")
            class CustomerEntity {
                @Id
                String id;
            }

            @Entity
            @Table(name = \"orders\")
            class OrderEntity {
                @Id
                String id;

                @Version
                long version;

                @Embedded
                AddressValue shippingAddress;

                @ManyToOne
                @JoinColumn(name = "customer_id", nullable = false)
                CustomerEntity customer;
            }
            """;

        SyntaxNode root = program(source,
            packageDecl(0, "package com.example.orders.domain;"),
            importDecl(1, "import jakarta.persistence.Column;"),
            importDecl(2, "import jakarta.persistence.Embeddable;"),
            importDecl(3, "import jakarta.persistence.Embedded;"),
            importDecl(4, "import jakarta.persistence.Entity;"),
            importDecl(5, "import jakarta.persistence.Id;"),
            importDecl(6, "import jakarta.persistence.JoinColumn;"),
            importDecl(7, "import jakarta.persistence.ManyToOne;"),
            importDecl(8, "import jakarta.persistence.Table;"),
            importDecl(9, "import jakarta.persistence.Version;"),
            classDecl(11, "AddressValue", """
                @Embeddable
                class AddressValue {
                    @Column(name = "street_name")
                    String street;
                }
                """,
                annotation(11, "@Embeddable"),
                fieldDecl(13, "@Column(name = \"street_name\") String street;", "street", annotation(13, "@Column(name = \"street_name\")"))
            ),
            classDecl(17, "CustomerEntity", """
                @Entity
                @Table(name = "customers")
                class CustomerEntity {
                    @Id
                    String id;
                }
                """,
                annotation(17, "@Entity"),
                annotation(18, "@Table(name = \"customers\")"),
                fieldDecl(20, "@Id String id;", "id", annotation(20, "@Id"))
            ),
            classDecl(24, "OrderEntity", """
                @Entity
                @Table(name = "orders")
                class OrderEntity {
                    @Id
                    String id;

                    @Version
                    long version;

                    @Embedded
                    AddressValue shippingAddress;

                    @ManyToOne
                    @JoinColumn(name = "customer_id", nullable = false)
                    CustomerEntity customer;
                }
                """,
                annotation(24, "@Entity"),
                annotation(25, "@Table(name = \"orders\")"),
                fieldDecl(27, "@Id String id;", "id", annotation(27, "@Id")),
                fieldDecl(30, "@Version long version;", "version", annotation(30, "@Version")),
                fieldDecl(33, "@Embedded AddressValue shippingAddress;", "shippingAddress", annotation(33, "@Embedded")),
                fieldDecl(36, "@ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer;", "customer",
                    annotation(36, "@ManyToOne"),
                    annotation(36, "@JoinColumn(name = \"customer_id\", nullable = false)")
                )
            )
        );

        StructuralExtractionResult result = extract("src/main/java/com/example/orders/domain/OrderEntity.java", source, root);

        var orderEntity = entityByQualifiedName(result, "com.example.orders.domain.OrderEntity");
        assertEquals(Boolean.TRUE, orderEntity.metadata().get("jpaEntity"));
        assertEquals("orders", orderEntity.metadata().get("tableName"));
        assertEquals("entity", orderEntity.metadata().get("jpaKind"));

        var customerEntity = entityByQualifiedName(result, "com.example.orders.domain.CustomerEntity");
        assertEquals(Boolean.TRUE, customerEntity.metadata().get("jpaEntity"));
        assertEquals("customers", customerEntity.metadata().get("tableName"));

        var addressValue = entityByQualifiedName(result, "com.example.orders.domain.AddressValue");
        assertEquals(Boolean.TRUE, addressValue.metadata().get("jpaEmbeddable"));
        assertEquals("embeddable", addressValue.metadata().get("jpaKind"));

        var idField = field(result, "com.example.orders.domain.OrderEntity", "id");
        assertEquals(Boolean.TRUE, idField.metadata().get("jpaId"));

        var versionField = field(result, "com.example.orders.domain.OrderEntity", "version");
        assertEquals(Boolean.TRUE, versionField.metadata().get("jpaVersion"));

        var embeddedField = field(result, "com.example.orders.domain.OrderEntity", "shippingAddress");
        assertEquals(Boolean.TRUE, embeddedField.metadata().get("jpaEmbedded"));

        var associationField = field(result, "com.example.orders.domain.OrderEntity", "customer");
        assertEquals("many-to-one", associationField.metadata().get("jpaAssociation"));
        assertEquals("customer_id", associationField.metadata().get("joinColumn"));
        assertEquals(Boolean.FALSE, associationField.metadata().get("nullable"));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(orderEntity.id())
            && rel.toEntityId().equals(customerEntity.id())
            && "hasAssociation".equals(rel.metadata().get("relationshipType"))
            && "association".equals(rel.metadata().get("associationKind"))
            && "many-to-one".equals(rel.metadata().get("associationCardinality"))
            && "many-to-one".equals(rel.metadata().get("jpaAssociation"))
            && "0".equals(rel.metadata().get("sourceLowerBound"))
            && "*".equals(rel.metadata().get("sourceUpperBound"))
            && "1".equals(rel.metadata().get("targetLowerBound"))
            && "1".equals(rel.metadata().get("targetUpperBound"))));

        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(orderEntity.id())
            && rel.toEntityId().equals(addressValue.id())
            && "embeds".equals(rel.metadata().get("relationshipType"))));
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
