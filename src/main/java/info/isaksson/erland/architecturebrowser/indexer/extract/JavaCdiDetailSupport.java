package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaCdiDetailSupport {
    record PublishedEvent(String eventType, boolean async, String publisherField) {}
    record ObservedEvent(String eventType, boolean async, List<String> qualifiers) {}
    private record InjectedEventField(String fieldName, String eventType) {}

    List<PublishedEvent> detectPublishedEvents(String snippet, String ownerTypeSnippet) {
        List<PublishedEvent> result = new ArrayList<>();
        for (InjectedEventField field : detectInjectedEventFields(ownerTypeSnippet)) {
            Matcher sync = Pattern.compile("\\b" + Pattern.quote(field.fieldName()) + "\\s*\\.\\s*fire\\s*\\(").matcher(snippet == null ? "" : snippet);
            while (sync.find()) result.add(new PublishedEvent(field.eventType(), false, field.fieldName()));
            Matcher async = Pattern.compile("\\b" + Pattern.quote(field.fieldName()) + "\\s*\\.\\s*fireAsync\\s*\\(").matcher(snippet == null ? "" : snippet);
            while (async.find()) result.add(new PublishedEvent(field.eventType(), true, field.fieldName()));
        }
        return List.copyOf(result);
    }

    Optional<ObservedEvent> detectObservedEvent(ExtractedEntityFact methodEntity, String snippet) {
        String parameters = String.valueOf(methodEntity.metadata().getOrDefault("parameters", ""));
        if (parameters == null || parameters.isBlank() || "()".equals(parameters.strip())) {
            parameters = snippet;
        }
        if (parameters == null || parameters.isBlank()) {
            return Optional.empty();
        }

        String normalized = parameters.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.startsWith("(") && normalized.endsWith(")")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        for (String parameter : normalized.split(",")) {
            String candidate = parameter == null ? "" : parameter.trim();
            if (candidate.isBlank() || (!candidate.contains("@Observes") && !candidate.contains("@ObservesAsync"))) {
                continue;
            }

            boolean async = candidate.contains("@ObservesAsync") || candidate.contains("ObservesAsync");
            java.util.LinkedHashSet<String> qualifiers = new java.util.LinkedHashSet<>(
                    SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(candidate)
            );
            qualifiers.removeIf(v -> v == null || v.endsWith("Observes") || v.endsWith("ObservesAsync"));

            String stripped = candidate
                    .replaceAll("@[A-Za-z_][\\w.]*\\s*(?:\\([^)]*\\))?", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (stripped.isBlank()) {
                continue;
            }

            Matcher typeMatcher = Pattern.compile("([A-Za-z_$][\\w.$<>]*)\\s+[A-Za-z_$][\\w$]*$").matcher(stripped);
            if (!typeMatcher.find()) {
                continue;
            }

            return Optional.of(new ObservedEvent(typeMatcher.group(1).trim(), async, List.copyOf(qualifiers)));
        }
        return Optional.empty();
    }

    LinkedHashMap<String,Object> publisherRelationshipMetadata(String targetLabel, String publisherMethod, String ownerQualifiedName, PublishedEvent publication) { LinkedHashMap<String,Object> m = new LinkedHashMap<>(); m.put("framework","cdi"); m.put("relationshipType","publishesEvent"); m.put("frameworkRelationship","publishesEvent"); m.put("dependencySource","eventPublish"); m.put("eventType",targetLabel); m.put("publisherMethod",publisherMethod); m.put("publisherQualifiedName",ownerQualifiedName); m.put("publisherAsync",publication.async()); if (publication.publisherField()!=null) m.put("publisherField",publication.publisherField()); return m; }
    LinkedHashMap<String,Object> observerTypeRelationshipMetadata(String targetLabel, String ownerQualifiedName, String methodName, ObservedEvent observed) { LinkedHashMap<String,Object> m = new LinkedHashMap<>(); m.put("framework","cdi"); m.put("relationshipType","eventObservedBy"); m.put("frameworkRelationship","observesEvent"); m.put("eventType",targetLabel); m.put("observerQualifiedName",ownerQualifiedName); m.put("observerMethod",methodName); m.put("observerAsync",observed.async()); if (!observed.qualifiers().isEmpty()) m.put("observerQualifiers",observed.qualifiers()); return m; }
    LinkedHashMap<String,Object> observerMethodRelationshipMetadata(String targetLabel, String methodName, ObservedEvent observed) { LinkedHashMap<String,Object> m = new LinkedHashMap<>(); m.put("framework","cdi"); m.put("relationshipType","observesEvent"); m.put("frameworkRelationship","observesEvent"); m.put("eventType",targetLabel); m.put("observerAsync",observed.async()); m.put("ownerMemberKind","method"); m.put("ownerMemberName",methodName); if (!observed.qualifiers().isEmpty()) m.put("observerQualifiers",observed.qualifiers()); return m; }

    private static List<InjectedEventField> detectInjectedEventFields(String ownerTypeSnippet) { if (ownerTypeSnippet == null || ownerTypeSnippet.isBlank()) return List.of(); List<InjectedEventField> result = new ArrayList<>(); Matcher m = Pattern.compile("@(?:[A-Za-z_][\\w.]*\\.)?Inject\\s+(?:private|protected|public)?\\s*(?:transient\\s+)?(?:final\\s+)?(?:@[^;\\n]+\\s+)*Event(?:<\\s*([A-Za-z_$][\\w.$<>]*)\\s*>)?\\s+([A-Za-z_$][\\w$]*)", Pattern.DOTALL).matcher(ownerTypeSnippet); while (m.find()) result.add(new InjectedEventField(m.group(2), m.group(1).trim())); return List.copyOf(result); }
}
