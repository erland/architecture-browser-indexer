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
import static org.junit.jupiter.api.Assertions.assertTrue;

class AngularFrameworkRelationshipRegressionTest {

    @Test
    void angularFrameworkRelationshipsRemainStableForModuleStandaloneAndProvidedInScenarios() {
        String source = """
            @Component({ standalone: true, imports: [SharedCardComponent], providers: [OrderFacade] })
            export class OrdersComponent {}

            @Directive({ standalone: true })
            export class SharedCardComponent {}

            @NgModule({ declarations: [OrdersComponent], imports: [SharedModule], exports: [OrdersComponent], bootstrap: [OrdersComponent], providers: [OrderFacade] })
            export class OrdersModule {}

            @Injectable({ providedIn: 'root' })
            export class OrdersService {}

            export class SharedModule {}
            export class OrderFacade {}
            """;

        SyntaxNode ordersComponent = new SyntaxNode("class_declaration", true, 0, 122, 0, 0, 1, 31, false, false,
            """
            @Component({ standalone: true, imports: [SharedCardComponent], providers: [OrderFacade] })
            export class OrdersComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 91, 0, 0, 0, 91, false, false,
                    "@Component({ standalone: true, imports: [SharedCardComponent], providers: [OrderFacade] })", List.of()),
                new SyntaxNode("type_identifier", true, 105, 120, 1, 13, 1, 28, false, false, "OrdersComponent", List.of())
            ));
        SyntaxNode sharedCardComponent = new SyntaxNode("class_declaration", true, 124, 202, 3, 0, 4, 35, false, false,
            """
            @Directive({ standalone: true })
            export class SharedCardComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 124, 155, 3, 0, 3, 31, false, false,
                    "@Directive({ standalone: true })", List.of()),
                new SyntaxNode("type_identifier", true, 169, 188, 4, 13, 4, 32, false, false, "SharedCardComponent", List.of())
            ));
        SyntaxNode ordersModule = new SyntaxNode("class_declaration", true, 204, 390, 6, 0, 7, 28, false, false,
            """
            @NgModule({ declarations: [OrdersComponent], imports: [SharedModule], exports: [OrdersComponent], bootstrap: [OrdersComponent], providers: [OrderFacade] })
            export class OrdersModule {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 204, 360, 6, 0, 6, 156, false, false,
                    "@NgModule({ declarations: [OrdersComponent], imports: [SharedModule], exports: [OrdersComponent], bootstrap: [OrdersComponent], providers: [OrderFacade] })", List.of()),
                new SyntaxNode("type_identifier", true, 374, 386, 7, 13, 7, 25, false, false, "OrdersModule", List.of())
            ));
        SyntaxNode ordersService = new SyntaxNode("class_declaration", true, 392, 462, 9, 0, 10, 29, false, false,
            """
            @Injectable({ providedIn: 'root' })
            export class OrdersService {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 392, 427, 9, 0, 9, 35, false, false,
                    "@Injectable({ providedIn: 'root' })", List.of()),
                new SyntaxNode("type_identifier", true, 441, 454, 10, 13, 10, 26, false, false, "OrdersService", List.of())
            ));
        SyntaxNode sharedModule = new SyntaxNode("class_declaration", true, 464, 491, 12, 0, 12, 27, false, false,
            "export class SharedModule {}", List.of(
                new SyntaxNode("type_identifier", true, 477, 489, 12, 13, 12, 25, false, false, "SharedModule", List.of())
            ));
        SyntaxNode orderFacade = new SyntaxNode("class_declaration", true, 493, source.length(), 13, 0, 13, 27, false, false,
            "export class OrderFacade {}", List.of(
                new SyntaxNode("type_identifier", true, 506, 517, 13, 13, 13, 24, false, false, "OrderFacade", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.angular.ts", source,
            program(source, ordersComponent, sharedCardComponent, ordersModule, ordersService, sharedModule, orderFacade));

        var ordersModuleEntity = entity(result, EntityKind.CLASS, "OrdersModule");
        var ordersComponentEntity = entity(result, EntityKind.CLASS, "OrdersComponent");
        var sharedCardEntity = entity(result, EntityKind.CLASS, "SharedCardComponent");
        var sharedModuleEntity = entity(result, EntityKind.CLASS, "SharedModule");
        var orderFacadeEntity = entity(result, EntityKind.CLASS, "OrderFacade");
        var ordersServiceEntity = entity(result, EntityKind.CLASS, "OrdersService");
        var applicationScopeEntity = entity(result, EntityKind.MODULE, "application:root");

        assertAngularRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "declares");
        assertAngularRelationship(result, ordersModuleEntity.id(), sharedModuleEntity.id(), "SharedModule", "imports");
        assertAngularRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "exports");
        assertAngularRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "bootstraps");
        assertAngularRelationship(result, ordersModuleEntity.id(), orderFacadeEntity.id(), "OrderFacade", "provides");
        assertAngularRelationship(result, ordersComponentEntity.id(), sharedCardEntity.id(), "SharedCardComponent", "imports");
        assertAngularRelationship(result, ordersComponentEntity.id(), orderFacadeEntity.id(), "OrderFacade", "provides");
        assertAngularRelationship(result, ordersServiceEntity.id(), applicationScopeEntity.id(), "root", "providedBy");
    }

    private static void assertAngularRelationship(StructuralExtractionResult result, String fromId, String toId, String label, String frameworkRelationship) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "angular".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElseThrow();
        assertEquals("angular:" + frameworkRelationship, relationship.metadata().get("dependencySource"));
        assertTrue(relationship.metadata().containsKey("resolvedFromDecoratorPayload"));
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

    private static info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .findFirst()
            .orElseThrow();
    }
}
