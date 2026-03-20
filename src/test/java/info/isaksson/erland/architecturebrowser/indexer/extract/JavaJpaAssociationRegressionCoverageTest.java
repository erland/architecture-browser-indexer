package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaJpaAssociationRegressionCoverageTest {

    @Test
    void coversAllKeyJpaAssociationCasesWithConservativeNormalizedBounds() {
        assertBounds(
            JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
                "one-to-one",
                "@OneToOne(optional = false) CustomerProfile profile;"
            ),
            "1", "1", "1", "1"
        );

        assertBounds(
            JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
                "one-to-one",
                "@OneToOne CustomerProfile profile;"
            ),
            "0", "1", "0", "1"
        );

        assertBounds(
            JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
                "many-to-one",
                "@ManyToOne(optional = false) CustomerEntity customer;"
            ),
            "0", "*", "1", "1"
        );

        assertBounds(
            JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
                "many-to-one",
                "@ManyToOne CustomerEntity customer;"
            ),
            "0", "*", "0", "1"
        );

        assertBounds(
            JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
                "one-to-many",
                "@OneToMany(mappedBy = \"order\") List<OrderLineEntity> lines;"
            ),
            "0", "1", "0", "*"
        );

        assertBounds(
            JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
                "many-to-many",
                "@ManyToMany Set<TagEntity> tags;"
            ),
            "0", "*", "0", "*"
        );
    }

    @Test
    void keepsNonJpaRelationshipsFreeFromNormalizedAssociationBounds() {
        Map<String, Object> bounds = JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
            "",
            "private CustomerService customerService;"
        );

        assertTrue(bounds.isEmpty());
    }

    @Test
    void keepsOptionalWhenSingleValuedEvidenceConflicts() {
        assertBounds(
            JavaJpaAssociationSemanticsSupport.deriveAssociationBounds(
                "many-to-one",
                "@ManyToOne(optional = false) @JoinColumn(name = \"customer_id\", nullable = true) CustomerEntity customer;"
            ),
            "0", "*", "0", "1"
        );
    }

    private static void assertBounds(
        Map<String, Object> bounds,
        String sourceLower,
        String sourceUpper,
        String targetLower,
        String targetUpper
    ) {
        assertEquals(sourceLower, bounds.get("sourceLowerBound"));
        assertEquals(sourceUpper, bounds.get("sourceUpperBound"));
        assertEquals(targetLower, bounds.get("targetLowerBound"));
        assertEquals(targetUpper, bounds.get("targetUpperBound"));
    }
}
