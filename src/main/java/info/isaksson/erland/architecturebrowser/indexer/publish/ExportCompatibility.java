package info.isaksson.erland.architecturebrowser.indexer.publish;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.publish.model.ExportContract;

import java.util.Map;

public final class ExportCompatibility {
    private ExportCompatibility() {
    }

    public static Map<String, Object> evaluate(ExportContract contract, ArchitectureIndexDocument document) {
        boolean schemaMatches = contract.schemaVersion().equals(document.schemaVersion());
        boolean payloadTypeMatches = ExportContractSupport.PAYLOAD_TYPE.equals(contract.payloadType());
        boolean contractVersionSupported = ExportContractSupport.CURRENT_CONTRACT_VERSION.equals(contract.contractVersion());

        return Map.of(
            "schemaMatches", schemaMatches,
            "payloadTypeMatches", payloadTypeMatches,
            "contractVersionSupported", contractVersionSupported,
            "compatible", schemaMatches && payloadTypeMatches && contractVersionSupported
        );
    }
}
