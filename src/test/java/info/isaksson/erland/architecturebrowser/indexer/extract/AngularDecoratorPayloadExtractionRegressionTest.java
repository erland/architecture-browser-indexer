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

class AngularDecoratorPayloadExtractionRegressionTest extends AbstractTypeScriptExtractionTestSupport {
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
}
