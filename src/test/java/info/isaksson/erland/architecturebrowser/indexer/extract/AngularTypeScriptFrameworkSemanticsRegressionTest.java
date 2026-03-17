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
    void extractsAngularComponentDirectiveAndPipeDecoratorPayloadMetadata() {
        String source = """
            @Component({
              selector: 'app-order-list',
              templateUrl: './order-list.component.html',
              styleUrls: ['./order-list.component.css', './shared.css'],
              standalone: true,
              imports: [CommonModule, RouterModule],
              providers: [OrderFacade],
              template: `<section>Orders</section>`
            })
            export class OrderListComponent {}

            @Directive({ selector: '[appFocus]', standalone: true, providers: [FocusService] })
            export class FocusDirective {}

            @Pipe({ name: 'money', standalone: false })
            export class MoneyPipe {}
            """;

        SyntaxNode component = new SyntaxNode("class_declaration", true, 0, 329, 0, 0, 8, 35, false, false,
            """
            @Component({
              selector: 'app-order-list',
              templateUrl: './order-list.component.html',
              styleUrls: ['./order-list.component.css', './shared.css'],
              standalone: true,
              imports: [CommonModule, RouterModule],
              providers: [OrderFacade],
              template: `<section>Orders</section>`
            })
            export class OrderListComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 280, 0, 0, 7, 2, false, false,
                    """
                    @Component({
                      selector: 'app-order-list',
                      templateUrl: './order-list.component.html',
                      styleUrls: ['./order-list.component.css', './shared.css'],
                      standalone: true,
                      imports: [CommonModule, RouterModule],
                      providers: [OrderFacade],
                      template: `<section>Orders</section>`
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 294, 312, 8, 13, 8, 31, false, false, "OrderListComponent", List.of())
            ));
        SyntaxNode directive = new SyntaxNode("class_declaration", true, 331, 455, 10, 0, 11, 31, false, false,
            """
            @Directive({ selector: '[appFocus]', standalone: true, providers: [FocusService] })
            export class FocusDirective {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 331, 418, 10, 0, 10, 87, false, false,
                    "@Directive({ selector: '[appFocus]', standalone: true, providers: [FocusService] })", List.of()),
                new SyntaxNode("type_identifier", true, 432, 446, 11, 13, 11, 27, false, false, "FocusDirective", List.of())
            ));
        SyntaxNode pipe = new SyntaxNode("class_declaration", true, 457, source.length(), 13, 0, 14, 24, false, false,
            """
            @Pipe({ name: 'money', standalone: false })
            export class MoneyPipe {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 457, 503, 13, 0, 13, 46, false, false,
                    "@Pipe({ name: 'money', standalone: false })", List.of()),
                new SyntaxNode("type_identifier", true, 517, 526, 14, 13, 14, 22, false, false, "MoneyPipe", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/angular-metadata.ts", source, program(source, component, directive, pipe));

        var componentEntity = entity(result, EntityKind.CLASS, "OrderListComponent");
        assertEquals("angular", componentEntity.metadata().get("framework"));
        assertEquals("Component", componentEntity.metadata().get("angularDecorator"));
        assertEquals("component", componentEntity.metadata().get("angularKind"));
        assertEquals("app-order-list", componentEntity.metadata().get("angularSelector"));
        assertEquals("./order-list.component.html", componentEntity.metadata().get("angularTemplateUrl"));
        assertEquals(true, componentEntity.metadata().get("angularHasInlineTemplate"));
        assertEquals(true, componentEntity.metadata().get("angularStandalone"));
        assertEquals(List.of("./order-list.component.css", "./shared.css"), componentEntity.metadata().get("angularStyleUrls"));
        assertEquals(List.of("CommonModule", "RouterModule"), componentEntity.metadata().get("angularImports"));
        assertEquals(List.of("OrderFacade"), componentEntity.metadata().get("angularProviders"));

        var directiveEntity = entity(result, EntityKind.CLASS, "FocusDirective");
        assertEquals("Directive", directiveEntity.metadata().get("angularDecorator"));
        assertEquals("directive", directiveEntity.metadata().get("angularKind"));
        assertEquals("[appFocus]", directiveEntity.metadata().get("angularSelector"));
        assertEquals(true, directiveEntity.metadata().get("angularStandalone"));
        assertEquals(List.of("FocusService"), directiveEntity.metadata().get("angularProviders"));

        var pipeEntity = entity(result, EntityKind.CLASS, "MoneyPipe");
        assertEquals("Pipe", pipeEntity.metadata().get("angularDecorator"));
        assertEquals("pipe", pipeEntity.metadata().get("angularKind"));
        assertEquals("money", pipeEntity.metadata().get("angularPipeName"));
        assertEquals(false, pipeEntity.metadata().get("angularStandalone"));
    }



    @Test
    void extractsAngularNgModuleAndInjectableDecoratorPayloadMetadata() {
        String source = """
            @NgModule({
              imports: [CommonModule, RouterModule.forChild(routes)],
              declarations: [OrderListComponent, FocusDirective],
              exports: [OrderListComponent],
              providers: [OrderFacade, provideHttpClient()],
              bootstrap: [OrderListComponent]
            })
            export class OrdersModule {}

            @Injectable({ providedIn: 'root' })
            export class OrderService {}
            """;

        SyntaxNode ordersModule = new SyntaxNode("class_declaration", true, 0, 297, 0, 0, 6, 29, false, false,
            """
            @NgModule({
              imports: [CommonModule, RouterModule.forChild(routes)],
              declarations: [OrderListComponent, FocusDirective],
              exports: [OrderListComponent],
              providers: [OrderFacade, provideHttpClient()],
              bootstrap: [OrderListComponent]
            })
            export class OrdersModule {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 267, 0, 0, 5, 2, false, false,
                    """
                    @NgModule({
                      imports: [CommonModule, RouterModule.forChild(routes)],
                      declarations: [OrderListComponent, FocusDirective],
                      exports: [OrderListComponent],
                      providers: [OrderFacade, provideHttpClient()],
                      bootstrap: [OrderListComponent]
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 281, 293, 6, 13, 6, 25, false, false, "OrdersModule", List.of())
            ));
        SyntaxNode orderService = new SyntaxNode("class_declaration", true, 299, source.length(), 8, 0, 9, 29, false, false,
            """
            @Injectable({ providedIn: 'root' })
            export class OrderService {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 299, 334, 8, 0, 8, 35, false, false,
                    "@Injectable({ providedIn: 'root' })", List.of()),
                new SyntaxNode("type_identifier", true, 348, 360, 9, 13, 9, 25, false, false, "OrderService", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.module.ts", source, program(source, ordersModule, orderService));

        var ordersModuleEntity = entity(result, EntityKind.CLASS, "OrdersModule");
        assertEquals("angular", ordersModuleEntity.metadata().get("framework"));
        assertEquals("NgModule", ordersModuleEntity.metadata().get("angularDecorator"));
        assertEquals("module", ordersModuleEntity.metadata().get("angularKind"));
        assertEquals(List.of("CommonModule", "RouterModule.forChild(routes)"), ordersModuleEntity.metadata().get("angularImports"));
        assertEquals(List.of("OrderListComponent", "FocusDirective"), ordersModuleEntity.metadata().get("angularDeclarations"));
        assertEquals(List.of("OrderListComponent"), ordersModuleEntity.metadata().get("angularExports"));
        assertEquals(List.of("OrderFacade", "provideHttpClient()"), ordersModuleEntity.metadata().get("angularProviders"));
        assertEquals(List.of("OrderListComponent"), ordersModuleEntity.metadata().get("angularBootstrap"));

        var orderServiceEntity = entity(result, EntityKind.CLASS, "OrderService");
        assertEquals("Injectable", orderServiceEntity.metadata().get("angularDecorator"));
        assertEquals("injectable", orderServiceEntity.metadata().get("angularKind"));
        assertEquals("root", orderServiceEntity.metadata().get("angularProvidedIn"));
    }



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
    void extractsAngularInjectableProvidedByApplicationScopeRelationship() {
        String source = """
            @Injectable({ providedIn: 'root' })
            export class OrdersService {}
            """;
        SyntaxNode service = new SyntaxNode("class_declaration", true, 0, source.length(), 0, 0, 1, 29, false, false,
            """
            @Injectable({ providedIn: 'root' })
            export class OrdersService {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 35, 0, 0, 0, 35, false, false,
                    "@Injectable({ providedIn: 'root' })", List.of()),
                new SyntaxNode("type_identifier", true, 49, 62, 1, 13, 1, 26, false, false, "OrdersService", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.service.ts", source, program(source, service));

        var ordersServiceEntity = entity(result, EntityKind.CLASS, "OrdersService");
        var applicationScopeEntity = entity(result, EntityKind.MODULE, "application:root");
        assertAngularFrameworkRelationship(result, ordersServiceEntity.id(), applicationScopeEntity.id(), "root", "providedBy");
    }




    @Test
    void extractsAngularDiProviderAndConstructorInjectionRelationships() {
        String source = """
            export const ORDER_API = new InjectionToken<OrderApi>('ORDER_API');

            @Injectable()
            export class OrdersApiService {}

            export function ordersConfigFactory() { return {}; }

            @Injectable()
            export class OrdersFacade {}

            @Component({
              providers: [
                { provide: ORDER_API, useClass: OrdersApiService },
                { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory },
                OrdersFacade
              ]
            })
            export class OrdersComponent {
              constructor(@Inject(ORDER_API) private api: OrdersApiService, private facade: OrdersFacade) {}
            }
            """;

        SyntaxNode ordersApiService = classDeclaration(0, 0, 3, "OrdersApiService", List.of(
            new SyntaxNode("decorator", true, 0, 0, 3, 0, 3, 13, false, false, "@Injectable()", List.of())
        ));
        SyntaxNode ordersConfigFactory = new SyntaxNode("function_declaration", true, 0, 0, 5, 0, 5, 60, false, false,
            "export function ordersConfigFactory() { return {}; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 5, 16, 5, 35, false, false, "ordersConfigFactory", List.of())
            ));
        SyntaxNode ordersFacade = classDeclaration(0, 0, 8, "OrdersFacade", List.of(
            new SyntaxNode("decorator", true, 0, 0, 7, 0, 7, 13, false, false, "@Injectable()", List.of())
        ));
        SyntaxNode ordersComponent = new SyntaxNode("class_declaration", true, 0, 0, 10, 0, 18, 1, false, false,
            """
            @Component({
              providers: [
                { provide: ORDER_API, useClass: OrdersApiService },
                { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory },
                OrdersFacade
              ]
            })
            export class OrdersComponent {
              constructor(@Inject(ORDER_API) private api: OrdersApiService, private facade: OrdersFacade) {}
            }
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 0, 10, 0, 16, 2, false, false,
                    """
                    @Component({
                      providers: [
                        { provide: ORDER_API, useClass: OrdersApiService },
                        { provide: ORDERS_CONFIG, useFactory: ordersConfigFactory },
                        OrdersFacade
                      ]
                    })
                    """.strip(), List.of()),
                new SyntaxNode("type_identifier", true, 0, 0, 17, 13, 17, 28, false, false, "OrdersComponent", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.component.ts", source,
            program(source, ordersApiService, ordersConfigFactory, ordersFacade, ordersComponent));

        var ordersComponentEntity = entity(result, EntityKind.CLASS, "OrdersComponent");
        var ordersApiServiceEntity = entity(result, EntityKind.CLASS, "OrdersApiService");
        var ordersFacadeEntity = entity(result, EntityKind.CLASS, "OrdersFacade");
        var orderApiTokenEntity = entity(result, EntityKind.MODULE, "ORDER_API");
        var ordersConfigTokenEntity = entity(result, EntityKind.MODULE, "ORDERS_CONFIG");
        var ordersConfigFactoryEntity = entity(result, EntityKind.FUNCTION, "ordersConfigFactory");

        assertAngularDiRelationship(result, ordersComponentEntity.id(), orderApiTokenEntity.id(), "ORDER_API", "injects", true);
        assertAngularDiRelationship(result, ordersComponentEntity.id(), ordersFacadeEntity.id(), "OrdersFacade", "injects", true);
        assertAngularDiRelationship(result, orderApiTokenEntity.id(), ordersComponentEntity.id(), "ORDER_API", "providedBy", true);
        assertAngularDiRelationship(result, orderApiTokenEntity.id(), ordersApiServiceEntity.id(), "OrdersApiService", "resolvesTo", true);
        assertAngularDiRelationship(result, ordersFacadeEntity.id(), ordersComponentEntity.id(), "OrdersFacade", "providedBy", true);
        assertAngularDiRelationship(result, ordersConfigTokenEntity.id(), ordersConfigFactoryEntity.id(), "ordersConfigFactory", "resolvesTo", true);
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
