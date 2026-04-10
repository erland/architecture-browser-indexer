package info.isaksson.erland.architecturebrowser.indexer.ir;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrBrowserMetadataBridgeModularizationTest {

    @Test
    void javaDescriptorFactoryCapturesRelationshipCatalogMetadata() {
        ArchitectureIrBrowserViewDescriptorFactory.JavaBrowserViewDefinition definition =
            new ArchitectureIrBrowserViewDescriptorFactory.JavaBrowserViewDefinition(
                "javaEntityModelGraph",
                "Java entity model graph",
                "desc",
                "jpa",
                "entity-model",
                "entityModelTypeDependencies",
                "entityModelModuleDependencies",
                "entityAssociationRelationships",
                List.of("hasAssociation"),
                List.of(Map.of("relationshipKind", "DEPENDS_ON", "frameworks", List.of("jpa"), "architectureViewKinds", List.of("entity-model"))),
                List.of(),
                List.of(Map.of("associationKind", "aggregation", "associationCardinality", "one-to-many"))
            );

        Map<String, Object> metadata = definition.toMetadataMap();
        assertEquals("entityAssociationRelationships", metadata.get("preferredDependencyView"));
        assertEquals("graph-with-relationship-catalog", metadata.get("browserViewKind"));
        assertEquals(1, metadata.get("relationshipCatalogCount"));
        assertEquals(List.of("aggregation"), metadata.get("relationshipAssociationKinds"));
    }

    @Test
    void bridgePolicyExposesStablePreferredDependencyViews() {
        Map<String, Object> javaView = Map.of(
            "available", true,
            "preferredDependencyView", "endpointTypeDependencies",
            "typeDependencyView", "endpointTypeDependencies",
            "moduleDependencyView", "endpointModuleDependencies",
            "typeDependencyCount", 1,
            "moduleDependencyCount", 1
        );
        Map<String, Object> dependencyViews = Map.of(
            "endpointTypeDependencies", List.of(Map.of()),
            "endpointModuleDependencies", List.of(Map.of())
        );

        assertEquals(
            List.of("endpointModuleDependencies", "endpointTypeDependencies"),
            ArchitectureIrJavaViewpointBridgePolicy.preferredDependencyViews(javaView, dependencyViews)
        );
        assertTrue(ArchitectureIrJavaViewpointBridgePolicy.bridgeEvidenceSources(javaView, dependencyViews).contains("java-dependency-views"));
    }
}
