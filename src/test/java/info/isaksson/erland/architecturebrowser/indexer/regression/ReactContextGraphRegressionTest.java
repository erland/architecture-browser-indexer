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

class ReactContextGraphRegressionTest {

    @Test
    void reactContextExtractionAddsExplicitProviderAndConsumerRelationships() {
        String source = """
            import React, { createContext, useContext } from 'react';

            export const AuthContext = createContext(null);

            export function AuthProvider({ children }) {
              return <AuthContext.Provider value={{ user: 'alice' }}>{children}</AuthContext.Provider>;
            }

            export function useAuth() {
              return useContext(AuthContext);
            }

            export function OrdersPage() {
              const auth = useContext(AuthContext);
              return <section>{auth?.user}</section>;
            }
            """;

        StructuralExtractionResult result = extract("src/context/AuthProvider.tsx", source,
            program(source,
                functionDeclaration("AuthProvider", 4,
                    "export function AuthProvider({ children }) { return <AuthContext.Provider value={{ user: 'alice' }}>{children}</AuthContext.Provider>; }"),
                functionDeclaration("useAuth", 8,
                    "export function useAuth() { return useContext(AuthContext); }"),
                functionDeclaration("OrdersPage", 12,
                    "export function OrdersPage() { const auth = useContext(AuthContext); return <section>{auth?.user}</section>; }")
            ));

        var authContext = entity(result, EntityKind.UI_MODULE, "AuthContext");
        assertEquals(Boolean.TRUE, authContext.metadata().get("reactContext"));
        assertEquals(Boolean.FALSE, authContext.metadata().get("external"));
        assertEquals(Boolean.TRUE, authContext.metadata().get("declaredReactContext"));

        assertContextRelationship(result, entity(result, EntityKind.FUNCTION, "AuthProvider").id(), authContext.id(), "AuthContext", "providesContext", true);
        assertContextRelationship(result, entity(result, EntityKind.FUNCTION, "useAuth").id(), authContext.id(), "AuthContext", "consumesContext", true);
        assertContextRelationship(result, entity(result, EntityKind.FUNCTION, "OrdersPage").id(), authContext.id(), "AuthContext", "consumesContext", true);

        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && "AuthProvider".equals(rel.label())
            && rel.fromEntityId().equals(rel.toEntityId())));
    }

    private static void assertContextRelationship(
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
                && "react".equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        String expectedSource = "providesContext".equals(frameworkRelationship) ? "react:provides-context" : "react:consumes-context";
        assertEquals(expectedSource, relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromReactContextExtraction"));
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
        if (Boolean.TRUE.equals(entity.metadata().get("reactContext"))) {
            score += 10;
        }
        if (Boolean.TRUE.equals(entity.metadata().get("declaredReactContext"))) {
            score += 5;
        }
        if (Boolean.FALSE.equals(entity.metadata().get("external"))) {
            score += 2;
        }
        return score;
    }
}
