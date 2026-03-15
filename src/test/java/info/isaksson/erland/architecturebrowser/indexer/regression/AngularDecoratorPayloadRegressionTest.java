package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
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

class AngularDecoratorPayloadRegressionTest {

    @Test
    void angularDecoratorPayloadMetadataRemainsStableOnObservedEntities() {
        String source = """
            @Component({ selector: 'app-orders', standalone: true, imports: [CommonModule], templateUrl: './orders.component.html' })
            export class OrdersComponent {}

            @NgModule({ declarations: [OrdersComponent], exports: [OrdersComponent], providers: [OrdersFacade] })
            export class OrdersModule {}

            @Injectable({ providedIn: 'root' })
            export class OrdersService {}
            """;

        SyntaxNode component = new SyntaxNode("class_declaration", true, 0, 152, 0, 0, 1, 31, false, false,
            """
            @Component({ selector: 'app-orders', standalone: true, imports: [CommonModule], templateUrl: './orders.component.html' })
            export class OrdersComponent {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 0, 122, 0, 0, 0, 122, false, false,
                    "@Component({ selector: 'app-orders', standalone: true, imports: [CommonModule], templateUrl: './orders.component.html' })", List.of()),
                new SyntaxNode("type_identifier", true, 136, 151, 1, 13, 1, 28, false, false, "OrdersComponent", List.of())
            ));
        SyntaxNode module = new SyntaxNode("class_declaration", true, 154, 300, 3, 0, 4, 28, false, false,
            """
            @NgModule({ declarations: [OrdersComponent], exports: [OrdersComponent], providers: [OrdersFacade] })
            export class OrdersModule {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 154, 256, 3, 0, 3, 102, false, false,
                    "@NgModule({ declarations: [OrdersComponent], exports: [OrdersComponent], providers: [OrdersFacade] })", List.of()),
                new SyntaxNode("type_identifier", true, 270, 282, 4, 13, 4, 25, false, false, "OrdersModule", List.of())
            ));
        SyntaxNode service = new SyntaxNode("class_declaration", true, 302, source.length(), 6, 0, 7, 29, false, false,
            """
            @Injectable({ providedIn: 'root' })
            export class OrdersService {}
            """.strip(), List.of(
                new SyntaxNode("decorator", true, 302, 337, 6, 0, 6, 35, false, false,
                    "@Injectable({ providedIn: 'root' })", List.of()),
                new SyntaxNode("type_identifier", true, 351, 364, 7, 13, 7, 26, false, false, "OrdersService", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders.angular.ts", source, program(source, component, module, service));

        var componentEntity = entity(result, "OrdersComponent");
        assertEquals("component", componentEntity.metadata().get("angularKind"));
        assertEquals("app-orders", componentEntity.metadata().get("angularSelector"));
        assertEquals(true, componentEntity.metadata().get("angularStandalone"));
        assertEquals(List.of("CommonModule"), componentEntity.metadata().get("angularImports"));
        assertEquals("./orders.component.html", componentEntity.metadata().get("angularTemplateUrl"));

        var moduleEntity = entity(result, "OrdersModule");
        assertEquals("module", moduleEntity.metadata().get("angularKind"));
        assertEquals(List.of("OrdersComponent"), moduleEntity.metadata().get("angularDeclarations"));
        assertEquals(List.of("OrdersComponent"), moduleEntity.metadata().get("angularExports"));
        assertEquals(List.of("OrdersFacade"), moduleEntity.metadata().get("angularProviders"));

        var serviceEntity = entity(result, "OrdersService");
        assertEquals("injectable", serviceEntity.metadata().get("angularKind"));
        assertEquals("root", serviceEntity.metadata().get("angularProvidedIn"));
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

    private static info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact entity(StructuralExtractionResult result, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.CLASS && name.equals(entity.name()))
            .findFirst()
            .orElseThrow();
    }
}
