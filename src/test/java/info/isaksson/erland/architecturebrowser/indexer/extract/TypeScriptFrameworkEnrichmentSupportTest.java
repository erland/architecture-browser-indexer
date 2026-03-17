package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptFrameworkEnrichmentSupportTest {

    @Test
    void preservesAngularFrameworkRelationshipsThroughDedicatedAngularEnrichmentSupport() {
        String source = """
            @Component({ imports: [SharedCardComponent], providers: [OrderFacade] })
            export class OrdersComponent {}
            export class SharedCardComponent {}
            export class OrderFacade {}
            """;

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        TypeScriptExtractionContext context = context("src/app/orders.angular.ts", source);
        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        namedEntities.put("OrdersComponent", namedEntity("OrdersComponent", EntityKind.CLASS, "@Component({ imports: [SharedCardComponent], providers: [OrderFacade] }) export class OrdersComponent {}", Map.of(
            "framework", "angular",
            "angularKind", "component",
            "angularStandalone", true,
            "angularImports", List.of("SharedCardComponent"),
            "angularProviders", List.of("OrderFacade"),
            "qualifiedName", "src.app.OrdersComponent"
        )));
        namedEntities.put("SharedCardComponent", namedEntity("SharedCardComponent", EntityKind.CLASS, "export class SharedCardComponent {}", Map.of(
            "qualifiedName", "src.app.SharedCardComponent"
        )));
        namedEntities.put("OrderFacade", namedEntity("OrderFacade", EntityKind.CLASS, "export class OrderFacade {}", Map.of(
            "qualifiedName", "src.app.OrderFacade"
        )));
        context = new TypeScriptExtractionContext(context.parseResult(), accumulator, context.relativePath(), context.extractionMode(), context.root(), context.fileEntity());

        namedEntities.values().forEach(accumulator::addEntity);
        AngularTypeScriptFrameworkEnrichmentSupport.extract(context, namedEntities);

        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(namedEntities.get("OrdersComponent").id())
            && rel.toEntityId().equals(namedEntities.get("SharedCardComponent").id())
            && "angular".equals(rel.metadata().get("framework"))
            && "imports".equals(rel.metadata().get("frameworkRelationship"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(namedEntities.get("OrdersComponent").id())
            && rel.toEntityId().equals(namedEntities.get("OrderFacade").id())
            && "angular".equals(rel.metadata().get("framework"))
            && "provides".equals(rel.metadata().get("frameworkRelationship"))));
    }

    @Test
    void preservesReactFrameworkRelationshipsThroughDedicatedReactEnrichmentSupport() {
        String source = """
            export const AuthContext = createContext(null);
            export function useAuth() { return useContext(AuthContext); }
            export function OrdersPage() { const auth = useAuth(); return <section>{auth?.user}</section>; }
            """;

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        TypeScriptExtractionContext context = context("src/hooks/useOrders.tsx", source);
        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        namedEntities.put("AuthContext", namedEntity("AuthContext", EntityKind.CLASS, "export const AuthContext = createContext(null);", Map.of(
            "qualifiedName", "src.hooks.AuthContext"
        )));
        namedEntities.put("useAuth", namedEntity("useAuth", EntityKind.FUNCTION, "export function useAuth() { return useContext(AuthContext); }", Map.of(
            "qualifiedName", "src.hooks.useAuth"
        )));
        namedEntities.put("OrdersPage", namedEntity("OrdersPage", EntityKind.FUNCTION, "export function OrdersPage() { const auth = useAuth(); return <section>{auth?.user}</section>; }", Map.of(
            "qualifiedName", "src.hooks.OrdersPage"
        )));
        context = new TypeScriptExtractionContext(context.parseResult(), accumulator, context.relativePath(), context.extractionMode(), context.root(), context.fileEntity());

        namedEntities.values().forEach(accumulator::addEntity);
        ReactTypeScriptFrameworkEnrichmentSupport.extract(context, namedEntities);

        ExtractedEntityFact useAuth = accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.FUNCTION && "useAuth".equals(entity.name()) && Boolean.TRUE.equals(entity.metadata().get("reactHook")))
            .findFirst()
            .orElseThrow();

        assertEquals("context", useAuth.metadata().get("hookClassification"));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && rel.fromEntityId().equals(namedEntities.get("OrdersPage").id())
            && rel.toEntityId().equals(useAuth.id())
            && "react".equals(rel.metadata().get("framework"))
            && "usesHook".equals(rel.metadata().get("frameworkRelationship"))));
    }

    private static TypeScriptExtractionContext context(String relativePath, String source) {
        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0,
            Math.max(0, source.split("\\R", -1).length - 1),
            source.isEmpty() ? 0 : source.length() - source.lastIndexOf('\n') - 1,
            false, false, source, List.of());
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
        return new TypeScriptExtractionContext(
            parseResult,
            new ExtractionAccumulator(),
            relativePath,
            ExtractionMode.SYNTAX_TREE,
            root,
            ExtractionSupport.fileModuleEntity("scope:file:test", relativePath, "typescript")
        );
    }

    private static ExtractedEntityFact namedEntity(String name, EntityKind kind, String snippet, Map<String, Object> metadata) {
        return new ExtractedEntityFact(
            "entity:test:" + name,
            kind,
            info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED,
            name,
            name,
            "scope:file:test",
            List.of(ExtractionSupport.sourceRef("src/test.tsx", 1, snippet, Map.of("language", "typescript"))),
            metadata
        );
    }
}
