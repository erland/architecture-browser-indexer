package info.isaksson.erland.architecturebrowser.indexer.testing;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ArchitectureContractAssertions {

    private ArchitectureContractAssertions() {
    }


    public static void assertHasEndpoint(ExtractedEntityFact entity, String httpMethod, String path) {
        assertTrue(Boolean.TRUE.equals(entity.metadata().get("jaxRsEndpoint"))
                && httpMethod.equals(entity.metadata().get("httpMethod"))
                && path.equals(entity.metadata().get("path")),
            () -> "Expected endpoint with httpMethod=" + httpMethod + ", path=" + path + ". Entity=" + entity);
    }

    public static void assertHasRoute(List<Map<String, Object>> routes, String path, String routeType) {
        assertTrue(routes.stream().anyMatch(route ->
                path.equals(route.get("path"))
                    && (routeType == null || routeType.equals(route.get("routeType")))),
            () -> "Expected route path=" + path + ", routeType=" + routeType + ". Routes=" + routes);
    }

    public static void assertHasFrameworkDependencyView(List<Map<String, Object>> dependencies, String frameworkRelationship) {
        assertTrue(dependencies.stream().anyMatch(dep -> {
                Object fr = dep.get("frameworkRelationships");
                return fr instanceof List<?> list && list.stream().map(String::valueOf).anyMatch(frameworkRelationship::equals);
            }),
            () -> "Expected framework dependency view relationship='" + frameworkRelationship + "'. Dependencies=" + dependencies);
    }

    public static void assertHasEntityKind(List<ExtractedEntityFact> entities, String name, String kind) {
        assertTrue(entities.stream().anyMatch(entity ->
                kind.equals(entity.kind().name()) && name.equals(entity.name())),
            () -> "Expected entity kind=" + kind + " with name='" + name + "'. Entities=" + entities);
    }

    public static void assertContainsViews(Object actualViews, String... expectedViews) {
        List<String> actual = asStringList(actualViews);
        for (String expected : expectedViews) {
            assertTrue(actual.contains(expected),
                () -> "Expected view '" + expected + "' to be present. Actual views=" + actual);
        }
    }

    public static void assertHasPackageDependency(List<Map<String, Object>> dependencies, String sourcePackageName, String targetPackageName) {
        assertTrue(dependencies.stream().anyMatch(dep ->
                sourcePackageName.equals(dep.get("sourcePackageName"))
                    && targetPackageName.equals(dep.get("targetPackageName"))),
            () -> "Expected package dependency " + sourcePackageName + " -> " + targetPackageName + ". packageDependencies=" + dependencies);
    }

    public static void assertPublishesEvent(List<ExtractedRelationshipFact> relationships, String eventType, String publisherMethod) {
        assertTrue(relationships.stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "publishesEvent".equals(rel.metadata().get("relationshipType"))
                    && eventType.equals(rel.metadata().get("eventType"))
                    && publisherMethod.equals(rel.metadata().get("publisherMethod"))),
            () -> "Expected CDI publish relationship for eventType=" + eventType + ", publisherMethod=" + publisherMethod + ". Relationships=" + relationships);
    }

    public static void assertObservesEvent(List<ExtractedRelationshipFact> relationships, String eventType, String observerMethod, Boolean observerAsync) {
        assertTrue(relationships.stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "eventObservedBy".equals(rel.metadata().get("relationshipType"))
                    && eventType.equals(rel.metadata().get("eventType"))
                    && observerMethod.equals(rel.metadata().get("observerMethod"))
                    && (observerAsync == null || observerAsync.equals(rel.metadata().get("observerAsync")))),
            () -> "Expected CDI observer relationship for eventType=" + eventType + ", observerMethod=" + observerMethod + ", observerAsync=" + observerAsync + ". Relationships=" + relationships);
    }


    public static void assertHasRelationshipKind(List<ExtractedRelationshipFact> relationships, RelationshipKind kind, String relationshipType) {
        assertTrue(relationships.stream().anyMatch(rel ->
                rel.kind() == kind && Objects.equals(relationshipType, rel.metadata().get("relationshipType"))),
            () -> "Expected relationship kind=" + kind + " with relationshipType='" + relationshipType + "'. Relationships=" + relationships);
    }

    public static void assertHasRelationshipByLabel(List<ExtractedRelationshipFact> relationships, RelationshipKind kind, String label) {
        assertTrue(relationships.stream().anyMatch(rel -> rel.kind() == kind && label.equals(rel.label())),
            () -> "Expected relationship kind=" + kind + " with label='" + label + "'. Relationships=" + relationships);
    }

    public static void assertDependencyViewRelationship(List<ArchitectureRelationship> relationships, String dependencyView, String dependencySource) {
        assertTrue(relationships.stream().anyMatch(relationship ->
                relationship.kind() == RelationshipKind.DEPENDS_ON
                    && dependencyView.equals(relationship.metadata().get("dependencyView"))
                    && dependencySource.equals(relationship.metadata().get("dependencySource"))),
            () -> "Expected dependency relationship with dependencyView=" + dependencyView + ", dependencySource=" + dependencySource + ". Relationships=" + relationships);
    }

    private static List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
