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




    public static void assertHasTypeDependency(List<Map<String, Object>> dependencies, String sourceTypeName, String targetTypeName, String dependencySource) {
        assertTrue(dependencies.stream().anyMatch(dep ->
                sourceTypeName.equals(dep.get("sourceTypeName"))
                    && targetTypeName.equals(dep.get("targetTypeName"))
                    && (dependencySource == null || listContains(dep.get("dependencySources"), dependencySource))),
            () -> "Expected type dependency " + sourceTypeName + " -> " + targetTypeName + (dependencySource == null ? "" : " via " + dependencySource) + ". Dependencies=" + dependencies);
    }

    public static void assertHasExternalDependencyTarget(List<Map<String, Object>> dependencies, String targetName, String dependencySource) {
        assertTrue(dependencies.stream().anyMatch(dep ->
                targetName.equals(dep.get("targetName"))
                    && Boolean.TRUE.equals(dep.get("externalTarget"))
                    && (dependencySource == null || listContains(dep.get("dependencySources"), dependencySource))),
            () -> "Expected external dependency target='" + targetName + "'" + (dependencySource == null ? "" : " via " + dependencySource) + ". Dependencies=" + dependencies);
    }

    public static void assertHasBrowserViewIds(List<Map<String, Object>> views, String... expectedIds) {
        List<String> actual = views.stream().map(view -> String.valueOf(view.get("id"))).toList();
        for (String expected : expectedIds) {
            assertTrue(actual.contains(expected),
                () -> "Expected browser view id='" + expected + "'. Actual ids=" + actual);
        }
    }

    public static void assertHasBrowserViewDescriptor(List<Map<String, Object>> views,
                                                      String id,
                                                      String framework,
                                                      String typeDependencyView,
                                                      String moduleDependencyView,
                                                      String frameworkRelationship) {
        assertTrue(views.stream().anyMatch(view ->
                id.equals(view.get("id"))
                    && framework.equals(view.get("framework"))
                    && typeDependencyView.equals(view.get("typeDependencyView"))
                    && moduleDependencyView.equals(view.get("moduleDependencyView"))
                    && Boolean.TRUE.equals(view.get("available"))
                    && listContains(view.get("frameworkRelationships"), frameworkRelationship)),
            () -> "Expected browser view descriptor id='" + id + "', framework='" + framework + "', typeDependencyView='" + typeDependencyView + "', moduleDependencyView='" + moduleDependencyView + "', frameworkRelationship='" + frameworkRelationship + "'. Views=" + views);
    }

    public static void assertHasUiModuleProfile(List<?> entities, String name, String profile) {
        assertTrue(entities.stream().anyMatch(raw -> {
                if (!(raw instanceof info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity entity)) {
                    return false;
                }
                return entity.kind().name().equals("UI_MODULE")
                    && name.equals(entity.name())
                    && profile.equals(entity.metadata().get("uiProfile"));
            }),
            () -> "Expected UI_MODULE '" + name + "' with uiProfile='" + profile + "'. Entities=" + entities);
    }

    public static void assertHasWritePathRelationship(List<ExtractedRelationshipFact> relationships, String writeOperation, String entityType) {
        assertTrue(relationships.stream().anyMatch(rel ->
                rel.kind() == RelationshipKind.DEPENDS_ON
                    && "writePath".equals(rel.metadata().get("relationshipType"))
                    && writeOperation.equals(rel.metadata().get("writeOperation"))
                    && entityType.equals(rel.metadata().get("entityType"))),
            () -> "Expected write-path relationship writeOperation='" + writeOperation + "', entityType='" + entityType + "'. Relationships=" + relationships);
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

    private static boolean listContains(Object value, String expected) {
        return value instanceof List<?> list && list.stream().map(String::valueOf).anyMatch(expected::equals);
    }

    private static List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
