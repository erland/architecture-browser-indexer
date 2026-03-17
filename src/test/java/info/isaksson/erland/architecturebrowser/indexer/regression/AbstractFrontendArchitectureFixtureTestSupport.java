package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.testing.fixtures.SyntaxNodeFixtureBuilder;
import info.isaksson.erland.architecturebrowser.indexer.testing.fixtures.TypeScriptArchitectureDocumentFixtureBuilder;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

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
        return TypeScriptArchitectureDocumentFixtureBuilder.buildDocument(
            files.stream().map(file -> new TypeScriptArchitectureDocumentFixtureBuilder.TypeScriptFixtureFile(file.path(), file.source(), file.root(), file.language(), file.technologies())).toList()
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
        return SyntaxNodeFixtureBuilder.program(source, children);
    }

    protected static SyntaxNode importStatement(String source, String snippet) { return SyntaxNodeFixtureBuilder.node("import_statement", source, snippet, List.of()); }

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
        return SyntaxNodeFixtureBuilder.node(type, source, snippet, children);
    }

    private static SyntaxNode localNode(String source, String type, String snippet, List<SyntaxNode> children) {
        int start = source.indexOf(snippet);
        if (start < 0) throw new IllegalArgumentException("Snippet not found in local source: " + snippet);
        int end = start + snippet.length();
        int[] from = lineAndColumn(source, start);
        int[] to = lineAndColumn(source, end);
        return SyntaxNodeFixtureBuilder.node(type, source, snippet, children);
    }

    private static SyntaxNode localLeaf(String source, String type, String text) { return SyntaxNodeFixtureBuilder.localLeaf(source, type, text); }

    private static int[] lineAndColumn(String source, int offset) {
        int line=0,column=0;
        for (int i=0;i<offset && i<source.length();i++) {
            if (source.charAt(i)=='\n') { line++; column=0; } else { column++; }
        }
        return new int[]{line,column};
    }

    private static String between(String text, char start, char end) { return SyntaxNodeFixtureBuilder.between(text, start, end); }

    private static String declaredType(String snippet) { return SyntaxNodeFixtureBuilder.declaredType(snippet); }

    private static String normalize(String text) { return text.stripIndent().strip() + "\n"; }
    private static String extension(String path) { int idx=path.lastIndexOf('.'); return idx<0?"":path.substring(idx+1); }

    protected record TsFixtureFile(String path, String source, SyntaxNode root, String language, Set<String> technologies) {}
}
