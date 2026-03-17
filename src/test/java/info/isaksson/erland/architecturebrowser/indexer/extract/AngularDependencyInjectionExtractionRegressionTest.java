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

class AngularDependencyInjectionExtractionRegressionTest extends AbstractTypeScriptExtractionTestSupport {
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
}
