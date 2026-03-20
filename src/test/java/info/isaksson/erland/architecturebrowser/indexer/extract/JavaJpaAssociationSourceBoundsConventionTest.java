package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaJpaAssociationSourceBoundsConventionTest {
    @Test
    void manyToOneUsesFullAssociationEndConvention() {
        Map<String, Object> bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds("many-to-one", "@ManyToOne(optional = false)");

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("*", bounds.get("sourceUpperBound"));
        assertEquals("1", bounds.get("targetLowerBound"));
        assertEquals("1", bounds.get("targetUpperBound"));
    }

    @Test
    void oneToManyUsesFullAssociationEndConvention() {
        Map<String, Object> bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds("one-to-many", "@OneToMany(mappedBy = \"order\")");

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("1", bounds.get("sourceUpperBound"));
        assertEquals("0", bounds.get("targetLowerBound"));
        assertEquals("*", bounds.get("targetUpperBound"));
    }

    @Test
    void oneToOneUsesSameSingleValuedBoundsAtBothEnds() {
        Map<String, Object> bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds("one-to-one", "@OneToOne(optional = false)");

        assertEquals("1", bounds.get("sourceLowerBound"));
        assertEquals("1", bounds.get("sourceUpperBound"));
        assertEquals("1", bounds.get("targetLowerBound"));
        assertEquals("1", bounds.get("targetUpperBound"));
    }

    @Test
    void manyToManyUsesCollectionBoundsAtBothEnds() {
        Map<String, Object> bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds("many-to-many", "@ManyToMany");

        assertEquals("0", bounds.get("sourceLowerBound"));
        assertEquals("*", bounds.get("sourceUpperBound"));
        assertEquals("0", bounds.get("targetLowerBound"));
        assertEquals("*", bounds.get("targetUpperBound"));
    }
}
