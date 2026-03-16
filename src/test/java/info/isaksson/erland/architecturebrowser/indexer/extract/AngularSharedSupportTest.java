package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AngularSharedSupportTest {

    @Test
    void topLevelObjectFieldsPreserveNestedArraysAndObjects() {
        Map<String, String> fields = AngularLiteralSupport.topLevelObjectFields("""
            {
              imports: [CommonModule, RouterModule.forChild(routes)],
              providers: [{ provide: ORDERS_TOKEN, useClass: OrdersService }],
              standalone: true
            }
            """);

        assertEquals("[CommonModule, RouterModule.forChild(routes)]", fields.get("imports"));
        assertEquals("[{ provide: ORDERS_TOKEN, useClass: OrdersService }]", fields.get("providers"));
        assertEquals("true", fields.get("standalone"));
    }

    @Test
    void decoratorModelExtractorReturnsAngularKindAndFields() {
        SyntaxNode declaration = new SyntaxNode(
            "class_declaration", true, 0, 97, 0, 0, 1, 0, false, false,
            "@Directive({ selector: '[appTrack]', providers: [TrackDirective] }) export class TrackDirective {}",
            List.of(
                new SyntaxNode(
                    "decorator", true, 0, 67, 0, 0, 0, 67, false, false,
                    "@Directive({ selector: '[appTrack]', providers: [TrackDirective] })",
                    List.of()
                )
            )
        );

        AngularDecoratorModel model = AngularDecoratorModelExtractor.extract(declaration).orElseThrow();

        assertEquals("Directive", model.decoratorName());
        assertEquals("directive", model.angularKind());
        assertEquals("'[appTrack]'", model.fields().get("selector"));
        assertEquals("[TrackDirective]", model.fields().get("providers"));
    }

    @Test
    void selectorAndReferenceNormalizationStayStable() {
        assertEquals(List.of("app-orders", "app-detail"), AngularReferenceSupport.normalizedSelectorValues("app-orders, [app-detail]"));
        assertEquals("ORDERS_TOKEN", AngularReferenceSupport.normalizeReference("InjectionToken<OrdersConfig>(ORDERS_TOKEN)"));
        assertTrue(AngularReferenceSupport.normalizePipeName("currency").equals("currency"));
    }
}
