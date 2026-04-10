package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureIrViewpointDerivationServiceModularizationTest {

    @Test
    void keepsCanonicalBaseViewpointRegistrationOrder() {
        List<ArchitectureViewpoint> viewpoints = ArchitectureIrViewpointDerivationService.derive(
            List.of(),
            List.of(),
            java.util.Map.of()
        );

        assertEquals(
            List.of(
                "api-surface",
                "request-handling",
                "persistence-model",
                "integration-map",
                "module-dependencies",
                "ui-navigation"
            ),
            viewpoints.stream()
                .map(ArchitectureViewpoint::id)
                .filter(id -> !"event-flow".equals(id))
                .toList()
        );
    }
}
