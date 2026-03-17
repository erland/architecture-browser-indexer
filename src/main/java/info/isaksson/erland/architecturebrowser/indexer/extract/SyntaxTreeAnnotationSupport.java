package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SyntaxTreeAnnotationSupport {
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@([A-Za-z_][\\w.$]*)");
    private static final Set<String> MODIFIERS = Set.of(
        "public", "private", "protected", "static", "final", "abstract", "native", "synchronized", "strictfp", "default",
        "export", "readonly", "async", "declare", "override", "const"
    );

    private SyntaxTreeAnnotationSupport() {
    }

    static List<String> extractAnnotationsFromSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return List.of();
        }
        Matcher matcher = ANNOTATION_PATTERN.matcher(snippet);
        LinkedHashSet<String> annotations = new LinkedHashSet<>();
        while (matcher.find()) {
            annotations.add(annotationName(matcher.group(1)));
        }
        return List.copyOf(annotations);
    }

    static boolean isFrameworkAnnotation(String annotationText) {
        if (annotationText == null || annotationText.isBlank()) {
            return false;
        }
        String normalized = annotationName(annotationText).toLowerCase(Locale.ROOT);
        return normalized.startsWith("component")
            || normalized.startsWith("directive")
            || normalized.startsWith("pipe")
            || normalized.startsWith("injectable")
            || normalized.startsWith("ngmodule")
            || normalized.startsWith("path")
            || normalized.startsWith("get")
            || normalized.startsWith("post")
            || normalized.startsWith("put")
            || normalized.startsWith("delete")
            || normalized.startsWith("manytoone")
            || normalized.startsWith("onetomany")
            || normalized.startsWith("onetoone")
            || normalized.startsWith("manytomany")
            || normalized.startsWith("entity")
            || normalized.startsWith("embeddable")
            || normalized.startsWith("mappedsuperclass")
            || normalized.startsWith("embedded")
            || normalized.startsWith("column")
            || normalized.startsWith("id")
            || normalized.startsWith("version")
            || normalized.startsWith("joincolumn")
            || normalized.startsWith("jointable")
            || normalized.startsWith("table")
            || normalized.startsWith("inheritance")
            || normalized.startsWith("observes")
            || normalized.startsWith("observesasync")
            || normalized.startsWith("inject")
            || normalized.startsWith("provides")
            || normalized.startsWith("singleton")
            || normalized.startsWith("applicationscoped");
    }

    static boolean isModifierText(String text) {
        return text != null && MODIFIERS.contains(text.trim());
    }

    static Optional<String> findTypeName(SyntaxNode node, String... candidateTypes) {
        if (node == null || candidateTypes == null || candidateTypes.length == 0) {
            return Optional.empty();
        }
        Set<String> typeSet = Set.of(candidateTypes);
        List<SyntaxNode> candidates = new ArrayList<>();
        collectCandidates(node, typeSet, candidates);
        return candidates.stream()
            .map(SyntaxNode::textSnippet)
            .filter(text -> text != null && !text.isBlank())
            .map(String::trim)
            .findFirst();
    }

    private static void collectCandidates(SyntaxNode node, Set<String> types, List<SyntaxNode> matches) {
        for (SyntaxNode child : node.children()) {
            if (types.contains(child.type())) {
                matches.add(child);
            }
            collectCandidates(child, types, matches);
        }
    }

    private static String annotationName(String value) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.startsWith("@")) {
            candidate = candidate.substring(1);
        }
        int lastDot = candidate.lastIndexOf('.');
        return lastDot >= 0 ? candidate.substring(lastDot + 1) : candidate;
    }
}
