package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaJpaInverseAssociationMergeSupportTest {

    @Test
    void mergesBidirectionalManyToOneAndOneToManyIntoSingleCanonicalRelationship() {
        ArchitectureRelationship manyToOne = relationship(
            "rel:task-project",
            "entity:task",
            "entity:project",
            "project",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationKind", "association",
                "associationCardinality", "many-to-one",
                "targetLowerBound", "1",
                "targetUpperBound", "1",
                "sourceLowerBound", "0",
                "sourceUpperBound", "*",
                "ownerPropertyName", "project",
                "associationOptional", false,
                "joinColumnNullable", false
            )
        );
        ArchitectureRelationship oneToMany = relationship(
            "rel:project-tasks",
            "entity:project",
            "entity:task",
            "tasks",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationKind", "association",
                "associationCardinality", "one-to-many",
                "targetLowerBound", "0",
                "targetUpperBound", "*",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1",
                "ownerPropertyName", "tasks",
                "mappedBy", "project",
                "orphanRemoval", true,
                "cascadeAll", true,
                "cascadeRemove", true
            )
        );

        List<ArchitectureRelationship> merged = JavaJpaInverseAssociationMergeSupport.mergeInverseJpaAssociations(
            List.of(manyToOne, oneToMany),
            entityMapOf(
                entity("entity:project"),
                entity("entity:task")
            )
        );

        assertEquals(1, merged.size());
        ArchitectureRelationship canonical = merged.getFirst();
        assertEquals("rel:project-tasks", canonical.id());
        assertEquals("entity:project", canonical.fromEntityId());
        assertEquals("entity:task", canonical.toEntityId());
        assertNotNull(canonical.normalizedAssociation());
        assertEquals("one-to-many", canonical.normalizedAssociation().associationCardinality());
        assertEquals(Boolean.TRUE, canonical.normalizedAssociation().bidirectional());
        assertEquals(List.of("rel:project-tasks", "rel:task-project"), canonical.normalizedAssociation().evidenceRelationshipIds());
        assertEquals("entity:task", canonical.normalizedAssociation().owningSideEntityId());
        assertEquals("entity:project", canonical.normalizedAssociation().inverseSideEntityId());
        assertEquals("project", canonical.normalizedAssociation().owningSideMemberId());
        assertEquals("tasks", canonical.normalizedAssociation().inverseSideMemberId());
        assertEquals("1", canonical.normalizedAssociation().sourceLowerBound());
        assertEquals("1", canonical.normalizedAssociation().sourceUpperBound());
        assertEquals("0", canonical.normalizedAssociation().targetLowerBound());
        assertEquals("*", canonical.normalizedAssociation().targetUpperBound());
        assertEquals("containment", canonical.normalizedAssociation().associationKind());
        assertEquals("containment", canonical.metadata().get("associationKind"));
        assertEquals(Boolean.TRUE, canonical.metadata().get("containmentPromoted"));
        assertEquals("1", canonical.metadata().get("sourceLowerBound"));
        assertEquals("1", canonical.metadata().get("sourceUpperBound"));
        assertEquals("0", canonical.metadata().get("targetLowerBound"));
        assertEquals("*", canonical.metadata().get("targetUpperBound"));
        assertEquals(Boolean.TRUE, canonical.metadata().get("inverseJpaAssociationMerged"));
    }


    @Test
    void mergesBidirectionalManyToManyWhenMappedByMatchesOppositeMember() {
        ArchitectureRelationship owning = relationship(
            "rel:student-courses",
            "entity:student",
            "entity:course",
            "courses",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "many-to-many",
                "ownerPropertyName", "courses",
                "sourceLowerBound", "0",
                "sourceUpperBound", "*",
                "targetLowerBound", "0",
                "targetUpperBound", "*"
            )
        );
        ArchitectureRelationship inverse = relationship(
            "rel:course-students",
            "entity:course",
            "entity:student",
            "students",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "many-to-many",
                "ownerPropertyName", "students",
                "mappedBy", "courses",
                "sourceLowerBound", "0",
                "sourceUpperBound", "*",
                "targetLowerBound", "0",
                "targetUpperBound", "*"
            )
        );

        List<ArchitectureRelationship> merged = JavaJpaInverseAssociationMergeSupport.mergeInverseJpaAssociations(
            List.of(owning, inverse),
            Map.of()
        );

        assertEquals(1, merged.size());
        assertEquals("rel:student-courses", merged.getFirst().id());
        assertEquals("many-to-many", merged.getFirst().normalizedAssociation().associationCardinality());
        assertEquals(List.of("rel:course-students", "rel:student-courses"), merged.getFirst().normalizedAssociation().evidenceRelationshipIds());
    }



    @Test
    void promotesOneToOneToContainmentWhenRequiredOwnershipIsIdentityBound() {
        ArchitectureRelationship owning = relationship(
            "rel:task-details-task",
            "entity:taskDetails",
            "entity:task",
            "task",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationKind", "association",
                "associationCardinality", "one-to-one",
                "ownerPropertyName", "task",
                "associationOptional", false,
                "joinColumnNullable", false,
                "mapsId", true,
                "targetLowerBound", "1",
                "targetUpperBound", "1",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1"
            )
        );
        ArchitectureRelationship inverse = relationship(
            "rel:task-task-details",
            "entity:task",
            "entity:taskDetails",
            "taskDetails",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationKind", "association",
                "associationCardinality", "one-to-one",
                "ownerPropertyName", "taskDetails",
                "mappedBy", "task",
                "targetLowerBound", "0",
                "targetUpperBound", "1",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1"
            )
        );

        List<ArchitectureRelationship> merged = JavaJpaInverseAssociationMergeSupport.mergeInverseJpaAssociations(
            List.of(owning, inverse),
            Map.of()
        );

        assertEquals(1, merged.size());
        assertEquals("containment", merged.getFirst().normalizedAssociation().associationKind());
        assertEquals("containment", merged.getFirst().metadata().get("associationKind"));
    }

    @Test
    void doesNotPromoteToContainmentWhenRequiredOwnershipIsMissingEvenIfInverseLifecycleHintsExist() {
        ArchitectureRelationship manyToOne = relationship(
            "rel:task-project",
            "entity:task",
            "entity:project",
            "project",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationKind", "association",
                "associationCardinality", "many-to-one",
                "targetLowerBound", "0",
                "targetUpperBound", "1",
                "sourceLowerBound", "0",
                "sourceUpperBound", "*",
                "ownerPropertyName", "project"
            )
        );
        ArchitectureRelationship oneToMany = relationship(
            "rel:project-tasks",
            "entity:project",
            "entity:task",
            "tasks",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationKind", "association",
                "associationCardinality", "one-to-many",
                "targetLowerBound", "0",
                "targetUpperBound", "*",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1",
                "ownerPropertyName", "tasks",
                "mappedBy", "project",
                "orphanRemoval", true,
                "cascadeAll", true,
                "cascadeRemove", true
            )
        );

        List<ArchitectureRelationship> merged = JavaJpaInverseAssociationMergeSupport.mergeInverseJpaAssociations(
            List.of(manyToOne, oneToMany),
            Map.of()
        );

        assertEquals(1, merged.size());
        assertEquals("association", merged.getFirst().normalizedAssociation().associationKind());
    }

    @Test
    void derivesConservativeOptionalBoundsWhenInversePairHasConflictingLowerBoundSignals() {
        ArchitectureRelationship manyToOne = relationship(
            "rel:task-project",
            "entity:task",
            "entity:project",
            "project",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "many-to-one",
                "targetLowerBound", "0",
                "targetUpperBound", "1",
                "sourceLowerBound", "0",
                "sourceUpperBound", "*",
                "ownerPropertyName", "project"
            )
        );
        ArchitectureRelationship oneToMany = relationship(
            "rel:project-tasks",
            "entity:project",
            "entity:task",
            "tasks",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "one-to-many",
                "targetLowerBound", "0",
                "targetUpperBound", "*",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1",
                "ownerPropertyName", "tasks",
                "mappedBy", "project"
            )
        );

        List<ArchitectureRelationship> merged = JavaJpaInverseAssociationMergeSupport.mergeInverseJpaAssociations(
            List.of(manyToOne, oneToMany),
            Map.of()
        );

        assertEquals(1, merged.size());
        ArchitectureRelationship canonical = merged.getFirst();
        assertEquals("0", canonical.normalizedAssociation().sourceLowerBound());
        assertEquals("1", canonical.normalizedAssociation().sourceUpperBound());
        assertEquals("0", canonical.normalizedAssociation().targetLowerBound());
        assertEquals("*", canonical.normalizedAssociation().targetUpperBound());
    }

    @Test
    void leavesNonMatchingSameEntityAssociationsUnmerged() {
        ArchitectureRelationship manyToOne = relationship(
            "rel:task-project",
            "entity:task",
            "entity:project",
            "project",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "many-to-one",
                "ownerPropertyName", "project"
            )
        );
        ArchitectureRelationship oneToMany = relationship(
            "rel:project-archived-tasks",
            "entity:project",
            "entity:task",
            "archivedTasks",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "one-to-many",
                "ownerPropertyName", "archivedTasks",
                "mappedBy", "archivedFromProject"
            )
        );

        List<ArchitectureRelationship> merged = JavaJpaInverseAssociationMergeSupport.mergeInverseJpaAssociations(
            List.of(manyToOne, oneToMany),
            Map.of()
        );

        assertEquals(2, merged.size());
        assertTrue(merged.stream().allMatch(relationship -> relationship.normalizedAssociation() == null));
    }

    @Test
    void normalizationServiceAppliesJpaInverseMergeAfterRuleNormalization() {
        ArchitectureRelationship manyToOne = relationship(
            "rel:task-project",
            "entity:task",
            "entity:project",
            "project",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "many-to-one",
                "ownerPropertyName", "project"
            )
        );
        ArchitectureRelationship oneToMany = relationship(
            "rel:project-tasks",
            "entity:project",
            "entity:task",
            "tasks",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "one-to-many",
                "ownerPropertyName", "tasks",
                "mappedBy", "project",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1",
                "targetLowerBound", "0",
                "targetUpperBound", "*"
            )
        );

        ArchitectureRelationshipNormalizationService service = ArchitectureRelationshipNormalizationService.of(List.of(
            context -> new NormalizedArchitectureRelationship(List.of(" accesses-persistence "))
        ));

        List<ArchitectureRelationship> merged = service.normalizeRelationships(
            List.of(manyToOne, oneToMany),
            entityMapOf(
                entity("entity:project"),
                entity("entity:task")
            )
        );

        assertEquals(1, merged.size());
        assertEquals(List.of("accesses-persistence"), merged.getFirst().architecturalSemantics());
        assertNotNull(merged.getFirst().normalizedAssociation());
    }

    @Test
    void keepsUnidirectionalPeerAssociationsExplicitWithoutSyntheticInverseMerge() {
        ArchitectureRelationship oneToMany = relationship(
            "rel:project-tasks",
            "entity:project",
            "entity:task",
            "tasks",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationKind", "association",
                "associationCardinality", "one-to-many",
                "targetLowerBound", "0",
                "targetUpperBound", "*",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1",
                "ownerPropertyName", "tasks"
            )
        );

        List<ArchitectureRelationship> merged = JavaJpaInverseAssociationMergeSupport.mergeInverseJpaAssociations(
            List.of(oneToMany),
            Map.of()
        );

        assertEquals(1, merged.size());
        ArchitectureRelationship handled = merged.getFirst();
        assertNotNull(handled.normalizedAssociation());
        assertEquals(Boolean.FALSE, handled.normalizedAssociation().bidirectional());
        assertEquals(List.of("rel:project-tasks"), handled.normalizedAssociation().evidenceRelationshipIds());
        assertEquals("unidirectional-peer-association", handled.metadata().get("jpaAssociationHandling"));
        assertEquals(Boolean.TRUE, handled.metadata().get("jpaAssociationPeerEntity"));
        assertEquals(Boolean.TRUE, handled.metadata().get("jpaAssociationUnidirectional"));
    }

    @Test
    void leavesExplicitNonPeerJpaValueLikeRelationshipsOutsidePeerAssociationNormalization() {
        ArchitectureRelationship valueLike = relationship(
            "rel:order-tags",
            "entity:order",
            "entity:tagValue",
            "tags",
            metadataMapOf(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "one-to-many",
                "ownerPropertyName", "tags",
                "jpaAssociationEvidence", metadataMapOf(
                    "valueLikeTarget", true,
                    "elementCollection", true,
                    "handlingCategory", "value-collection"
                )
            )
        );

        List<ArchitectureRelationship> merged = JavaJpaInverseAssociationMergeSupport.mergeInverseJpaAssociations(
            List.of(valueLike),
            Map.of()
        );

        assertEquals(1, merged.size());
        ArchitectureRelationship handled = merged.getFirst();
        assertEquals("value-collection", handled.metadata().get("jpaAssociationHandling"));
        assertEquals(Boolean.FALSE, handled.metadata().get("jpaAssociationPeerEntity"));
        assertEquals(Boolean.TRUE, handled.metadata().get("jpaNonPeerAssociation"));
        assertEquals(null, handled.normalizedAssociation());
    }

    private static ArchitectureRelationship relationship(String id, String fromEntityId, String toEntityId, String label, Map<String, Object> metadata) {
        return new ArchitectureRelationship(
            id,
            RelationshipKind.DEPENDS_ON,
            fromEntityId,
            toEntityId,
            label,
            List.of(),
            metadata,
            null,
            null
        );
    }

    private static ArchitectureEntity entity(String id) {
        return new ArchitectureEntity(
            id,
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            id,
            id,
            "scope:repo",
            List.of(),
            metadataMapOf("jpaEntity", true),
            null,
            null
        );
    }

private static Map<String, Object> metadataMapOf(Object... entries) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < entries.length; i += 2) {
        map.put((String) entries[i], entries[i + 1]);
    }
    return map;
}


private static Map<String, ArchitectureEntity> entityMapOf(ArchitectureEntity... entities) {
    Map<String, ArchitectureEntity> map = new LinkedHashMap<>();
    for (ArchitectureEntity entity : entities) {
        map.put(entity.id(), entity);
    }
    return map;
}

}
