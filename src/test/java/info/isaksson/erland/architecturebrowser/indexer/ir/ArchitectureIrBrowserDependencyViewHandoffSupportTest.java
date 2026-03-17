package info.isaksson.erland.architecturebrowser.indexer.ir;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrBrowserDependencyViewHandoffSupportTest {
    @Test
    void appliesBrowserViewsAndEvidenceStatusThroughExplicitHandoffSupport() {
        Map<String, Object> result = ArchitectureIrBrowserDependencyViewHandoffSupport.applyBrowserViewHandoff(
            Map.of("typeDependencies", List.of(), "packageDependencies", List.of(), "moduleDependencies", List.of()),
            new ArchitectureIrBrowserViewCompositionInputs(
                List.of(Map.of("sourceTypeId", "a", "targetTypeId", "b")),
                List.of(Map.of("sourceModuleName", "src/main/ts", "targetModuleName", "src/main/ts")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );

        assertEquals(Boolean.TRUE, result.get("hasFrontendBrowserView"));
        assertTrue(result.containsKey("frontendBrowserViews"));
        assertTrue(result.containsKey("browserViewCatalog"));
        assertTrue(result.containsKey("evidenceStatus"));
    }
}
