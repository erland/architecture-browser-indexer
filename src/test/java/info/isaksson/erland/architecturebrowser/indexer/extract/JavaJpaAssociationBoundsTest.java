package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JavaJpaAssociationBoundsTest {

    @Test
    void derivesMandatoryBoundsForManyToOneWhenJoinColumnIsNonNullable() {
        var bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "many-to-one",
            "@ManyToOne @JoinColumn(name = \"customer_id\", nullable = false) CustomerEntity customer;"
        );

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("*", bounds.get("sourceUpperBound"));
        assertEquals("1", bounds.get("targetLowerBound"));
        assertEquals("1", bounds.get("targetUpperBound"));
    }

    @Test
    void derivesOptionalBoundsForOneToOneWhenOptionalIsTrue() {
        var bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "one-to-one",
            "@OneToOne(optional = true) CustomerProfile profile;"
        );

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("1", bounds.get("sourceUpperBound"));
        assertEquals("0", bounds.get("targetLowerBound"));
        assertEquals("1", bounds.get("targetUpperBound"));
    }


    @Test
    void derivesMandatoryBoundsForManyToOneWhenAssociationOptionalIsFalse() {
        var bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "many-to-one",
            "@ManyToOne(optional = false) CustomerEntity customer;"
        );

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("*", bounds.get("sourceUpperBound"));
        assertEquals("1", bounds.get("targetLowerBound"));
        assertEquals("1", bounds.get("targetUpperBound"));
    }

    @Test
    void derivesOptionalBoundsForManyToOneWhenAssociationOptionalityConflictsWithJoinColumnNullability() {
        var bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "many-to-one",
            "@ManyToOne(optional = false) @JoinColumn(name = \"customer_id\", nullable = true) CustomerEntity customer;"
        );

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("*", bounds.get("sourceUpperBound"));
        assertEquals("0", bounds.get("targetLowerBound"));
        assertEquals("1", bounds.get("targetUpperBound"));
    }

    @Test
    void derivesOptionalBoundsForManyToOneWhenOptionalityEvidenceIsMissing() {
        var bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "many-to-one",
            "@ManyToOne CustomerEntity customer;"
        );

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("*", bounds.get("sourceUpperBound"));
        assertEquals("0", bounds.get("targetLowerBound"));
        assertEquals("1", bounds.get("targetUpperBound"));
    }

    @Test
    void derivesConservativeCollectionBoundsForOneToMany() {
        var bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "one-to-many",
            "@OneToMany(mappedBy = \"order\") List<OrderLineEntity> lines;"
        );

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("1", bounds.get("sourceUpperBound"));
        assertEquals("0", bounds.get("targetLowerBound"));
        assertEquals("*", bounds.get("targetUpperBound"));
    }

    @Test
    void derivesConservativeCollectionBoundsForManyToMany() {
        var bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "many-to-many",
            "@ManyToMany Set<TagEntity> tags;"
        );

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("*", bounds.get("sourceUpperBound"));
        assertEquals("0", bounds.get("targetLowerBound"));
        assertEquals("*", bounds.get("targetUpperBound"));
    }

    @Test
    void derivesMandatoryBoundsForOneToOneWhenOptionalIsFalse() {
        var bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "one-to-one",
            "@OneToOne(optional = false) CustomerProfile profile;"
        );

        assertEquals("1", bounds.get("sourceLowerBound"));
        assertEquals("1", bounds.get("sourceUpperBound"));
        assertEquals("1", bounds.get("targetLowerBound"));
        assertEquals("1", bounds.get("targetUpperBound"));
    }
}
