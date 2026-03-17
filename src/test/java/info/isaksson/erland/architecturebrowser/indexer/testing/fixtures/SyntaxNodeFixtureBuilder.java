package info.isaksson.erland.architecturebrowser.indexer.testing.fixtures;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;

public final class SyntaxNodeFixtureBuilder {
    private SyntaxNodeFixtureBuilder() {}

    public static SyntaxNode program(String source, SyntaxNode... children) {
        int[] end = lineAndColumn(source, source.length());
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, end[0], end[1], false, false, source, List.of(children));
    }

    public static SyntaxNode node(String type, String source, String snippet, List<SyntaxNode> children) {
        int start = source.indexOf(snippet);
        if (start < 0) throw new IllegalArgumentException("Snippet not found: " + snippet);
        int end = start + snippet.length();
        int[] from = lineAndColumn(source, start);
        int[] to = lineAndColumn(source, end);
        return new SyntaxNode(type, true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, children);
    }

    public static SyntaxNode localNode(String source, String type, String snippet, List<SyntaxNode> children) {
        return node(type, source, snippet, children);
    }

    public static SyntaxNode localLeaf(String source, String type, String text) {
        return localNode(source, type, text, List.of());
    }

    public static SyntaxNode classDeclaration(String source, String name, List<SyntaxNode> extraChildren) {
        int startIndex = source.indexOf(name) >= 0 ? Math.max(0, source.lastIndexOf("class", source.indexOf(name))) : 0;
        int endIndex = source.length();
        int[] from = lineAndColumn(source, startIndex);
        int[] to = lineAndColumn(source, endIndex);
        java.util.ArrayList<SyntaxNode> children = new java.util.ArrayList<>();
        children.add(localLeaf(source, "type_identifier", name));
        children.addAll(extraChildren);
        return new SyntaxNode("class_declaration", true, startIndex, endIndex, from[0], from[1], to[0], to[1], false, false, source, List.copyOf(children));
    }

    public static int[] lineAndColumn(String source, int offset) {
        int line = 0;
        int column = 0;
        for (int i = 0; i < offset && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }

    public static String normalize(String text) {
        return text.stripIndent().strip() + "\n";
    }

    public static String between(String text, char start, char end) {
        int from = text.indexOf(start);
        int to = text.indexOf(end, from + 1);
        if (from < 0 || to < 0) return "()";
        return text.substring(from, to + 1);
    }

    public static String declaredType(String snippet) {
        int colon = snippet.indexOf(':');
        if (colon < 0) return "";
        String tail = snippet.substring(colon + 1).trim();
        if (tail.endsWith(";")) tail = tail.substring(0, tail.length() - 1).trim();
        return tail;
    }
}
