package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
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

class AngularDiGraphRegressionTest {

    @Test
    void angularDiExtractionAddsProviderInjectionAndResolutionRelationships() {
        String source = """
            export const ORDER_API = new InjectionToken<OrderApi>('ORDER_API');

            @Injectable()
            export class OrdersApiService {}

            export function ordersConfigFactory() { return {}; }

            @Component({ providers: [{ provide: ORDER_API, useClass: OrdersApiService }, { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory }] })
            export class OrdersComponent {
              constructor(@Inject(ORDER_API) private api: OrdersApiService) {}
            }
            """;

        StructuralExtractionResult result = extract("src/app/orders.component.ts", source,
            program(source,
                classDeclaration(3, "OrdersApiService", List.of(new SyntaxNode("decorator", true, 0, 0, 3, 0, 3, 13, false, false, "@Injectable()", List.of()))),
                functionDeclaration("ordersConfigFactory", 5, "export function ordersConfigFactory() { return {}; }"),
                new SyntaxNode("class_declaration", true, 0, 0, 7, 0, 10, 1, false, false,
                    """
                    @Component({ providers: [{ provide: ORDER_API, useClass: OrdersApiService }, { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory }] })
                    export class OrdersComponent {
                      constructor(@Inject(ORDER_API) private api: OrdersApiService) {}
                    }
                    """.strip(), List.of(
                        new SyntaxNode("decorator", true, 0, 0, 7, 0, 7, 120, false, false,
                            "@Component({ providers: [{ provide: ORDER_API, useClass: OrdersApiService }, { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory }] })", List.of()),
                        new SyntaxNode("type_identifier", true, 0, 0, 8, 13, 8, 28, false, false, "OrdersComponent", List.of())
                    ))
            ));

        var ordersComponent = entity(result, EntityKind.CLASS, "OrdersComponent");
        var ordersApiService = entity(result, EntityKind.CLASS, "OrdersApiService");
        var orderApi = entity(result, EntityKind.MODULE, "ORDER_API");
        var ordersConfig = entity(result, EntityKind.MODULE, "ORDERS_CONFIG");
        var ordersConfigFactory = entity(result, EntityKind.FUNCTION, "ordersConfigFactory");

        assertAngularDiRelationship(result, ordersComponent.id(), orderApi.id(), "ORDER_API", "injects", true);
        assertAngularDiRelationship(result, orderApi.id(), ordersComponent.id(), "ORDER_API", "providedBy", true);
        assertAngularDiRelationship(result, orderApi.id(), ordersApiService.id(), "OrdersApiService", "resolvesTo", true);
        assertAngularDiRelationship(result, ordersConfig.id(), ordersConfigFactory.id(), "ordersConfigFactory", "resolvesTo", true);
    }

    private static void assertAngularDiRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String frameworkRelationship,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "angular".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("angular:" + frameworkRelationship, relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromAngularDiExtraction"));
    }

    private static StructuralExtractionResult extract(String relativePath, String source, SyntaxNode root) {
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
        return new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.SUCCESS, 1)));
    }

    private static SyntaxNode program(String source, SyntaxNode... children) {
        int endLine = Math.max(0, source.split("\\R", -1).length - 1);
        int endColumn = source.isEmpty() ? 0 : source.length() - source.lastIndexOf('\n') - 1;
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, endLine, endColumn, false, false, source, List.of(children));
    }

    private static SyntaxNode functionDeclaration(String name, int line, String snippet) {
        return new SyntaxNode("function_declaration", true, 0, 0, line, 0, line, Math.max(snippet.length(), name.length()), false, false,
            snippet, List.of(
                new SyntaxNode("identifier", true, 0, 0, line, 16, line, 16 + name.length(), false, false, name, List.of())
            ));
    }

    private static SyntaxNode classDeclaration(int line, String name, List<SyntaxNode> extraChildren) {
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.add(new SyntaxNode("type_identifier", true, 0, 0, line, 13, line, 13 + name.length(), false, false, name, List.of()));
        children.addAll(extraChildren);
        return new SyntaxNode("class_declaration", true, 0, 0, line, 0, line, 13 + name.length(), false, false,
            "export class " + name + " {}", List.copyOf(children));
    }

    private static info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .findFirst()
            .orElseThrow();
    }
}
