package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaCdiDomainSemanticsSupport {
    private JavaCdiDomainSemanticsSupport() {}

    record PublishedCdiEvent(String eventType, boolean async, String publisherField) {}
    record ObservedCdiEvent(String eventType, boolean async, List<String> qualifiers) {}

    static List<PublishedCdiEvent> detectCdiPublishedEvents(String methodSnippet, String ownerTypeSnippet) {
        if (methodSnippet == null || methodSnippet.isBlank()) {
            return List.of();
        }
        LinkedHashMap<String, PublishedCdiEvent> events = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("([A-Za-z_$][\\w$]*)\\s*\\.\\s*(fireAsync|fire)\\s*\\((.*?)\\)", Pattern.DOTALL).matcher(methodSnippet);
        while (matcher.find()) {
            String publisherField = matcher.group(1);
            boolean async = "fireAsync".equals(matcher.group(2));
            String args = matcher.group(3) == null ? "" : matcher.group(3);
            String eventType = extractCdiEventTypeFromField(ownerTypeSnippet, publisherField)
                .or(() -> extractCdiEventTypeFromArguments(args))
                .orElse(null);
            if (eventType == null || eventType.isBlank()) {
                continue;
            }
            events.putIfAbsent(publisherField + ":" + eventType + ":" + async, new PublishedCdiEvent(eventType, async, publisherField));
        }
        return List.copyOf(events.values());
    }

    static Optional<String> extractCdiEventTypeFromField(String ownerTypeSnippet, String publisherField) {
        if (ownerTypeSnippet == null || ownerTypeSnippet.isBlank() || publisherField == null || publisherField.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("(?:^|[;{}\\s])(?:@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?\\s*)*(?:public|protected|private)?\\s*(?:static\\s+|final\\s+|transient\\s+)*?(?:[A-Za-z_][\\w.]*\\.)?Event\\s*<\\s*([^>]+?)\\s*>\\s+" + Pattern.quote(publisherField) + "\\b", Pattern.DOTALL).matcher(ownerTypeSnippet);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1)).map(String::trim).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }

    static Optional<String> extractCdiEventTypeFromArguments(String args) {
        if (args == null || args.isBlank()) {
            return Optional.empty();
        }
        Matcher constructorMatcher = Pattern.compile("new\\s+([A-Za-z_$][\\w.$]*)\\b").matcher(args);
        if (constructorMatcher.find()) {
            return Optional.of(constructorMatcher.group(1));
        }
        return Optional.empty();
    }

    static Optional<ObservedCdiEvent> detectCdiObservedEvent(ExtractedEntityFact methodEntity, String snippet) {
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        if (parameters == null || parameters.isBlank() || "()".equals(parameters.strip())) {
            parameters = snippet;
        }
        if (parameters == null || parameters.isBlank()) {
            return Optional.empty();
        }
        String normalized = parameters.replace('\n', ' ').replace('\r', ' ');
        Matcher matcher = Pattern.compile("((?:@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?\\s*)*)@(?:[A-Za-z_][\\w.]*\\.)?(ObservesAsync|Observes)\\b(?:\\s*\\([^)]*\\))?\\s+([A-Za-z_$][\\w.$<>]*)").matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        boolean async = "ObservesAsync".equals(matcher.group(2));
        String eventType = matcher.group(3) == null ? "" : matcher.group(3).trim();
        String annotationPrefix = matcher.group(1) == null ? "" : matcher.group(1);
        java.util.LinkedHashSet<String> qualifiers = new java.util.LinkedHashSet<>(SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(annotationPrefix));
        qualifiers.removeIf(value -> value == null || value.endsWith("Observes") || value.endsWith("ObservesAsync"));
        return eventType.isBlank() ? Optional.empty() : Optional.of(new ObservedCdiEvent(eventType, async, List.copyOf(qualifiers)));
    }
}
