package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportContract;

import java.util.List;
import java.util.Map;

final class ExportContractSupport {
    static final String CURRENT_CONTRACT_VERSION = "1.0";
    static final String PAYLOAD_TYPE = "architecture-index-document";

    private ExportContractSupport() {
    }

    static ExportContract defaultContract(String producerVersion, ArchitectureIndexDocument document) {
        return new ExportContract(
            CURRENT_CONTRACT_VERSION,
            document.schemaVersion(),
            "architecture-browser-indexer/" + producerVersion,
            PAYLOAD_TYPE,
            List.of("FILE", "HTTP_IMPORT"),
            Map.of(
                "requiresSchemaVersion", document.schemaVersion(),
                "requiresPayloadType", PAYLOAD_TYPE,
                "supportsPartialResults", true,
                "supportsDiagnostics", true
            )
        );
    }
}
