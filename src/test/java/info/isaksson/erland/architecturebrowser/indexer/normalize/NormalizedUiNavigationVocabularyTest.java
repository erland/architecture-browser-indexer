package info.isaksson.erland.architecturebrowser.indexer.normalize;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizedUiNavigationVocabularyTest {

    @Test
    void architecturalRoleEnumReservesUiNavigationRoleIds() {
        Set<String> ids = Arrays.stream(ArchitecturalRole.values())
            .map(ArchitecturalRole::id)
            .collect(Collectors.toSet());

        assertTrue(ids.contains("ui-page"));
        assertTrue(ids.contains("ui-layout"));
        assertTrue(ids.contains("ui-navigation-node"));
    }

    @Test
    void architecturalRelationshipSemanticEnumReservesUiNavigationSemanticIds() {
        Set<String> ids = Arrays.stream(ArchitecturalRelationshipSemantic.values())
            .map(ArchitecturalRelationshipSemantic::id)
            .collect(Collectors.toSet());

        assertTrue(ids.contains("navigates-to"));
        assertTrue(ids.contains("contains-route"));
        assertTrue(ids.contains("redirects-to"));
        assertTrue(ids.contains("guards-route"));
    }
}
