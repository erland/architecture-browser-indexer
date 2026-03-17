package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import info.isaksson.erland.architecturebrowser.indexer.topology.TopologyService;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class AbstractFrontendArchitectureFixtureTestSupport {

    protected static ArchitectureIndexDocument buildAngularDocument() {
        return buildDocument(FrontendArchitectureFixtureFixtures.angularFiles());
    }

    protected static ArchitectureIndexDocument buildReactDocument() {
        return buildDocument(FrontendArchitectureFixtureFixtures.reactFiles());
    }

    protected static void assertHasDependencyViews(ArchitectureIndexDocument document, String... keys) {
        Map<String, Object> views = dependencyViews(document);
        for (String key : keys) {
            assertTrue(views.containsKey(key), () -> "Missing dependency view " + key + ". keys=" + views.keySet());
        }
    }

    protected static void assertFrontendBrowserViewIds(ArchitectureIndexDocument document, String... ids) {
        List<Map<String, Object>> views = frontendBrowserViews(document);
        Set<String> actual = views.stream().map(view -> String.valueOf(view.get("id"))).collect(Collectors.toCollection(LinkedHashSet::new));
        for (String id : ids) {
            assertTrue(actual.contains(id), () -> "Missing frontend browser view " + id + ". actual=" + actual);
        }
    }

    protected static void assertFrameworkRelationshipPresent(List<Map<String, Object>> dependencies, String relationship) {
        assertTrue(dependencies.stream().anyMatch(dep -> ((List<?>) dep.getOrDefault("frameworkRelationships", List.of())).contains(relationship)),
            () -> "Expected framework relationship " + relationship + " in dependencies=" + dependencies);
    }

    protected static List<Map<String, Object>> dependencyViewList(ArchitectureIndexDocument document, String key) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> value = (List<Map<String, Object>>) dependencyViews(document).get(key);
        return value;
    }

    protected static List<Map<String, Object>> frontendBrowserViews(ArchitectureIndexDocument document) {
        @SuppressWarnings("unchecked")
        Map<String, Object> frontendBrowserViews = (Map<String, Object>) dependencyViews(document).get("frontendBrowserViews");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> views = (List<Map<String, Object>>) frontendBrowserViews.get("views");
        return views;
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> dependencyViews(ArchitectureIndexDocument document) {
        return (Map<String, Object>) document.metadata().get("dependencyViews");
    }

    private static ArchitectureIndexDocument buildDocument(List<TsFixtureFile> files) {
        Set<String> technologies = files.stream()
            .flatMap(file -> file.technologies().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        FileInventory inventory = new FileInventory(
            files.stream()
                .map(file -> new FileInventoryEntry(file.path(), file.source().length(), extension(file.path()), "source", file.language(), false, List.copyOf(file.technologies())))
                .toList(),
            files.size(),
            files.size(),
            0,
            Set.of(ParseLanguage.TYPESCRIPT.name().toLowerCase()),
            technologies
        );

        ParseBatchResult parseBatchResult = new ParseBatchResult(
            files.stream()
                .map(file -> new SourceParseResult(
                    new SourceParseRequest(Path.of(file.path()), file.path(), ParseLanguage.TYPESCRIPT, file.source()),
                    ParseStatus.SUCCESS,
                    new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", file.root(), false, file.root().nodeCount()),
                    List.of(),
                    Map.of("parserBackend", "tree-sitter-jtreesitter")
                ))
                .toList(),
            Map.of(ParseLanguage.TYPESCRIPT, files.size()),
            Map.of(ParseStatus.SUCCESS, files.size())
        );

        StructuralExtractionResult extraction = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(parseBatchResult);
        InterpretationResult interpretation = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);
        TopologyResult topology = new TopologyService().infer(inventory, extraction, interpretation);

        return ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("fixture", "/tmp/fixture", Instant.parse("2026-03-15T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatchResult,
            extraction,
            interpretation,
            topology
        );
    }

    protected static TsFixtureFile tsFile(String path, String language, String technology, String body, List<String> imports, List<SyntaxNode> declarations) {
        String source = normalize(body);
        List<SyntaxNode> children = new ArrayList<>();
        for (String importSnippet : imports) {
            children.add(importStatement(source, importSnippet));
        }
        children.addAll(declarations);
        return new TsFixtureFile(path, source, program(source, children.toArray(SyntaxNode[]::new)), language, Set.of(technology));
    }

    protected static SyntaxNode program(String source, SyntaxNode... children) {
        int[] end = lineAndColumn(source, source.length());
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, end[0], end[1], false, false, source, List.of(children));
    }

    protected static SyntaxNode importStatement(String source, String snippet) { return node("import_statement", source, snippet, List.of()); }

    protected static SyntaxNode interfaceDeclaration(String snippet, String name, List<String> extendsTypes, List<SyntaxNode> properties, List<SyntaxNode> methods) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        children.add(localLeaf(normalized, "type_identifier", name));
        if (!extendsTypes.isEmpty()) {
            List<SyntaxNode> extChildren = extendsTypes.stream().map(type -> localLeaf(normalized, "type_identifier", type)).toList();
            children.add(localNode(normalized, "extends_clause", "extends " + String.join(", ", extendsTypes), extChildren));
        }
        children.addAll(properties);
        children.addAll(methods);
        return localNode(normalized, "interface_declaration", normalized, children);
    }

    protected static SyntaxNode classDeclaration(String snippet, String name, List<String> decorators, String extendsType, List<String> implementsTypes, List<SyntaxNode> members) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        decorators.forEach(decorator -> children.add(localNode(normalized, "decorator", decorator, List.of())));
        children.add(localLeaf(normalized, "type_identifier", name));
        if (extendsType != null) {
            children.add(localNode(normalized, "extends_clause", "extends " + extendsType, List.of(localLeaf(normalized, "type_identifier", extendsType))));
        }
        if (!implementsTypes.isEmpty()) {
            List<SyntaxNode> implChildren = implementsTypes.stream().map(type -> localLeaf(normalized, "type_identifier", type)).toList();
            children.add(localNode(normalized, "implements_clause", "implements " + String.join(", ", implementsTypes), implChildren));
        }
        children.addAll(members);
        return localNode(normalized, "class_declaration", normalized, children);
    }

    protected static SyntaxNode publicField(String snippet, String name) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = List.of(localLeaf(normalized, "property_identifier", name), localLeaf(normalized, "type_identifier", declaredType(normalized)));
        return localNode(normalized, "public_field_definition", normalized, children);
    }

    protected static SyntaxNode propertySignature(String snippet, String name) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = List.of(localLeaf(normalized, "property_identifier", name), localLeaf(normalized, "type_identifier", declaredType(normalized)));
        return localNode(normalized, "property_signature", normalized, children);
    }

    protected static SyntaxNode methodDefinition(String snippet, String name, String returnType) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        children.add(localLeaf(normalized, "property_identifier", name));
        children.add(localNode(normalized, "formal_parameters", between(normalized, '(', ')'), List.of()));
        if (returnType != null) children.add(localLeaf(normalized, "type_identifier", returnType));
        return localNode(normalized, "method_definition", normalized, children);
    }

    protected static SyntaxNode functionDeclaration(String snippet, String name) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        children.add(localLeaf(normalized, "identifier", name));
        children.add(localNode(normalized, "formal_parameters", between(normalized, '(', ')'), List.of()));
        return localNode(normalized, "function_declaration", normalized, children);
    }

    private static SyntaxNode node(String type, String source, String snippet, List<SyntaxNode> children) {
        int start = source.indexOf(snippet);
        if (start < 0) throw new IllegalArgumentException("Snippet not found: " + snippet);
        int end = start + snippet.length();
        int[] from = lineAndColumn(source, start);
        int[] to = lineAndColumn(source, end);
        return new SyntaxNode(type, true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, children);
    }

    private static SyntaxNode localNode(String source, String type, String snippet, List<SyntaxNode> children) {
        int start = source.indexOf(snippet);
        if (start < 0) throw new IllegalArgumentException("Snippet not found in local source: " + snippet);
        int end = start + snippet.length();
        int[] from = lineAndColumn(source, start);
        int[] to = lineAndColumn(source, end);
        return new SyntaxNode(type, true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, children);
    }

    private static SyntaxNode localLeaf(String source, String type, String text) { return localNode(source, type, text, List.of()); }

    private static int[] lineAndColumn(String source, int offset) {
        int line=0,column=0;
        for (int i=0;i<offset && i<source.length();i++) {
            if (source.charAt(i)=='\n') { line++; column=0; } else { column++; }
        }
        return new int[]{line,column};
    }

    private static String between(String text, char start, char end) {
        int from=text.indexOf(start), to=text.indexOf(end, from+1);
        if (from<0 || to<0) return "()";
        return text.substring(from, to+1);
    }

    private static String declaredType(String snippet) {
        int colon=snippet.indexOf(':');
        if (colon<0) return "";
        String tail=snippet.substring(colon+1).trim();
        if (tail.endsWith(";")) tail=tail.substring(0, tail.length()-1).trim();
        return tail;
    }

    private static String normalize(String text) { return text.stripIndent().strip() + "\n"; }
    private static String extension(String path) { int idx=path.lastIndexOf('.'); return idx<0?"":path.substring(idx+1); }

    protected record TsFixtureFile(String path, String source, SyntaxNode root, String language, Set<String> technologies) {}
}
