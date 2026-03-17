package info.isaksson.erland.architecturebrowser.indexer.ir;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedMetadataModelAdaptersTest {
    @Test
    void preservesDependencyViewSummaryAndBrowserDescriptorMetadata() {
        DependencyViewEntry entry = DependencyViewEntry.of(
            Map.of("sourceTypeId", "a", "targetTypeId", "b", "relationshipKind", "DEPENDS_ON"),
            new DependencyViewEntry.DependencyViewSummary(
                List.of("field"),
                List.of("composition"),
                List.of("react"),
                List.of("renders"),
                List.of("framework", "composition"),
                List.of("rel-1"),
                List.of("App renders Child")
            ),
            Map.of("evidenceRelationshipCount", 1),
            Map.of("internalTarget", true)
        );
        Map<String, Object> metadata = entry.toMetadataMap();
        assertEquals(List.of("field"), metadata.get("dependencySources"));
        assertEquals(List.of("framework", "composition"), metadata.get("architectureViewKinds"));
        assertEquals(true, metadata.get("internalTarget"));

        BrowserViewDescriptor descriptor = new BrowserViewDescriptor(
            "reactComponentCompositionGraph",
            "React component composition graph",
            "desc",
            "react",
            "composition",
            "compositionTypeDependencies",
            "compositionModuleDependencies",
            List.of("renders"),
            true,
            2,
            1
        );
        Map<String, Object> browserMetadata = descriptor.toMetadataMap();
        assertEquals("react", browserMetadata.get("framework"));
        assertEquals(true, browserMetadata.get("available"));
        assertEquals(2, browserMetadata.get("typeDependencyCount"));
        assertTrue(((List<?>) browserMetadata.get("relationshipTypes")).contains("renders"));
    }
}
