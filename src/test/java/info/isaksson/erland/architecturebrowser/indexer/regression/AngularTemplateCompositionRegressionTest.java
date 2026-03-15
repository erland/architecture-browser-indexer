package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
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

class AngularTemplateCompositionRegressionTest {

    @Test
    void angularTemplateExtractionAddsComponentDirectiveAndPipeRelationships() {
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

        StructuralExtractionResult result = extract("src/app/orders-page.component.ts", source, program(source, sharedCard, trackClick, orderStatusPipe, ordersPage));

        var ordersPageEntity = entity(result, EntityKind.CLASS, "OrdersPageComponent");
        var sharedCardEntity = entity(result, EntityKind.CLASS, "SharedCardComponent");
        var trackClickEntity = entity(result, EntityKind.CLASS, "TrackClickDirective");
        var orderStatusPipeEntity = entity(result, EntityKind.CLASS, "OrderStatusPipe");

        assertAngularTemplateRelationship(result, ordersPageEntity.id(), sharedCardEntity.id(), "SharedCardComponent", "templateRenders");
        assertAngularTemplateRelationship(result, ordersPageEntity.id(), trackClickEntity.id(), "TrackClickDirective", "usesDirective");
        assertAngularTemplateRelationship(result, ordersPageEntity.id(), orderStatusPipeEntity.id(), "OrderStatusPipe", "usesPipe");
    }

    private static void assertAngularTemplateRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String frameworkRelationship
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
        String expectedSource = switch (frameworkRelationship) {
            case "templateRenders" -> "angular:template-renders";
            case "usesDirective" -> "angular:template-uses-directive";
            case "usesPipe" -> "angular:template-uses-pipe";
            default -> throw new IllegalArgumentException(frameworkRelationship);
        };
        assertEquals(expectedSource, relationship.metadata().get("dependencySource"));
        assertEquals(Boolean.TRUE, relationship.metadata().get("resolvedFromAngularTemplateExtraction"));
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

    private static ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .findFirst()
            .orElseThrow();
    }
}
