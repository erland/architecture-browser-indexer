package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParsingService;

import java.util.LinkedHashMap;
import java.util.Map;

final class ArchitectureIrDocumentMetadataBuilder {
    private ArchitectureIrDocumentMetadataBuilder() {
    }

    static Map<String, Object> build(
        ArchitectureIrAssemblyInputs inputs,
        ArchitectureIrAssemblyState assembly,
        RunAssessment assessment
    ) {
        Map<String, Object> documentMetadata = new LinkedHashMap<>();
        documentMetadata.put("inventoryEntries", inputs.inventory().entries());
        documentMetadata.put("inventorySummary", Map.of(
            "totalFiles", inputs.inventory().totalFiles(),
            "indexedFiles", inputs.inventory().indexedFiles(),
            "ignoredFiles", inputs.inventory().ignoredFiles(),
            "detectedLanguages", inputs.inventory().detectedLanguages(),
            "detectedTechnologyMarkers", inputs.inventory().detectedTechnologyMarkers()
        ));
        if (inputs.parseBatchResult() != null) {
            documentMetadata.put("parseSummary", TreeSitterParsingService.summarize(inputs.parseBatchResult()));
        }
        if (inputs.extractionResult() != null) {
            documentMetadata.put("extractionSummary", inputs.extractionResult().summary());
        }
        if (inputs.interpretationResult() != null) {
            documentMetadata.put("interpretationSummary", inputs.interpretationResult().summary());
        }
        if (inputs.topologyResult() != null) {
            documentMetadata.put("topologySummary", inputs.topologyResult().summary());
        }
        documentMetadata.put("dependencyViews", assembly.dependencyViews());
        documentMetadata.put("diagnosticSummary", assessment.diagnosticSummary());
        documentMetadata.put("partialResult", assessment.partialResult());
        return Map.copyOf(documentMetadata);
    }
}
