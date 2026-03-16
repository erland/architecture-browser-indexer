package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

final class JavaSourceReferenceSupport {

    private JavaSourceReferenceSupport() {}

    static SourceReference primaryReference(
        String relativePath,
        SyntaxNode node,
        String kind,
        java.util.List<SourceReference> existingRefs,
        String snippet
    ) {
        if (existingRefs != null && !existingRefs.isEmpty()) {
            return existingRefs.getFirst();
        }
        return ExtractionSupport.sourceRef(
            relativePath,
            SyntaxTreeExtractionSupport.oneBasedLine(node),
            snippet,
            Map.of("language", "java", "kind", kind == null || kind.isBlank() ? fallbackKind(node) : kind)
        );
    }

    static int lineOf(SourceReference ref, SyntaxNode fallbackNode) {
        return ref != null && ref.startLine() != null ? ref.startLine() : SyntaxTreeExtractionSupport.oneBasedLine(fallbackNode);
    }

    static String exactNodeSnippet(String sourceText, SyntaxNode node) {
        if (sourceText == null || sourceText.isBlank() || node == null) {
            return null;
        }
        int start = Math.max(0, Math.min(node.startByte(), sourceText.length()));
        int end = Math.max(start, Math.min(node.endByte(), sourceText.length()));
        if (start >= end) {
            return null;
        }
        String snippet = sourceText.substring(start, end);
        return snippet.isBlank() ? null : snippet;
    }

    private static String fallbackKind(SyntaxNode node) {
        return node == null ? "java-node" : node.type();
    }
}
