package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.NormalizedAssociation;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrNormalizedAssociationCatalogSupportTest {
    @Test
    @SuppressWarnings("unchecked")
    void exportsCanonicalEntityAssociationCatalogAndFeedsJavaEntityModelView() {
        ArchitectureEntity project = new ArchitectureEntity(
            "entity:project",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "Project",
            "Project",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.Project")
        );
        ArchitectureEntity task = new ArchitectureEntity(
            "entity:task",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "Task",
            "Task",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.Task")
        );
        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(project.id(), project);
        entitiesById.put(task.id(), task);

        ArchitectureRelationship mergedAssociation = new ArchitectureRelationship(
            "rel:project-tasks",
            RelationshipKind.USES,
            project.id(),
            task.id(),
            "tasks",
            List.of(),
            Map.of(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "jpaAssociationHandling", "merged-bidirectional-peer-association"
            ),
            null,
            new NormalizedAssociation(
                "containment",
                "one-to-many",
                "1",
                "1",
                "0",
                "*",
                Boolean.TRUE,
                List.of("rel:project-tasks", "rel:task-project"),
                task.id(),
                "project",
                project.id(),
                "tasks"
            )
        );

        Map<String, Object> catalogs = ArchitectureIrNormalizedAssociationCatalogSupport.build(List.of(mergedAssociation), entitiesById);
        List<Map<String, Object>> entityAssociations = (List<Map<String, Object>>) catalogs.get("entityAssociationRelationships");
        assertEquals(1, entityAssociations.size());
        assertEquals("one-to-many", entityAssociations.getFirst().get("associationCardinality"));
        assertEquals(Boolean.TRUE, entityAssociations.getFirst().get("canonicalForEntityViews"));

        Map<String, Object> relationshipCatalogs = (Map<String, Object>) catalogs.get("relationshipCatalogs");
        Map<String, Object> entityAssociationCatalog = (Map<String, Object>) relationshipCatalogs.get("entityAssociations");
        assertEquals("entityAssociationRelationships", entityAssociationCatalog.get("id"));
        assertEquals(Boolean.TRUE, entityAssociationCatalog.get("available"));

        Map<String, Object> javaViews = ArchitectureIrJavaBrowserViewSupport.buildJavaBrowserViews(
            List.of(),
            List.of(),
            entityAssociations,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
        List<Map<String, Object>> views = (List<Map<String, Object>>) javaViews.get("views");
        Map<String, Object> entityModelView = views.stream()
            .filter(view -> "javaEntityModelGraph".equals(view.get("id")))
            .findFirst()
            .orElseThrow();
        assertEquals(Boolean.TRUE, entityModelView.get("available"));
        assertEquals("entityAssociationRelationships", entityModelView.get("relationshipCatalogView"));
        assertEquals("entityAssociationRelationships", entityModelView.get("preferredDependencyView"));
        assertEquals(Integer.valueOf(1), entityModelView.get("relationshipCatalogCount"));

        List<String> preferredViews = documentViewpointPreference(javaViews);
        assertTrue(preferredViews.contains("entityAssociationRelationships"));

        Map<String, Object> dependencyViews = new LinkedHashMap<>();
        dependencyViews.put("javaBrowserViews", javaViews);
        dependencyViews.put("entityAssociationRelationships", entityAssociations);
        assertTrue(ArchitectureIrJavaViewpointBridgeSupport.apply(List.of(), dependencyViews).stream()
            .anyMatch(viewpoint -> "persistence-model".equals(viewpoint.id())
                && viewpoint.preferredDependencyViews() != null
                && viewpoint.preferredDependencyViews().contains("entityAssociationRelationships")));

    }

    private static List<String> documentViewpointPreference(Map<String, Object> javaViews) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> views = (List<Map<String, Object>>) javaViews.get("views");
        return views.stream()
            .filter(view -> "javaEntityModelGraph".equals(view.get("id")))
            .map(view -> String.valueOf(view.get("preferredDependencyView")))
            .toList();
    }
}
