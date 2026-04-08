package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaJpaAssociationEvidenceSupportTest {
    @Test
    void extractsRichBidirectionalAssociationEvidenceFromJpaFieldSnippet() {
        var evidence = JavaJpaAssociationEvidenceSupport.extractFieldAssociationEvidence(
            List.of("OneToMany"),
            "java.util.Set<Task>",
            "@OneToMany(mappedBy = \"project\", cascade = CascadeType.ALL, orphanRemoval = true) private Set<Task> tasks = new LinkedHashSet<>();"
        ).orElseThrow();

        assertEquals("OneToMany", evidence.annotationSimpleName());
        assertEquals("one-to-many", evidence.associationKind());
        assertEquals("Task", evidence.targetType());
        assertTrue(evidence.collectionValued());
        assertEquals("project", evidence.mappedBy());
        assertTrue(evidence.cascadeAll());
        assertTrue(evidence.cascadeRemove());
        assertTrue(evidence.orphanRemoval());
        assertFalse(evidence.valueLikeTarget());
    }

    @Test
    void extractsValueLikeEvidenceForElementCollectionAndEmbeddedPatterns() {
        var elementCollection = JavaJpaAssociationEvidenceSupport.extractFieldAssociationEvidence(
            List.of("ElementCollection"),
            "java.util.Set<String>",
            "@ElementCollection private Set<String> tags;"
        ).orElseThrow();
        assertEquals("element-collection", elementCollection.associationKind());
        assertTrue(elementCollection.elementCollection());
        assertTrue(elementCollection.collectionValued());
        assertTrue(elementCollection.valueLikeTarget());
        assertFalse(elementCollection.peerEntityAssociation());
        assertEquals("value-collection", elementCollection.handlingCategory());

        var embedded = JavaJpaAssociationEvidenceSupport.extractMethodAssociationEvidence(
            List.of("Embedded", "MapsId"),
            "Address",
            "@Embedded @MapsId public Address getAddress() { return address; }",
            "address"
        ).orElseThrow();
        assertEquals("Embedded", embedded.annotationSimpleName());
        assertTrue(embedded.embedded());
        assertTrue(embedded.mapsId());
        assertTrue(embedded.propertyAccess());
        assertEquals("address", embedded.propertyName());
        assertTrue(embedded.valueLikeTarget());
        assertFalse(embedded.peerEntityAssociation());
        assertEquals("embedded-value", embedded.handlingCategory());
    }

    @Test
    void propertyAndRelationshipMetadataExposeEvidenceMap() {
        JavaJpaDetailSupport support = new JavaJpaDetailSupport();
        var details = support.analyzeField(
            new info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact(
                "f",
                info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind.FIELD,
                info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED,
                "project",
                "project",
                "scope:file",
                java.util.List.of(new info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference("x", 1, 1, "@ManyToOne(optional = false) @JoinColumn(name = \"project_id\", nullable = false) private Project project;", Map.of())),
                Map.of("annotations", List.of("ManyToOne"), "declaredType", "Project")
            ),
            "@ManyToOne(optional = false) @JoinColumn(name = \"project_id\", nullable = false) private Project project;"
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> evidence = (Map<String, Object>) details.metadata().get("jpaAssociationEvidence");
        assertEquals("many-to-one", evidence.get("associationKind"));
        assertEquals("project_id", evidence.get("joinColumn"));
        assertEquals(Boolean.FALSE, evidence.get("associationOptional"));
        assertEquals(Boolean.FALSE, evidence.get("joinColumnNullable"));
        assertEquals(Boolean.TRUE, evidence.get("peerEntityAssociation"));
        assertEquals(Boolean.TRUE, evidence.get("inverseMergeEligible"));
        assertEquals("peer-entity-association", evidence.get("handlingCategory"));
    }
}
