package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReactCustomHookRegressionTest {

    @Test
    void reactCustomHookExtractionClassifiesHooksAndAddsUsageRelationships() {
        String source = """
            import { useContext } from 'react';
            import { useQuery } from '@tanstack/react-query';

            export const AuthContext = createContext(null);

            export function useAuth() {
              return useContext(AuthContext);
            }

            export function useOrdersQuery() {
              return useQuery({ queryKey: ['orders'], queryFn: async () => [] });
            }

            export function OrdersPage() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return <section>{auth?.user}-{orders.data?.length}</section>;
            }
            """;

        StructuralExtractionResult result = extract("src/hooks/useOrders.tsx", source,
            program(source,
                functionDeclaration("useAuth", 5,
                    "export function useAuth() { return useContext(AuthContext); }"),
                functionDeclaration("useOrdersQuery", 9,
                    "export function useOrdersQuery() { return useQuery({ queryKey: ['orders'], queryFn: async () => [] }); }"),
                functionDeclaration("OrdersPage", 13,
                    "export function OrdersPage() { const auth = useAuth(); const orders = useOrdersQuery(); return <section>{auth?.user}-{orders.data?.length}</section>; }")
            ));

        var useAuthEntity = entity(result, EntityKind.FUNCTION, "useAuth");
        var useOrdersQueryEntity = entity(result, EntityKind.FUNCTION, "useOrdersQuery");
        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");

        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("reactHook"));
        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("customHook"));
        assertEquals("context", useAuthEntity.metadata().get("hookClassification"));
        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("declaredReactHook"));
        assertEquals(Boolean.FALSE, useAuthEntity.metadata().get("external"));

        assertEquals(Boolean.TRUE, useOrdersQueryEntity.metadata().get("reactHook"));
        assertEquals("data-fetch", useOrdersQueryEntity.metadata().get("hookClassification"));

        assertHookRelationship(result, ordersPageEntity.id(), useAuthEntity.id(), "useAuth", "component", "context", true);
        assertHookRelationship(result, ordersPageEntity.id(), useOrdersQueryEntity.id(), "useOrdersQuery", "component", "data-fetch", true);

        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(rel.toEntityId())
            && "usesHook".equals(rel.metadata().get("frameworkRelationship"))));
    }

    private static void assertHookRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String consumerKind,
        String hookClassification,
        boolean resolved
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && "react".equals(rel.metadata().get("framework"))
                && "usesHook".equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("react:uses-hook", relationship.metadata().get("dependencySource"));
        assertEquals(consumerKind, relationship.metadata().get("hookConsumerKind"));
        assertEquals(hookClassification, relationship.metadata().get("hookClassification"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromReactHookExtraction"));
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

    private static ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .sorted((left, right) -> Integer.compare(entityScore(right), entityScore(left)))
            .findFirst()
            .orElseThrow();
    }

    private static int entityScore(ExtractedEntityFact entity) {
        int score = 0;
        if (Boolean.TRUE.equals(entity.metadata().get("reactHook"))) {
            score += 10;
        }
        if (Boolean.TRUE.equals(entity.metadata().get("declaredReactHook"))) {
            score += 5;
        }
        if (Boolean.FALSE.equals(entity.metadata().get("external"))) {
            score += 2;
        }
        return score;
    }
}
