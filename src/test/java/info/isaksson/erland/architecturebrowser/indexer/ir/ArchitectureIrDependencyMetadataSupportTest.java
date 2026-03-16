package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArchitectureIrDependencyMetadataSupportTest {

    @Test
    void shapesImportEvidenceMetadataWithStableFlagsAndNames() {
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "r1",
            RelationshipKind.DEPENDS_ON,
            "source",
            "target",
            "imports",
            List.of(),
            Map.of("dependencySource", "typescript:import")
        );
        ArchitectureEntity source = new ArchitectureEntity("source", EntityKind.MODULE, EntityOrigin.OBSERVED, "orders.module", "Orders", "repo", List.of(), Map.of());
        ArchitectureEntity target = new ArchitectureEntity("target", EntityKind.CLASS, EntityOrigin.OBSERVED, "OrderService", "OrderService", "repo", List.of(), Map.of());

        Map<String, Object> metadata = ArchitectureIrDependencyMetadataSupport.shapeImportEvidenceMetadata(
            relationship,
            source,
            target,
            new LinkedHashMap<>(relationship.metadata()),
            true,
            false,
            "internal",
            "service"
        );

        assertEquals("evidence", metadata.get("dependencyView"));
        assertEquals("supporting-evidence", metadata.get("dependencyTier"));
        assertEquals("file-import", metadata.get("evidenceKind"));
        assertEquals("orders.module", metadata.get("evidenceSourceName"));
        assertEquals("OrderService", metadata.get("evidenceTargetName"));
        assertFalse((Boolean) metadata.get("recommendedForArchitectureViews"));
    }

    @Test
    void putsSummaryCollectionsAsImmutableLists() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        Set<String> dependencySources = new LinkedHashSet<>(Set.of("java:type"));
        Set<String> dependencyCategories = new LinkedHashSet<>(Set.of("inheritance"));
        Set<String> frameworks = new LinkedHashSet<>(Set.of("jax-rs"));
        Set<String> frameworkRelationships = new LinkedHashSet<>(Set.of("endpoint"));
        Set<String> architectureViewKinds = new LinkedHashSet<>(Set.of("endpoint"));
        Set<String> evidenceRelationshipIds = new LinkedHashSet<>(Set.of("r1"));
        Set<String> evidenceLabels = new LinkedHashSet<>(Set.of("calls"));

        Map<String, Object> shaped = ArchitectureIrDependencyMetadataSupport.putSummaryCollections(
            metadata,
            dependencySources,
            dependencyCategories,
            frameworks,
            frameworkRelationships,
            architectureViewKinds,
            evidenceRelationshipIds,
            evidenceLabels
        );

        assertEquals(List.of("java:type"), shaped.get("dependencySources"));
        assertEquals(List.of("inheritance"), shaped.get("dependencyCategories"));
        assertEquals(List.of("jax-rs"), shaped.get("frameworks"));
        assertEquals(List.of("endpoint"), shaped.get("frameworkRelationships"));
        assertEquals(List.of("endpoint"), shaped.get("architectureViewKinds"));
        assertEquals(List.of("r1"), shaped.get("evidenceRelationshipIds"));
        assertEquals(List.of("calls"), shaped.get("evidenceLabels"));
    }
}
