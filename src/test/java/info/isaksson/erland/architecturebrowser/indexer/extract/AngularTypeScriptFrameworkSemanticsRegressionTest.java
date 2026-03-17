package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AngularTypeScriptFrameworkSemanticsRegressionTest extends AbstractTypeScriptExtractionTestSupport {
    @Test
    void extractsAngularModuleAndStandaloneFrameworkRelationshipsFromDecoratorPayloads() {
        String source = """
            @Component({ standalone: true, imports: [SharedCardComponent, CommonModule], providers: [OrderFacade] })
            export class OrdersComponent {}

            @Directive({ standalone: true })
            export class SharedCardComponent {}

            @NgModule({
              declarations: [OrdersComponent],
              imports: [SharedModule, OrdersComponent],
              exports: [OrdersComponent],
              bootstrap: [OrdersComponent],
              providers: [OrdersFacade, provideHttpClient()]
            })
            export class OrdersModule {}

            export class SharedModule {}
            export class OrderFacade {}
            """;

        SyntaxNode ordersComponent = new SyntaxNode("class_declaration", true, 0, 140, 0, 0, 1, 31, false, false,
            """
            @Component({ standalone: true, imports: [SharedCardComponent, CommonModule], providers: [OrderFacade] })
            export class OrdersComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 109, 0, 0, 0, 109, false, false,
                    "@Component({ standalone: true, imports: [SharedCardComponent, CommonModule], providers: [OrderFacade] })", List.of()),
                new SyntaxNode("type_identifier", true, 123, 138, 1, 13, 1, 28, false, false, "OrdersComponent", List.of())
            ));
        SyntaxNode sharedCardComponent = new SyntaxNode("class_declaration", true, 142, 220, 3, 0, 4, 35, false, false,
            """
            @Directive({ standalone: true })
            export class SharedCardComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 142, 173, 3, 0, 3, 31, false, false,
                    "@Directive({ standalone: true })", List.of()),
                new SyntaxNode("type_identifier", true, 187, 206, 4, 13, 4, 32, false, false, "SharedCardComponent", List.of())
            ));
        SyntaxNode ordersModule = new SyntaxNode("class_declaration", true, 222, 478, 6, 0, 13, 28, false, false,
            """
            @NgModule({
              declarations: [OrdersComponent],
              imports: [SharedModule, OrdersComponent],
              exports: [OrdersComponent],
              bootstrap: [OrdersComponent],
              providers: [OrderFacade, provideHttpClient()]
            })
            export class OrdersModule {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 222, 448, 6, 0, 12, 2, false, false,
                    """
                    @NgModule({
                      declarations: [OrdersComponent],
                      imports: [SharedModule, OrdersComponent],
                      exports: [OrdersComponent],
                      bootstrap: [OrdersComponent],
                      providers: [OrderFacade, provideHttpClient()]
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 462, 474, 13, 13, 13, 25, false, false, "OrdersModule", List.of())
            ));
        SyntaxNode sharedModule = new SyntaxNode("class_declaration", true, 480, 507, 15, 0, 15, 27, false, false,
            "export class SharedModule {}", List.of(
                new SyntaxNode("type_identifier", true, 493, 505, 15, 13, 15, 25, false, false, "SharedModule", List.of())
            ));
        SyntaxNode orderFacade = new SyntaxNode("class_declaration", true, 509, source.length(), 16, 0, 16, 27, false, false,
            "export class OrderFacade {}", List.of(
                new SyntaxNode("type_identifier", true, 522, 533, 16, 13, 16, 24, false, false, "OrderFacade", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.angular.ts", source,
            program(source, ordersComponent, sharedCardComponent, ordersModule, sharedModule, orderFacade));

        var ordersModuleEntity = entity(result, EntityKind.CLASS, "OrdersModule");
        var ordersComponentEntity = entity(result, EntityKind.CLASS, "OrdersComponent");
        var sharedCardEntity = entity(result, EntityKind.CLASS, "SharedCardComponent");
        var sharedModuleEntity = entity(result, EntityKind.CLASS, "SharedModule");
        var orderFacadeEntity = entity(result, EntityKind.CLASS, "OrderFacade");
        var provideHttpClientEntity = entity(result, EntityKind.FUNCTION, "provideHttpClient");

        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "declares");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), sharedModuleEntity.id(), "SharedModule", "imports");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "exports");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), ordersComponentEntity.id(), "OrdersComponent", "bootstraps");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), orderFacadeEntity.id(), "OrderFacade", "provides");
        assertAngularFrameworkRelationship(result, ordersModuleEntity.id(), provideHttpClientEntity.id(), "provideHttpClient()", "provides");
        assertAngularFrameworkRelationship(result, ordersComponentEntity.id(), sharedCardEntity.id(), "SharedCardComponent", "imports");
        assertAngularFrameworkRelationship(result, ordersComponentEntity.id(), orderFacadeEntity.id(), "OrderFacade", "provides");
    }
    @Test
    void extractsAngularTemplateCompositionRelationshipsFromInlineTemplates() {
        String source = """
            @Component({
              selector: 'shared-card',
              template: '<section><ng-content></ng-content></section>'
            })
            export class SharedCardComponent {}

            @Directive({ selector: '[appTrackClick]' })
            export class TrackClickDirective {}

            @Pipe({ name: 'orderStatus' })
            export class OrderStatusPipe {}

            @Component({
              selector: 'orders-page',
              template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
            })
            export class OrdersPageComponent {}
            """;

        SyntaxNode sharedCard = new SyntaxNode("class_declaration", true, 0, 0, 0, 0, 3, 1, false, false,
            """
            @Component({
              selector: 'shared-card',
              template: '<section><ng-content></ng-content></section>'
            })
            export class SharedCardComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 0, 0, 0, 2, 2, false, false,
                    """
                    @Component({
                      selector: 'shared-card',
                      template: '<section><ng-content></ng-content></section>'
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 0, 0, 3, 13, 3, 32, false, false, "SharedCardComponent", List.of())
            ));
        SyntaxNode trackClick = new SyntaxNode("class_declaration", true, 0, 0, 5, 0, 6, 1, false, false,
            """
            @Directive({ selector: '[appTrackClick]' })
            export class TrackClickDirective {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 0, 5, 0, 5, 42, false, false,
                    "@Directive({ selector: '[appTrackClick]' })", List.of()),
                new SyntaxNode("type_identifier", true, 0, 0, 6, 13, 6, 32, false, false, "TrackClickDirective", List.of())
            ));
        SyntaxNode orderStatusPipe = new SyntaxNode("class_declaration", true, 0, 0, 8, 0, 9, 1, false, false,
            """
            @Pipe({ name: 'orderStatus' })
            export class OrderStatusPipe {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 0, 8, 0, 8, 31, false, false,
                    "@Pipe({ name: 'orderStatus' })", List.of()),
                new SyntaxNode("type_identifier", true, 0, 0, 9, 13, 9, 28, false, false, "OrderStatusPipe", List.of())
            ));
        SyntaxNode ordersPage = new SyntaxNode("class_declaration", true, 0, 0, 11, 0, 15, 1, false, false,
            """
            @Component({
              selector: 'orders-page',
              template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
            })
            export class OrdersPageComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 0, 11, 0, 14, 2, false, false,
                    """
                    @Component({
                      selector: 'orders-page',
                      template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 0, 0, 15, 13, 15, 32, false, false, "OrdersPageComponent", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders-page.component.ts", source,
            program(source, sharedCard, trackClick, orderStatusPipe, ordersPage));

        var ordersPageEntity = entity(result, EntityKind.CLASS, "OrdersPageComponent");
        var sharedCardEntity = entity(result, EntityKind.CLASS, "SharedCardComponent");
        var trackClickEntity = entity(result, EntityKind.CLASS, "TrackClickDirective");
        var orderStatusPipeEntity = entity(result, EntityKind.CLASS, "OrderStatusPipe");

        assertAngularTemplateRelationship(result, ordersPageEntity.id(), sharedCardEntity.id(), "SharedCardComponent", "templateRenders");
        assertAngularTemplateRelationship(result, ordersPageEntity.id(), trackClickEntity.id(), "TrackClickDirective", "usesDirective");
        assertAngularTemplateRelationship(result, ordersPageEntity.id(), orderStatusPipeEntity.id(), "OrderStatusPipe", "usesPipe");
    }
}
